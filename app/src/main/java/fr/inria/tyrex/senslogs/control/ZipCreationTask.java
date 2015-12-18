package fr.inria.tyrex.senslogs.control;

import android.os.AsyncTask;
import android.os.Handler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Creation of log files asynchronously and put it on a zip
 */
public class ZipCreationTask extends AsyncTask<ZipCreationTask.Params,
        ZipCreationTask.Progress, File> {

    private static final int BUFFER = 2048;

    public static class Params {
        public Collection<File> inputFiles;
        public File outputFile;

        public Params(File outputFile, Collection<File> inputFiles) {
            this.inputFiles = inputFiles;
            this.outputFile = outputFile;
        }
    }

    public static class Progress {
        public File currentFile;
        public float totalProgress;

        public Progress(File currentFile, float totalProgress) {
            this.currentFile = currentFile;
            this.totalProgress = totalProgress;
        }
    }

    @Override
    protected File doInBackground(Params... params) {

        if (params.length != 1) {
            return null;
        }

        File outputFile = params[0].outputFile;
        Collection<File> inputFiles = params[0].inputFiles;

        long totalFilesSize = 0l;
        for (File file : inputFiles) {
            totalFilesSize += file.length();
        }

        long currentFilesRead = 0l;
        try {
            BufferedInputStream origin;
            FileOutputStream dest = new FileOutputStream(outputFile);

            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

            byte data[] = new byte[BUFFER];

            for (File file : inputFiles) {
                FileInputStream fi = new FileInputStream(file);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(file.getName());
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                    currentFilesRead += count;
                    publishProgress(new Progress(file, (float) currentFilesRead / totalFilesSize));
                }
                origin.close();
            }

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return outputFile;
    }


    @Override
    protected void onProgressUpdate(final Progress... values) {
        super.onProgressUpdate(values);

        if (values.length != 1) {
            return;
        }

        for (final Map.Entry<ZipCreationListener, Handler> listener : mListeners.entrySet()) {
            listener.getValue().post(new Runnable() {
                @Override
                public void run() {
                    listener.getKey().onProgress(values[0].currentFile, values[0].totalProgress);
                }
            });
        }
    }

    @Override
    protected void onPostExecute(final File file) {
        super.onPostExecute(file);

        for (final Map.Entry<ZipCreationListener, Handler> listener : mListeners.entrySet()) {
            listener.getValue().post(new Runnable() {
                @Override
                public void run() {
                    listener.getKey().onTaskFinished(file, file.length());
                }
            });
        }
    }


    private Map<ZipCreationListener, Handler> mListeners = new HashMap<>();

    public interface ZipCreationListener {
        void onProgress(File currentFile, float ratio);

        void onTaskFinished(File outputFile, long fileSize);
    }

    public void addListener(ZipCreationListener listener) {
        mListeners.put(listener, new Handler());
    }

    public void removeListener(ZipCreationListener listener) {
        mListeners.remove(listener);
    }

}
