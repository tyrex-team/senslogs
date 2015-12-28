package fr.inria.tyrex.senslogs.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import fr.inria.tyrex.senslogs.control.ZipCreationTask;

/**
 * Log of a record made by {@link fr.inria.tyrex.senslogs.control.Recorder}
 */
public class Log implements Serializable {

    private String mName;
    private File mFile;
    private long mCompressedSize;
    private long mUncompressedSize;
    private Date mStartCapture;
    private Date mEndCapture;
    private HashSet<Sensor> mSensors;
    private transient ZipCreationTask mZipCreationTask;

    private transient List<Listener> mListeners = new ArrayList<>();

    public Log() {
        mSensors = new HashSet<>();
    }

    public Log(String name, File file, long compressedSize, long uncompressedSize,
               Date startCapture, Date endCapture, HashSet<Sensor> sensors) {
        this.mName = name;
        this.mFile = file;
        this.mCompressedSize = compressedSize;
        this.mUncompressedSize = uncompressedSize;
        this.mSensors = sensors;
        this.mStartCapture = startCapture;
        this.mEndCapture = endCapture;
    }

    public Log(String name, File file, long uncompressedSize, Date startCapture,
               Date endCapture, HashSet<Sensor> sensors, final ZipCreationTask zipCreationTask) {

        this.mName = name;
        this.mFile = file;
        this.mUncompressedSize = uncompressedSize;
        this.mSensors = sensors;
        this.mStartCapture = startCapture;
        this.mEndCapture = endCapture;
        this.mZipCreationTask = zipCreationTask;

        mZipCreationTask.addListener(new ZipCreationTask.ZipCreationListener() {
            @Override
            public void onProgress(File currentFile, float percent) {

            }

            @Override
            public void onTaskFinished(File outputFile, long fileSize) {
                mCompressedSize = fileSize;
                mZipCreationTask.removeListener(this);
                mZipCreationTask = null;
                notifyDatasetChanged();
            }
        });
    }

    public ZipCreationTask getCreationTask() {
        return mZipCreationTask;
    }


    public void setName(String newName) {
        mName = newName;
        notifyDatasetChanged();
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

    public Date getStartCapture() {
        return mStartCapture;
    }

    public Date getEndCapture() {
        return mEndCapture;
    }

    public HashSet<Sensor> getSensors() {
        return mSensors;
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
                "mSensors=" + mSensors +
                ", mEndCapture=" + mEndCapture +
                ", mStartCapture=" + mStartCapture +
                ", mUncompressedSize=" + mUncompressedSize +
                ", mCompressedSize=" + mCompressedSize +
                ", mFile=" + mFile +
                ", mName='" + mName + '\'' +
                '}';
    }
}
