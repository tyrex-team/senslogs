package fr.inria.tyrex.senslogs;

import android.content.Context;
import android.hardware.SensorManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.inria.tyrex.senslogs.control.LogsManager;
import fr.inria.tyrex.senslogs.control.SensorsPreferences;
import fr.inria.tyrex.senslogs.control.Recorder;
import fr.inria.tyrex.senslogs.model.Sensor;
import fr.inria.tyrex.senslogs.model.sensors.AndroidSensor;
import fr.inria.tyrex.senslogs.model.sensors.BluetoothSensor;
import fr.inria.tyrex.senslogs.model.sensors.LocationGpsSensor;
import fr.inria.tyrex.senslogs.model.sensors.LocationPassiveSensor;
import fr.inria.tyrex.senslogs.model.sensors.LocationWifiAndCellsSensor;
import fr.inria.tyrex.senslogs.model.sensors.NfcSensor;
import fr.inria.tyrex.senslogs.model.sensors.NmeaSensor;
import fr.inria.tyrex.senslogs.model.sensors.WifiSensor;

/**
 * Application class accessible from all activities
 */
public class Application extends android.app.Application {

    public final static String LOG_TAG = "SensorsRecorder";

    private SensorsPreferences mSensorsPreferences;
    private Recorder mRecorder;
    private LogsManager mLogsManager;

    private ArrayList<Sensor> mAvailableSensorsList;


    @Override
    public void onCreate() {
        super.onCreate();

        // Clean internal directory (just in case)
        deleteRecursive(getCacheDir());
        for (File child : getFilesDir().listFiles())
            if (child.isDirectory())
                deleteRecursive(child);


        generateAvailableSensors();

        mLogsManager = new LogsManager(this, mAvailableSensorsList);
        mSensorsPreferences = new SensorsPreferences(this, mAvailableSensorsList);
        mRecorder = new Recorder(this, mLogsManager);
    }

    public SensorsPreferences getPreferences() {
        return mSensorsPreferences;
    }

    public Recorder getRecorder() {
        return mRecorder;
    }

    public LogsManager getLogsManager() {
        return mLogsManager;
    }


    public List<Sensor> getAvailableSensors() {
        return mAvailableSensorsList;
    }

    private void generateAvailableSensors() {

        mAvailableSensorsList = new ArrayList<>();

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        for (android.hardware.Sensor sensor : sensorManager.getSensorList(android.hardware.Sensor.TYPE_ALL)) {
            mAvailableSensorsList.add(new AndroidSensor(sensor));
        }

        if (LocationGpsSensor.getInstance().exists(this)) {
            mAvailableSensorsList.add(LocationGpsSensor.getInstance());
        }
        if (LocationWifiAndCellsSensor.getInstance().exists(this)) {
            mAvailableSensorsList.add(LocationWifiAndCellsSensor.getInstance());
        }
        if (LocationPassiveSensor.getInstance().exists(this)) {
            mAvailableSensorsList.add(LocationPassiveSensor.getInstance());
        }
        if (BluetoothSensor.getInstance().exists(this)) {
            mAvailableSensorsList.add(BluetoothSensor.getInstance());
        }
        if (NfcSensor.getInstance().exists(this)) {
            mAvailableSensorsList.add(NfcSensor.getInstance());
        }
        if (WifiSensor.getInstance().exists(this)) {
            mAvailableSensorsList.add(WifiSensor.getInstance());
        }
        if (NmeaSensor.getInstance().exists(this)) {
            mAvailableSensorsList.add(NmeaSensor.getInstance());
        }
    }

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        if(!fileOrDirectory.delete()) {
            android.util.Log.e(Application.LOG_TAG, "Cannot delete log file");
        }
    }

    public void clearAll() {
        mLogsManager.clearAll();
        mSensorsPreferences.clearAll();
    }

}
