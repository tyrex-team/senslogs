package fr.inria.tyrex.senslogs.control;

import android.content.Context;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.List;

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
 * Created by thibaud on 18/12/15.
 */
public class SensorsManager {


    private ArrayList<Sensor> mAvailableSensorsList;
    private List<Sensor> mSensorsToCalibrate;


    public SensorsManager(Context context) {
        generateAvailableSensors(context);
    }

    public List<Sensor> getAvailableSensors() {
        return mAvailableSensorsList;
    }

    public List<Sensor> getSensorsToCalibrate() { return mSensorsToCalibrate;}

    private void generateAvailableSensors(Context context) {

        mAvailableSensorsList = new ArrayList<>();
        mSensorsToCalibrate = new ArrayList<>();

        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        for (android.hardware.Sensor sensor : sensorManager.getSensorList(android.hardware.Sensor.TYPE_ALL)) {
            AndroidSensor as = new AndroidSensor(sensor);
            mAvailableSensorsList.add(as);
            switch(sensor.getType()) {
                case android.hardware.Sensor.TYPE_ACCELEROMETER:
                case android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                case android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                    mSensorsToCalibrate.add(as);
            }
        }

        if (LocationGpsSensor.getInstance().exists(context)) {
            mAvailableSensorsList.add(LocationGpsSensor.getInstance());
        }
        if (LocationWifiAndCellsSensor.getInstance().exists(context)) {
            mAvailableSensorsList.add(LocationWifiAndCellsSensor.getInstance());
        }
        if (LocationPassiveSensor.getInstance().exists(context)) {
            mAvailableSensorsList.add(LocationPassiveSensor.getInstance());
        }
        if (BluetoothSensor.getInstance().exists(context)) {
            mAvailableSensorsList.add(BluetoothSensor.getInstance());
        }
        if (NfcSensor.getInstance().exists(context)) {
            mAvailableSensorsList.add(NfcSensor.getInstance());
        }
        if (WifiSensor.getInstance().exists(context)) {
            mAvailableSensorsList.add(WifiSensor.getInstance());
        }
        if (NmeaSensor.getInstance().exists(context)) {
            mAvailableSensorsList.add(NmeaSensor.getInstance());
        }
    }

    public Sensor getSensorByName(String name) {
        for (Sensor sensor : mAvailableSensorsList) {
            if (sensor.getName().equals(name)) {
                return sensor;
            }
        }
        return null;
    }

    public Sensor getSensorByType(int type) {
        for (Sensor sensor : mAvailableSensorsList) {
            if (sensor.getType() == type) {
                return sensor;
            }
        }
        return null;
    }
}