package fr.inria.tyrex.senslogs.control;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.FieldsWritableObject;
import fr.inria.tyrex.senslogs.model.PositionReference;
import fr.inria.tyrex.senslogs.model.log.CalibrationLog;
import fr.inria.tyrex.senslogs.model.log.Log;
import fr.inria.tyrex.senslogs.model.sensors.CameraRecorder;
import fr.inria.tyrex.senslogs.model.sensors.Sensor;
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
    private boolean isInitialized = false;

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

    public boolean isRecording(Sensor sensor) {
        return mSensorsAndSettings.containsKey(sensor);
    }

    private void init(@NonNull Map<Sensor, Sensor.Settings> sensorsAndSettings,
                      @Nullable CalibrationLog.Type calibration)
            throws FileNotFoundException {

        // Store sensors and their settings
        mSensorsAndSettings = sensorsAndSettings;

        mLog = calibration != null ?
                CalibrationLog.create(calibration, mSensorsAndSettings.keySet()) :
                new Log(mSensorsAndSettings.keySet());

        mLog.init(mContext);
        mReferences.clear();

        // We need to create a new instance because writer is used during zip creation task
        mRecorderWriter = new RecorderWriter(mContext);
        mRecorderWriter.init(mLog);
    }


    public void play() throws FileNotFoundException {
        play(mPreferencesManager.getSelectedSensors(), null);
    }

    public void play(Map<Sensor, Sensor.Settings> sensorsAndSettings, CalibrationLog.Type calibration)
            throws FileNotFoundException {

        if (isRecording) return;


        // Need to init some properties for the first play
        if (!isInitialized) {
            init(sensorsAndSettings, calibration);
            isInitialized = true;
        }


        // Start and listen sensors
        for (final Map.Entry<Sensor, Sensor.Settings> sensorAndSetting : mSensorsAndSettings.entrySet()) {

            final Sensor sensor = sensorAndSetting.getKey();
            final Sensor.Settings settings = sensorAndSetting.getValue();

            if (sensor instanceof CameraRecorder) {
                mRecorderWriter.updateVideoPath();
            }

            if (sensor instanceof FieldsWritableObject) {
                sensor.setListener(new Sensor.Listener() {
                    @Override
                    public void onNewValues(double diffTimeSystem, double diffTimeSensor, Object[] objects) {
                        mRecorderWriter.asycWrite(sensor, diffTimeSystem, diffTimeSensor, objects);
                    }
                });
            }

            // Some sensors take a long time to start but have to be run on UI thread
            if (sensor.mustRunOnUiThread()) {
                sensor.start(mContext, settings, mLog.getRecordTimes());
            } else {
                new Thread(() -> sensor.start(mContext, settings, mLog.getRecordTimes())).start();
            }
        }

        createNotification();
        startTimer();
        isRecording = true;
    }


    public void pause() {

        if (!isRecording) return;

        stopTimer();
        removeNotification();

        for (final Sensor sensor : mSensorsAndSettings.keySet()) {

            sensor.setListener(null);

            if (sensor.mustRunOnUiThread()) {
                sensor.stop(mContext);
            } else {
                new Thread(() -> sensor.stop(mContext)).start();
            }
        }

        mLog.getRecordTimes().endTime = System.currentTimeMillis() / 1e3d;
        isRecording = false;
    }


    public void cancel() throws IOException {

        if (isRecording) {
            pause();
        }
        resetTimer();
        mRecorderWriter.finish();
        mRecorderWriter.removeFiles();
        isInitialized = false;
        isRecording = false;
    }


    public Log save(String title) throws IOException {
        return save(title, null, null, null);
    }

    public Log save(String title, String user, String positionOrientation, String comment) throws IOException {

        mRecorderWriter.writeReferences(mReferences);
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
        mLog.setUncompressedSize(mRecorderWriter.getDataSize());


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

        mLog.setZipFile(zipFile);

        mLogsManager.addLog(mLog);

        isInitialized = false;

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
    public long getDataSize() {
        return mRecorderWriter.getDataSize();
    }
    //</editor-fold>


    //<editor-fold desc="Timer">
    private long firstTime;
    private long computeTime;

    public long getCurrentTime() {
        return computeTime + System.currentTimeMillis() - firstTime;
    }

    private void startTimer() {
        firstTime = System.currentTimeMillis();
    }

    private void stopTimer() {
        computeTime += System.currentTimeMillis() - firstTime;
    }

    private void resetTimer() {
        computeTime = 0;
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

