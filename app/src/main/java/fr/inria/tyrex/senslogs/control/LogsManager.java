package fr.inria.tyrex.senslogs.control;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import fr.inria.tyrex.senslogs.Application;
import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.Log;
import fr.inria.tyrex.senslogs.model.preferences.LogsDataSource;

/**
 * Handle sensors logs and store in preferences for consistency
 */
public class LogsManager {


    private List<Log> mLogs;
    private LogsDataSource mDataSource;

    public LogsManager(Context context, SensorsManager sensorsManager) {
        mDataSource = new LogsDataSource(context, sensorsManager);
        loadLogs();
    }

    public List<Log> getLogs() {
        return mLogs;
    }


    public void addLog(Log log) {
        mLogs.add(log);
        mDataSource.addLog(log);
        log.addListener(mDatasetChangedListener);
    }


    public void deleteLog(Log log) {
        if(!log.getFile().delete()) {
            android.util.Log.e(Application.LOG_TAG, "Cannot delete log file");
        }
        mLogs.remove(log);
        mDataSource.deleteLog(log);
        log.removeListener(mDatasetChangedListener);
    }



    public File copyLogToSdCard(Context context, Log log, CopyTask.Listener listener) {

        File outputDir = new File(Environment.getExternalStorageDirectory(),
                context.getString(R.string.folder_logs_sd_card));

        if(!outputDir.exists() && !outputDir.mkdir()) {
            return null;
        }

        File outputFile = new File(outputDir, log.getFile().getName());

        CopyTask task = new CopyTask();
        task.setListener(listener);
        task.execute(new CopyTask.Input(log.getFile(), outputFile));

        return outputFile;
    }



    /*
    Shared preferences consistency
     */

    private Log.Listener mDatasetChangedListener = new Log.Listener() {
        @Override
        public void onDatasetChanged(Log log) {
            mDataSource.updateLog(log);
        }
    };

    private void loadLogs() {

        mLogs = mDataSource.getLogs();
        for(Log log : mLogs) {
            log.addListener(mDatasetChangedListener);
        }

    }

    public void clearAll() {
        Iterator<Log> iterator = mLogs.iterator();
        while(iterator.hasNext()) {
            Log log = iterator.next();
            log.removeListener(mDatasetChangedListener);
            if(!log.getFile().delete()) {
                android.util.Log.e(Application.LOG_TAG, "Cannot delete log file");
            }
            iterator.remove();
        }
        mDataSource.removeAll();
        mLogs.clear();
    }
}
