package fr.inria.tyrex.senslogs.control;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.Log;
import fr.inria.tyrex.senslogs.model.PositionReference;
import fr.inria.tyrex.senslogs.model.Sensor;
import fr.inria.tyrex.senslogs.ui.RecordActivity;
import fr.inria.tyrex.senslogs.ui.utils.KillNotificationsService;

/**
 * Handle timers and record sensors data
 */
public class Recorder {

    public final static int NOTIFICATION_ID = 1;

    private final Context mContext;
    private final LogsManager mLogsManager;
    private final PreferencesManager mPreferencesManager;

    private RecorderWriter mRecorderWriter;
    private Log mLog;

    private Map<Sensor, Sensor.Settings> mSensorsAndSettings;
    private LinkedList<PositionReference> mReferences;


    private boolean isRecording = false;
    private boolean isFirstPlay = true;

    public Recorder(Context context, LogsManager logsManager,
                    PreferencesManager preferencesManager) {
        mContext = context;
        mLogsManager = logsManager;
        mPreferencesManager = preferencesManager;
        mReferences = new LinkedList<>();
    }


    public boolean isRecording() {
        return isRecording;
    }


    public void play() throws FileNotFoundException {
        play(mPreferencesManager.getSelectedSensors());
    }

    public void play(Map<Sensor, Sensor.Settings> sensorsAndSettings) throws FileNotFoundException {
        play(sensorsAndSettings, Log.Calibration.NO);
    }

