package fr.inria.tyrex.senslogs.model;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import fr.inria.tyrex.senslogs.control.RecorderWriter;
import fr.inria.tyrex.senslogs.control.ZipCreationTask;

/**
 * Log of a record made by {@link fr.inria.tyrex.senslogs.control.Recorder}
 */
public class Log implements Serializable {

    public enum Calibration {NO, GYROSCOPE, MAGNETOMETER, ACCELEROMETER}

    private String mName;
    private File mFile;
    private transient Calibration mCalibration;

    private long mCompressedSize;
    private long mUncompressedSize;

    private RecordTimes mRecordTimes;

    private transient Location mGpsLastKnownLocation;
    private transient Location mNetworkLastKnownLocation;
    private transient Location mPassiveLastKnownLocation;

    private HashSet<Sensor> mSensors;

    private String mUser;
    private String mPositionOrientation;
    private String mComment;

    private transient ZipCreationTask mZipCreationTask;

    private transient List<Listener> mListeners;

    public Log() {
        this(Calibration.NO, new HashSet<Sensor>());
    }

    public Log(Calibration calibration, Set<Sensor> sensors) {

        mCalibration = calibration;
        mSensors = new HashSet<>(sensors);

        mListeners = new ArrayList<>();
        mRecordTimes = new RecordTimes();
    }

    public void init() {

        mRecordTimes.init();

        mGpsLastKnownLocation = null;
        mNetworkLastKnownLocation = null;
        mPassiveLastKnownLocation = null;
    }


    public void setName(String newName) {
        mName = newName;
        notifyDatasetChanged();
    }

    public void setGpsLastKnownLocation(Location gpsLastKnownLocation) {
        this.mGpsLastKnownLocation = gpsLastKnownLocation;
    }

    public void setNetworkLastKnownLocation(Location networkLastKnownLocation) {
        this.mNetworkLastKnownLocation = networkLastKnownLocation;
    }

    public void setPassiveLastKnownLocation(Location passiveLastKnownLocation) {
        this.mPassiveLastKnownLocation = passiveLastKnownLocation;
    }

    public void setFile(File file) {
        mFile = file;
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

    public File getFile() {
        return mFile;
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

        return mFile.equals(log.mFile);
    }

    @Override
    public int hashCode() {
        return mFile.hashCode();
    }


    @Override
    public String toString() {
        return "Log{" +
                "mName='" + mName + '\'' +
                ", mFile=" + mFile +
                ", mCalibration=" + mCalibration +
                ", mCompressedSize=" + mCompressedSize +
                ", mUncompressedSize=" + mUncompressedSize +
                ", mRecordTimes=" + mRecordTimes +
                ", mGpsLastKnownLocation=" + mGpsLastKnownLocation +
                ", mNetworkLastKnownLocation=" + mNetworkLastKnownLocation +
                ", mPassiveLastKnownLocation=" + mPassiveLastKnownLocation +
                ", mSensors=" + mSensors +
                ", mUser='" + mUser + '\'' +
                ", mPositionOrientation='" + mPositionOrientation + '\'' +
                ", mComment='" + mComment + '\'' +
                ", mZipCreationTask=" + mZipCreationTask +
                ", mListeners=" + mListeners +
                '}';
    }

    public Wini generateIniFile(Context context, File file, Map<RecorderWriter.WritableObject, File> files) {

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

        ini.put("Settings", "Calibration", mCalibration);
        ini.put("Settings", "User", mUser);
        ini.put("Settings", "PositionOrientation", mPositionOrientation);
        ini.put("Settings", "Comment", mComment);

        ini.put("Time", "StartTime", String.format(Locale.US, "%.3f", mRecordTimes.startTime));
        ini.put("Time", "EndTime", String.format(Locale.US, "%.3f", mRecordTimes.endTime));
        ini.put("Time", "BootTime", String.format(Locale.US, "%.3f", mRecordTimes.bootTime));
        ini.put("Time", "MonotonicAtStart", String.format(Locale.US, "%.3f", mRecordTimes.monotonicAtStart));

        String sensorsList = "";
        for (Map.Entry<RecorderWriter.WritableObject, File> fileEntry : files.entrySet()) {
            if (!(fileEntry.getKey() instanceof Sensor)) {
                continue;
            }
            Sensor sensor = (Sensor) fileEntry.getKey();
            sensorsList += sensor.getName() + ", ";
        }
        if(sensorsList.length() > 2) {
            ini.put("Sensors", "List", sensorsList.substring(0, sensorsList.length() - 2));
        }

        fillIniWithLocation(ini, "LastKnownGPSLocation", mGpsLastKnownLocation);
        fillIniWithLocation(ini, "LastKnownNetworkLocation", mNetworkLastKnownLocation);
        fillIniWithLocation(ini, "LastKnownPassiveLocation", mPassiveLastKnownLocation);

        return ini;
    }

    private void fillIniWithLocation(Wini ini, String sectionName, Location location) {
        if (location != null) {
            ini.put(sectionName, "Latitude", location.getLatitude());
            ini.put(sectionName, "Longitude", location.getLongitude());
            ini.put(sectionName, "Altitude", location.getAltitude());
            ini.put(sectionName, "UnixTime", String.format(Locale.US, "%.3f", location.getTime() / 1e3d));
            ini.put(sectionName, "Accuracy", location.getAccuracy());
            ini.put(sectionName, "Bearing", location.getBearing());
        }
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
}
