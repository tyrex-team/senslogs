package fr.inria.tyrex.senslogs.model.log;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;

import org.ini4j.Wini;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import fr.inria.tyrex.senslogs.control.ZipCreationTask;
import fr.inria.tyrex.senslogs.model.WritableObject;
import fr.inria.tyrex.senslogs.model.sensors.Sensor;

/**
 * Log of a record made by {@link fr.inria.tyrex.senslogs.control.Recorder}
 */
public class Log implements Serializable {


    private String mName;

    private File mTemporaryFolder;
    private File mZipFile;

    private long mCompressedSize;
    private long mUncompressedSize;

    private RecordTimes mRecordTimes;

    private HashSet<Sensor> mSensors;

    private String mUser;
    private String mPositionOrientation;
    private String mComment;

    private transient ZipCreationTask mZipCreationTask;

    private transient List<Listener> mListeners;

    public Log() {
        this(new HashSet<Sensor>());
    }

    public Log(Set<Sensor> sensors) {

        mSensors = new HashSet<>(sensors);

        mListeners = new ArrayList<>();
        mRecordTimes = new RecordTimes();
    }

    public void init(Context context) throws FileNotFoundException {

        // Create new folder for records
        mTemporaryFolder = new File(context.getFilesDir(), String.valueOf(UUID.randomUUID()));
        if (!mTemporaryFolder.mkdir()) {
            throw new FileNotFoundException();
        }

        mRecordTimes.init();

    }


    public void setName(String newName) {
        mName = newName;
        notifyDatasetChanged();
    }

    public void setZipFile(File zipFile) {
        mZipFile = zipFile;
    }

    public void setUncompressedSize(long uncompressedSize) {
        mUncompressedSize = uncompressedSize;
    }

    public void setZipCreationTask(ZipCreationTask zipCreationTask) {
        mZipCreationTask = zipCreationTask;

        if (mZipCreationTask != null) {
            mZipCreationTask.addListener(new ZipCreationTask.ZipCreationListener() {
                @Override
                public void onProgress(File currentFile, float percent) {

                }

                @Override
                public void onTaskFinished(File outputFile, long fileSize) {
                    mCompressedSize = fileSize;
                    if (mZipCreationTask != null) {
                        mZipCreationTask.removeListener(this);
                        mZipCreationTask = null;
                    }
                    notifyDatasetChanged();
                }
            });
        }
    }

    public void setUser(String user) {
        mUser = user;
    }

    public void setPositionOrientation(String positionOrientation) {
        mPositionOrientation = positionOrientation;
    }

    public void setComment(String comment) {
        mComment = comment;
    }


    public String getName() {
        return mName;
    }

    public File getTemporaryFolder() {
        return mTemporaryFolder;
    }

    public File getZipFile() {
        return mZipFile;
    }

    public long getCompressedSize() {
        return mCompressedSize;
    }

    public long getUncompressedSize() {
        return mUncompressedSize;
    }

    public RecordTimes getRecordTimes() {
        return mRecordTimes;
    }

    public HashSet<Sensor> getSensors() {
        return mSensors;
    }

    public ZipCreationTask getCreationTask() {
        return mZipCreationTask;
    }


    public interface Listener {
        void onDatasetChanged(Log log);
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    private void notifyDatasetChanged() {
        for (Listener listener : mListeners) {
            listener.onDatasetChanged(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Log log = (Log) o;

        return mZipFile.equals(log.mZipFile);
    }

    @Override
    public int hashCode() {
        return mZipFile.hashCode();
    }


    @Override
    public String toString() {
        return "Log{" +
                "mName='" + mName + '\'' +
                ", mZipFile=" + mZipFile +
                ", mCompressedSize=" + mCompressedSize +
                ", mUncompressedSize=" + mUncompressedSize +
                ", mRecordTimes=" + mRecordTimes +
                ", mSensors=" + mSensors +
                ", mUser='" + mUser + '\'' +
                ", mPositionOrientation='" + mPositionOrientation + '\'' +
                ", mComment='" + mComment + '\'' +
                ", mZipCreationTask=" + mZipCreationTask +
                ", mListeners=" + mListeners +
                '}';
    }

    public Wini generateIniFile(Context context, File file, Set<WritableObject> writableObjects) {

        Wini ini;
        try {
            if (!file.createNewFile())
                return null;
            ini = new Wini(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        ini.put("Device", "Manufacturer", Build.MANUFACTURER);
        ini.put("Device", "Model", Build.MODEL);
        ini.put("Device", "OSVersion", Build.VERSION.SDK_INT);
        ini.put("Device", "ID", Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID));

        ini.put("Settings", "User", mUser);
        ini.put("Settings", "PositionOrientation", mPositionOrientation);
        ini.put("Settings", "Comment", mComment);

        ini.put("Time", "StartTime", String.format(Locale.US, "%.3f", mRecordTimes.startTime));
        ini.put("Time", "EndTime", String.format(Locale.US, "%.3f", mRecordTimes.endTime));
        ini.put("Time", "BootTime", String.format(Locale.US, "%.3f", mRecordTimes.bootTime));
        ini.put("Time", "MonotonicAtStart", String.format(Locale.US, "%.3f", mRecordTimes.monotonicAtStart));

        String sensorsList = "";
        for (WritableObject writableObject : writableObjects) {
            if (!(writableObject instanceof Sensor)) continue;
            Sensor sensor = (Sensor) writableObject;
            sensorsList += sensor.getName() + ", ";
        }
        if (sensorsList.length() > 2) {
            ini.put("Sensors", "List", sensorsList.substring(0, sensorsList.length() - 2));
        }


        /*
         * Extra data
         */
        for (WritableObject writableObject : writableObjects) {
            if (!(writableObject instanceof Sensor)) continue;
            Sensor sensor = (Sensor) writableObject;
            for (IniRecord record : sensor.getExtraIniRecords(context)) {
                ini.put(record.sectionName, record.optionName, record.value);
            }
        }

        return ini;
    }

    public class RecordTimes implements Serializable {
        public double startTime; // in seconds from unix time
        public double endTime; // in seconds from unix time
        public double bootTime; // in seconds from unix time
        public double monotonicAtStart; // in seconds

        public void init() {
            startTime = System.currentTimeMillis() / 1e3d;
            monotonicAtStart = System.nanoTime() / 1e9d;
            bootTime = (System.currentTimeMillis() - SystemClock.elapsedRealtime()) / 1e3d;
        }
    }

    public static class IniRecord {
        public String sectionName;
        public String optionName;
        public Object value;

        public IniRecord(String sectionName, String optionName, Object value) {
            this.sectionName = sectionName;
            this.optionName = optionName;
            this.value = value;
        }
    }

}
