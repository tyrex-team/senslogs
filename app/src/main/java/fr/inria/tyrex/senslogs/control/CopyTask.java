package fr.inria.tyrex.senslogs.control;

import android.os.AsyncTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copy a file from Input.src to Input.dst
 */
public class CopyTask extends AsyncTask<CopyTask.Input, Long, File> {

    public static class Input {
        public File src;
        public File dst;

        public Input(File src, File dst) {
            this.src = src;
            this.dst = dst;
        }
    }

    @Override
    protected File doInBackground(Input... params) {

        if (params.length != 1) {
            return null;
        }

        try {

            InputStream in = new FileInputStream(params[0].src);
            OutputStream out = new FileOutputStream(params[0].dst);

            long totalSize = 0;

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                publishProgress(totalSize += len);
            }

            in.close();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return params[0].dst;
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        super.onProgressUpdate(values);
        if(mListener != null && values.length == 1) {
            mListener.onProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(File outputFile) {
        super.onPostExecute(outputFile);
        if (mListener != null) {
            mListener.onCopyFinished(outputFile);
        }
    }


    private Listener mListener;
    public void setListener(Listener listener) {
        mListener = listener;
    }
    public interface Listener {
        void onCopyFinished(File outputFile);
        void onProgress(Long currentSize);
    }
}
