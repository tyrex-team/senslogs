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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.Log;
import fr.inria.tyrex.senslogs.model.RecordProperties;
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
    private RecorderWriter mRecorderWriter;
    private RecordProperties mRecordProperties;

    private Map<Sensor, Sensor.Settings> mSensorsAndSettings;


    private boolean isRecording = false;

    public Recorder(Context context, LogsManager logsManager) {
        mContext = context;
        mLogsManager = logsManager;
        mRecordProperties = new RecordProperties();
    }

    public void start(Map<Sensor, Sensor.Settings> sensorsAndSettings) throws FileNotFoundException {
        mSensorsAndSettings = sensorsAndSettings;

        mRecordProperties.init();


        mRecorderWriter = new RecorderWriter(mContext);

        File tmpSubFolder = new File(mContext.getFilesDir(), String.valueOf(UUID.randomUUID()));

        if (!tmpSubFolder.mkdir()) {
            throw new FileNotFoundException();
        }

        Set<RecorderWriter.WritableObject> writableObjects = new HashSet<>();
        writableObjects.addAll(mSensorsAndSettings.keySet());
        writableObjects.add(TimestampReferenceManager.getWritableObject());

        mRecorderWriter.init(writableObjects, tmpSubFolder);
        for (final Sensor sensor : mSensorsAndSettings.keySet()) {
            sensor.setListener(new Sensor.Listener() {
                @Override
                public void onNewValues(double diffTimeSystem, double diffTimeSensor, Object[] objects) {
                    mRecorderWriter.write(sensor, diffTimeSystem, diffTimeSensor, objects);
                }
            });
        }

        LocationManager locationManager =
                (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        for (final Map.Entry<Sensor, Sensor.Settings> sensorAndSetting : mSensorsAndSettings.entrySet()) {

            Sensor sensor = sensorAndSetting.getKey();

            if (sensor.getType() == Sensor.TYPE_LOCATION_GPS &&
                    ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED){
                mRecordProperties.gpsLastKnownLocation =
                        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (sensor.getType() == Sensor.TYPE_LOCATION_PASSIVE &&
                    ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED){
                mRecordProperties.passiveLastKnownLocation =
                        locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }

            if (sensor.getType() == Sensor.TYPE_LOCATION_CELL_WIFI &&
                    ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED){
                mRecordProperties.networkLastKnownLocation =
                        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }

        resetTimer();
        resume();
    }


    public void resume() {

        if (isRecording) {
            return;
        }

        startTimer();
        (mDataSizeHandler = new Handler()).post(dataSizeUpdate);


        for (final Map.Entry<Sensor, Sensor.Settings> sensorAndSetting : mSensorsAndSettings.entrySet()) {
            if (sensorAndSetting.getKey().mustRunOnUiThread()) {
                sensorAndSetting.getKey().start(mContext, sensorAndSetting.getValue(), mRecordProperties);
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sensorAndSetting.getKey().start(mContext, sensorAndSetting.getValue(), mRecordProperties);
                    }
                }).start();
            }
        }

        createNotification();
        isRecording = true;

    }

    public void pause() {

        if (!isRecording) {
            return;
        }

        stopTimer();
        mDataSizeHandler.removeCallbacks(dataSizeUpdate);

        for (final Sensor sensor : mSensorsAndSettings.keySet()) {
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

        mRecordProperties.endTime = System.currentTimeMillis() / 1e3d;
        removeNotification();
        isRecording = false;
    }


    public void stop() throws IOException {

        pause();

        for (final Sensor sensor : mSensorsAndSettings.keySet()) {
            sensor.setListener(null);
        }

        resetTimer();
        mRecorderWriter.finish();

    }

    public void cancel() {
        mRecorderWriter.removeFiles();
    }

    public Log save(String title) throws IOException {

        if (title == null || title.isEmpty()) {
            title = mContext.getString(R.string.record_finish_empty_filename);
        }
        String filename = title.replaceAll("\\W+", "_");

        final Pair<File, ZipCreationTask> zipCreationPair = mRecorderWriter.createZipFile(filename,
                mRecordProperties);
        final HashSet<Sensor> sensors = new HashSet<>(mSensorsAndSettings.keySet());
        Log newLog = new Log(title, zipCreationPair.first, mRecorderWriter.getFilesSize(),
                mRecordProperties, sensors, zipCreationPair.second);

        mLogsManager.addLog(newLog);

        zipCreationPair.second.addListener(new ZipCreationTask.ZipCreationListener() {
            @Override
            public void onProgress(File currentFile, float ratio) {
            }

            @Override
            public void onTaskFinished(File outputFile, long fileSize) {
                mRecorderWriter.removeFiles();
                zipCreationPair.second.removeListener(this);
            }
        });

        return newLog;
    }


    public void addReferenceTimestamp(long timestamp) {
        double diffTime = timestamp / 1e3d - mRecordProperties.startTime;
        mRecorderWriter.write(TimestampReferenceManager.getWritableObject(), diffTime);
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

    public boolean isRecording() {
        return isRecording;
    }


    public interface DataSizeListener {
        void onNewTotalSize(long totalSize);
    }
    //</editor-fold>

    //<editor-fold desc="Timer">
    private Handler timerHandler;
    private long firstTime;
    private long computeTime = 0;

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

    public Set<Sensor> getSelectedSensors() {
        if (mSensorsAndSettings != null)
            return mSensorsAndSettings.keySet();
        return new HashSet<>();
    }


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
}
