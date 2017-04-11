package fr.inria.tyrex.senslogs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.inria.tyrex.senslogs.control.LogsManager;
import fr.inria.tyrex.senslogs.control.PreferencesManager;
import fr.inria.tyrex.senslogs.control.Recorder;
import fr.inria.tyrex.senslogs.control.SensorsManager;
import fr.inria.tyrex.senslogs.model.Log;

/**
 * Application class accessible from all activities
 */
public class Application extends android.app.Application {

    public final static String LOG_TAG = "SensorsRecorder";

    private SensorsManager mSensorsManager;
    private PreferencesManager mPreferencesManager;
    private Recorder mRecorder;
    private LogsManager mLogsManager;



    @Override
    public void onCreate() {
        super.onCreate();


        mSensorsManager = new SensorsManager(this);
        mLogsManager = new LogsManager(this, mSensorsManager);
        mPreferencesManager = new PreferencesManager(this, mSensorsManager);
        mRecorder = new Recorder(this, mLogsManager, mPreferencesManager);

        // Clean internal directory (just in case)
        cleanTmpFiles();
    }


    public PreferencesManager getPreferences() {
        return mPreferencesManager;
    }

    public Recorder getRecorder() {
        return mRecorder;
    }

    public LogsManager getLogsManager() {
        return mLogsManager;
    }

    public SensorsManager getSensorsManager() {
        return mSensorsManager;
    }


    private void cleanTmpFiles() {

        deleteRecursive(getCacheDir());

        if(mLogsManager == null) {
            return;
        }

        List<File> logFiles = new ArrayList<>();
        for(Log log : mLogsManager.getLogs()) {
            logFiles.add(log.getFile());
        }

        for (File child : getFilesDir().listFiles())
            if (!logFiles.contains(child))
                deleteRecursive(child);
    }

    public void clearAll() {
        mLogsManager.clearAll();
        mPreferencesManager.clearAll();
    }


    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        if(!fileOrDirectory.delete()) {
            android.util.Log.e(Application.LOG_TAG, "Cannot delete log file");
        }
    }

}
