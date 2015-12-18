package fr.inria.tyrex.senslogs.control;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.Log;
import fr.inria.tyrex.senslogs.model.PositionReference;
import fr.inria.tyrex.senslogs.model.Sensor;
import fr.inria.tyrex.senslogs.model.sensors.AndroidSensor;
import fr.inria.tyrex.senslogs.model.sensors.LocationSensor;

/**
 * Handle timers and record sensors data
 */
public class Recorder {

    private final Context mContext;
    private final LogsManager mLogsManager;
    private RecorderWriter mRecorderWriter;

    private Map<Sensor, Sensor.Settings> mSensorsAndSettings;

    private long mStartTime;
    private long mStartTimeLocation;
    private long mStartTimeAndroidSensors;

    private Date mStartDate;
    private Date mStopDate;
    private boolean isRecording = false;

    public Recorder(Context context, LogsManager logsManager) {
        mContext = context;
        mLogsManager = logsManager;
    }

    public void start(Map<Sensor, Sensor.Settings> sensorsAndSettings) throws FileNotFoundException {
        mSensorsAndSettings = sensorsAndSettings;

        mStartTimeLocation = System.nanoTime();

        // https://code.google.com/p/android/issues/detail?id=56561
        if(Build.DEVICE.equals("mako")) {
            mStartTimeAndroidSensors = System.currentTimeMillis()*1000000L;
        } else {
            mStartTimeAndroidSensors = System.nanoTime();
        }

        mStartTime = System.currentTimeMillis();

        mStartDate = new Date();
        mRecorderWriter = new RecorderWriter(mContext);

        File tmpSubFolder = new File(mContext.getFilesDir(), String.valueOf(UUID.randomUUID()));

        if (!tmpSubFolder.mkdir()) {
            throw new FileNotFoundException();
        }

        Set<RecorderWriter.WritableObject> writableObjects = new HashSet<>();
        writableObjects.addAll(mSensorsAndSettings.keySet());
        writableObjects.add(PositionReferenceManager.getWritableObject());

        mRecorderWriter.init(writableObjects, tmpSubFolder);
        for (final Sensor sensor : mSensorsAndSettings.keySet()) {
            sensor.setListener(new Sensor.Listener() {
                @Override
                public void onNewValues(long timestamp, Object[] objects) {

                    double diffTime; // in second
                    if (sensor instanceof AndroidSensor) {
                        diffTime = (timestamp - mStartTimeAndroidSensors) / 1e9d;
                    } else if (sensor instanceof LocationSensor) {
                        diffTime = (timestamp - mStartTimeLocation) / 1e9d;

                    } else {
                        diffTime = (timestamp - mStartTime) / 1e3d;
                    }

                    mRecorderWriter.write(sensor, diffTime, objects);
                }
            });
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
                sensorAndSetting.getKey().start(mContext, sensorAndSetting.getValue());
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sensorAndSetting.getKey().start(mContext, sensorAndSetting.getValue());
                    }
                }).start();
            }
        }

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

        mStopDate = new Date();
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

        if(title == null || title.isEmpty()) {
            title = mContext.getString(R.string.record_finish_empty_filename);
        }
        String filename = title.replaceAll("\\W+", "_");

        final Pair<File, ZipCreationTask> zipCreationPair = mRecorderWriter.createZipFile(filename);
        final HashSet<Sensor> sensors = new HashSet<>(mSensorsAndSettings.keySet());
        Log newLog = new Log(title, zipCreationPair.first, mRecorderWriter.getFilesSize(),
                mStartDate, mStopDate, sensors, zipCreationPair.second);

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


    public void addReference(long timestamp, PositionReference ref) {
        double diffTime = (timestamp - mStartTime) / 1e3d;
        mRecorderWriter.write(
                PositionReferenceManager.getWritableObject(),
                diffTime,
                ref.toObject());
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
        return mSensorsAndSettings.keySet();
    }
}
