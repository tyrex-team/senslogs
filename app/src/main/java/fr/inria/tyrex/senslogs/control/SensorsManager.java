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

    private static ArrayList<Sensor> mAvailableSensorsList;

    public SensorsManager(Context context) {
        generateAvailableSensors(context);
    }

    public List<Sensor> getAvailableSensors() {
        return mAvailableSensorsList;
    }

    private void generateAvailableSensors(Context context) {

        mAvailableSensorsList = new ArrayList<>();

        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        for (android.hardware.Sensor sensor : sensorManager.getSensorList(android.hardware.Sensor.TYPE_ALL)) {
            mAvailableSensorsList.add(new AndroidSensor(sensor));
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
}