    public void play(Map<Sensor, Sensor.Settings> sensorsAndSettings, Log.Calibration calibration)
            throws FileNotFoundException {

        if (isRecording) {
            return;
        }

        // Need to init some properties for the first play
        if (isFirstPlay) {

            // Retrieve sensors from preferences
            mSensorsAndSettings = sensorsAndSettings;

            mLog = new Log(calibration, mSensorsAndSettings.keySet());
            mLog.init();
            mReferences.clear();

            // Create new folder for records
            File tmpSubFolder = new File(mContext.getFilesDir(), String.valueOf(UUID.randomUUID()));
            if (!tmpSubFolder.mkdir()) {
                throw new FileNotFoundException();
            }

            // Retrieve and init writer with sensors
            Set<RecorderWriter.WritableObject> writableObjects = new HashSet<>();
            writableObjects.addAll(mSensorsAndSettings.keySet());

            // We need to create a new instance because writer is used during zip creation task
            mRecorderWriter = new RecorderWriter(mContext);
            mRecorderWriter.init(tmpSubFolder);
            mRecorderWriter.createFiles(writableObjects);

            // Retrieve last known locations for ini file
            LocationManager locationManager =
                    (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            for (final Map.Entry<Sensor, Sensor.Settings> sensorAndSetting : mSensorsAndSettings.entrySet()) {

                Sensor sensor = sensorAndSetting.getKey();

                if (sensor.getType() == Sensor.TYPE_LOCATION_GPS &&
                        ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                    mLog.setGpsLastKnownLocation(
                            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
                }

                if (sensor.getType() == Sensor.TYPE_LOCATION_PASSIVE &&
                        ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                    mLog.setPassiveLastKnownLocation(
                            locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
                }

                if (sensor.getType() == Sensor.TYPE_LOCATION_CELL_WIFI &&
                        ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                    mLog.setNetworkLastKnownLocation(
                            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
                }
            }

            isFirstPlay = false;
        }


        // Start and listen sensors
        for (final Map.Entry<Sensor, Sensor.Settings> sensorAndSetting : mSensorsAndSettings.entrySet()) {

            final Sensor sensor = sensorAndSetting.getKey();
            final Sensor.Settings settings = sensorAndSetting.getValue();

            sensor.setListener(new Sensor.Listener() {
                @Override
                public void onNewValues(double diffTimeSystem, double diffTimeSensor, Object[] objects) {
                    mRecorderWriter.asycWrite(sensor, diffTimeSystem, diffTimeSensor, objects);
                }
            });

            // Some sensors take a long time to start but have to be run on UI thread
            if (sensor.mustRunOnUiThread()) {
                sensor.start(mContext, settings, mLog.getRecordTimes());
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sensor.start(mContext, settings, mLog.getRecordTimes());
                    }
                }).start();
            }
        }

        createNotification();
        startTimer();
        (mDataSizeHandler = new Handler()).post(dataSizeUpdate);
        isRecording = true;
    }


    public void pause() {

        if (!isRecording) {
            return;
        }

        stopTimer();
        mDataSizeHandler.removeCallbacks(dataSizeUpdate);
        removeNotification();

        for (final Sensor sensor : mSensorsAndSettings.keySet()) {

            sensor.setListener(null);

            if (sensor.mustRunOnUiThread()) {
                sensor.stop(mContext);
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sensor.stop(mContext);
                    }
                }).start();
            }
        }

        mLog.getRecordTimes().endTime = System.currentTimeMillis() / 1e3d;
        isRecording = false;
    }


    public void cancel() throws IOException {

        if (isRecording) {
            pause();
        }
        mRecorderWriter.finish();
        mRecorderWriter.removeFiles();
        isFirstPlay = true;
        isRecording = false;
    }


    public Log save(String title) throws IOException {
        return save(title, null, null, null);
    }

    public Log save(String title, String user, String positionOrientation, String comment) throws IOException {

        if(mReferences.size() > 0) {

            // Write reference positions
            RecorderWriter.WritableObject prWritableObject = PositionsReferenceManager.getWritableObject();
            mRecorderWriter.createFile(prWritableObject);
            for (PositionReference reference : mReferences) {
                mRecorderWriter.write(prWritableObject, reference.elapsedTime, null, reference.toObject());
            }

        }

        mRecorderWriter.finish();

        resetTimer();

        // Set title to unknown if null and replace non word characters by underscore
        if (title == null || title.isEmpty()) {
            title = mContext.getString(R.string.record_finished_empty_filename);
        }
        String filename = title.replaceAll("\\W+", "_");

        mLog.setName(title);
        mLog.setUser(user);
        mLog.setComment(comment);
        mLog.setPositionOrientation(positionOrientation);
        mLog.setUncompressedSize(mRecorderWriter.getFilesSize());


        // Create Zip File
        final Pair<File, ZipCreationTask> zipCreationPair = mRecorderWriter.createZipFile(filename, mLog);
        final File zipFile = zipCreationPair.first;
        final ZipCreationTask zipTask = zipCreationPair.second;

        mLog.setZipCreationTask(zipTask);

        zipTask.addListener(new ZipCreationTask.ZipCreationListener() {
            @Override
            public void onProgress(File currentFile, float ratio) {
            }

            @Override
            public void onTaskFinished(File outputFile, long fileSize) {
                // Remove files when recorder finished
                mRecorderWriter.removeFiles();
                zipTask.removeListener(this);
            }
        });

        mLog.setFile(zipFile);

        mLogsManager.addLog(mLog);

        isFirstPlay = true;

        return mLog;
    }


    public Log.RecordTimes getRecordTimes() {
        return mLog.getRecordTimes();
    }


    public void addReference(double latitude, double longitude, Float level) {
        double elapsedTime = System.currentTimeMillis() / 1e3d - mLog.getRecordTimes().startTime;
        mReferences.add(new PositionReference(elapsedTime, latitude, longitude, level));
    }

    public void removeLastReference() {
        mReferences.pollLast();
    }


    //<editor-fold desc="DataSize">
    private Handler mDataSizeHandler;

    private Runnable dataSizeUpdate = new Runnable() {
        @Override
        public void run() {

            if (mDataSizeListener != null) {

                mDataSizeListener.first.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mDataSizeListener == null || mDataSizeListener.second == null) {
                            return;
                        }
                        mDataSizeListener.second.onNewTotalSize(mRecorderWriter.getFilesSize());
                    }
                });
            }

            mDataSizeHandler.postDelayed(dataSizeUpdate, 50);
        }
    };

    private Pair<Handler, DataSizeListener> mDataSizeListener;

    public void setDataSizeListener(DataSizeListener listener) {
        if (listener == null) {
            mDataSizeListener = null;
            return;
        }
        mDataSizeListener = new Pair<>(new Handler(), listener);
    }


    public interface DataSizeListener {
        void onNewTotalSize(long totalSize);
    }
    //</editor-fold>


    //<editor-fold desc="Timer">
    private Handler timerHandler;
    private long firstTime;
    private long computeTime;

    private Runnable timer = new Runnable() {
        @Override
        public void run() {

            final long diffTime = computeTime + System.currentTimeMillis() - firstTime;

            if (mTimerListener != null) {
                mTimerListener.first.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mTimerListener == null || mTimerListener.second == null) {
                            return;
                        }
                        mTimerListener.second.onNewTime(diffTime);
                    }
                });
            }

            timerHandler.postDelayed(timer, 73);
        }
    };

    private void startTimer() {
        timerHandler = new Handler();
        firstTime = System.currentTimeMillis();
        computeTime = 0;
        timerHandler.post(timer);
    }


    private void stopTimer() {
        computeTime += System.currentTimeMillis() - firstTime;
        timerHandler.removeCallbacks(timer);
    }

    private void resetTimer() {
        computeTime = 0;
        if (mTimerListener != null) {
            mTimerListener.first.post(new Runnable() {
                @Override
                public void run() {
                    mTimerListener.second.onReset();
                }
            });
        }
    }

    private Pair<Handler, TimerListener> mTimerListener;

    public void setTimerListener(TimerListener listener) {
        if (listener == null) {
            mTimerListener = null;
            return;
        }
        mTimerListener = new Pair<>(new Handler(), listener);
    }

    public interface TimerListener {
        void onNewTime(long diffTime);

        void onReset();
    }
    //</editor-fold>


    //<editor-fold desc="Notification">

    private void createNotification() {
        mContext.bindService(new Intent(mContext, KillNotificationsService.class),
                mNotificationConnection, Context.BIND_AUTO_CREATE);
    }

    private void removeNotification() {
        mContext.unbindService(mNotificationConnection);

        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);
    }


    // http://stackoverflow.com/questions/12997800/cancel-notification-on-remove-application-from-multitask-panel
    private ServiceConnection mNotificationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            ((KillNotificationsService.KillBinder) binder).service.startService(
                    new Intent(mContext, KillNotificationsService.class));

            Intent intent = new Intent(mContext, RecordActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(mContext)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(mContext.getString(R.string.app_name))
                            .setContentText(mContext.getString(R.string.record_notification))
                            .setContentIntent(pendingIntent);

            Notification notification = builder.build();
            notification.flags = Notification.FLAG_ONGOING_EVENT;

            NotificationManager mNotificationManager =
                    (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        }

        public void onServiceDisconnected(ComponentName className) {
        }

    };
    //</editor-fold>


}

