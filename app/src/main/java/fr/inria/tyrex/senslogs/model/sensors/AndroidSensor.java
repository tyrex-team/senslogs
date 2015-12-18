package fr.inria.tyrex.senslogs.model.sensors;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.Sensor;

/**
 * Sensor from Android's SensorManager
 */
public class AndroidSensor extends Sensor {

    transient private android.hardware.Sensor mSensor;

    public AndroidSensor(android.hardware.Sensor sensor) {
        super(sensor.getType(), getCategoryFromSensor(sensor));
        mSensor = sensor;
    }


    @Override
    public String getName() {
        return mSensor.getName();
    }

    @Override
    public String getStringType() {

        switch (mSensor.getType()) {
            case android.hardware.Sensor.TYPE_ACCELEROMETER:
                return "Accelerometer";
            case android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return "Gyroscope";
            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return "Magnetic Field";
            case android.hardware.Sensor.TYPE_GYROSCOPE:
                return "Gyroscope Calibrated";
            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD:
                return "Magnetic Field Calibrated";
            case android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR:
                return "Game Rotation Vector";
            case android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return "Geomagnetic Rotation Vector";
            case android.hardware.Sensor.TYPE_GRAVITY:
                return "Gravity";
            case android.hardware.Sensor.TYPE_LINEAR_ACCELERATION:
                return "Linear Acceleration";
            case android.hardware.Sensor.TYPE_ROTATION_VECTOR:
                return "Rotation Vector";
            case android.hardware.Sensor.TYPE_SIGNIFICANT_MOTION:
                return "Significant Motion Detector";
            case android.hardware.Sensor.TYPE_STEP_COUNTER:
                return "Step Counter";
            case android.hardware.Sensor.TYPE_STEP_DETECTOR:
                return "Step Detector";
            case android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE:
                return "Ambient Temperature";
            case android.hardware.Sensor.TYPE_LIGHT:
                return "Light";
            case android.hardware.Sensor.TYPE_PRESSURE:
                return "Pressure";
            case android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY:
                return "Relative Humidity";
            case android.hardware.Sensor.TYPE_HEART_RATE:
                return "Heart Rate";
            case android.hardware.Sensor.TYPE_PROXIMITY:
                return "Proximity";
        }
        return null;

    }

    @Override
    public String getStorageFileName(Context context) {

        String outputName;

        int fileNameResourceId = getStorageFileNameResourceId();

        // If file name is not defined in resources file
        if (fileNameResourceId != -1) {
            outputName = context.getString(fileNameResourceId);
        }
        // If file name is in resource file
        else {
            outputName = context.getString(R.string.file_name_unknown);
            if (Build.VERSION.SDK_INT >= 20) {
                outputName = String.format(outputName, mSensor.getStringType());
            }
        }

        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (!sensorManager.getDefaultSensor(mSensor.getType()).equals(mSensor)) {
            return outputName + "#";
        }

        return outputName;

    }

    @Override
    public String getDataDescription(Resources res) {

        int descriptionResourceId = getDataDescriptionResourceId();

        if (descriptionResourceId != -1) {
            return res.getString(descriptionResourceId);
        }

        return res.getString(R.string.description_unknown);
    }


    @Override
    public String getWebPage(Resources res) {
        return res.getString(R.string.webpage_sensor_from_sensor_manager);
    }

    @Override
    public boolean exists(Context context) {
        return true;
    }

    @Override
    public boolean checkPermission(Context context) {
        return true;
    }

    @Override
    public void start(Context context, Sensor.Settings settings) {

        if (!(settings instanceof Settings)) {
            return;
        }
        Settings sensorSettings = (Settings) settings;

        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(mSensorEventListener, mSensor, sensorSettings.sensorDelay);
    }

    @Override
    public void stop(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(mSensorEventListener);
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public Settings getDefaultSettings() {
        return Settings.DEFAULT;
    }


    public android.hardware.Sensor getSensor() {
        return mSensor;
    }

    public static class Settings extends Sensor.Settings {
        public int sensorDelay;

        public static Settings DEFAULT = new Settings(SensorManager.SENSOR_DELAY_NORMAL);

        public Settings(int sensorDelay) {
            this.sensorDelay = sensorDelay;
        }

        public static int getDelayIntegerFromString(String delay) {
            if ("UI".equals(delay)) {
                return SensorManager.SENSOR_DELAY_UI;
            } else if ("Normal".equals(delay)) {
                return SensorManager.SENSOR_DELAY_NORMAL;
            } else if ("Game".equals(delay)) {
                return SensorManager.SENSOR_DELAY_GAME;
            } else if ("Fastest".equals(delay)) {
                return SensorManager.SENSOR_DELAY_FASTEST;
            }
            throw new RuntimeException("Can't find delay value from string (" + delay + ")");
        }

        public static String getDelayStringFromInteger(int delay) {
            switch (delay) {
                case SensorManager.SENSOR_DELAY_UI:
                    return "UI";
                case SensorManager.SENSOR_DELAY_NORMAL:
                    return "Normal";
                case SensorManager.SENSOR_DELAY_GAME:
                    return "Game";
                case SensorManager.SENSOR_DELAY_FASTEST:
                    return "Fastest";
            }
            throw new RuntimeException("Can't find delay value from int (" + delay + ")");
        }

        @Override
        public String toString() {
            return "AndroidSensor.Settings{" +
                    "sensorDelay=" + sensorDelay +
                    '}';
        }
    }

    transient private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(final SensorEvent event) {

            if (mListener == null) {
                return;
            }

            final Object[] output = new Object[event.values.length];
            for (int i = 0; i < event.values.length; i++) {
                output[i] = event.values[i];
            }

            mListener.onNewValues(event.timestamp, output);
        }

        @Override
        public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
        }
    };


    private static Category getCategoryFromSensor(android.hardware.Sensor sensor) {
        switch (sensor.getType()) {
            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
            case android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
            case android.hardware.Sensor.TYPE_ACCELEROMETER:
                return Category.IMU;

            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD:
            case android.hardware.Sensor.TYPE_GYROSCOPE:
                return Category.IMU_CALIBRATED;

            case android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR:
            case android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
            case android.hardware.Sensor.TYPE_GRAVITY:
            case android.hardware.Sensor.TYPE_LINEAR_ACCELERATION:
            case android.hardware.Sensor.TYPE_ROTATION_VECTOR:
            case android.hardware.Sensor.TYPE_SIGNIFICANT_MOTION:
            case android.hardware.Sensor.TYPE_STEP_COUNTER:
            case android.hardware.Sensor.TYPE_STEP_DETECTOR:
                return Category.IMU_COMPUTED;

            case android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE:
            case android.hardware.Sensor.TYPE_LIGHT:
            case android.hardware.Sensor.TYPE_PRESSURE:
            case android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY:
                return Category.ENVIRONMENT;

            case android.hardware.Sensor.TYPE_HEART_RATE:
            case android.hardware.Sensor.TYPE_PROXIMITY:
                return Category.OTHER;
        }

        return Category.UNKNOWN;
    }


    private int getStorageFileNameResourceId() {

        switch (mSensor.getType()) {
            case android.hardware.Sensor.TYPE_ACCELEROMETER:
                return R.string.file_name_accelerometer;
            case android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return R.string.file_name_gyroscope_uncalibrated;
            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return R.string.file_name_magnetometer_uncalibrated;
            case android.hardware.Sensor.TYPE_GYROSCOPE:
                return R.string.file_name_gyroscope;
            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD:
                return R.string.file_name_magnetometer;
            case android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR:
                return R.string.file_name_game_rotation_vector;
            case android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return R.string.file_name_geomagnetic_rotation_vector;
            case android.hardware.Sensor.TYPE_GRAVITY:
                return R.string.file_name_gravity;
            case android.hardware.Sensor.TYPE_LINEAR_ACCELERATION:
                return R.string.file_name_linear_acceleration;
            case android.hardware.Sensor.TYPE_ROTATION_VECTOR:
                return R.string.file_name_rotation_vector;
            case android.hardware.Sensor.TYPE_SIGNIFICANT_MOTION:
                return R.string.file_name_significant_motion;
            case android.hardware.Sensor.TYPE_STEP_COUNTER:
                return R.string.file_name_step_counter;
            case android.hardware.Sensor.TYPE_STEP_DETECTOR:
                return R.string.file_name_step_detector;
            case android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE:
                return R.string.file_name_ambient_temperature;
            case android.hardware.Sensor.TYPE_LIGHT:
                return R.string.file_name_light;
            case android.hardware.Sensor.TYPE_PRESSURE:
                return R.string.file_name_pressure;
            case android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY:
                return R.string.file_name_relative_humidity;
            case android.hardware.Sensor.TYPE_HEART_RATE:
                return R.string.file_name_heart_rate;
            case android.hardware.Sensor.TYPE_PROXIMITY:
                return R.string.file_name_proximity;
        }
        return -1;

    }

    private int getDataDescriptionResourceId() {

        switch (mSensor.getType()) {
            case android.hardware.Sensor.TYPE_ACCELEROMETER:
                return R.string.description_accelerometer;
            case android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return R.string.description_gyroscope_uncalibrated;
            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return R.string.description_magnetometer_uncalibrated;
            case android.hardware.Sensor.TYPE_GYROSCOPE:
                return R.string.description_gyroscope;
            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD:
                return R.string.description_magnetometer;
            case android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR:
                return R.string.description_game_rotation_vector;
            case android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return R.string.description_geomagnetic_rotation_vector;
            case android.hardware.Sensor.TYPE_GRAVITY:
                return R.string.description_gravity;
            case android.hardware.Sensor.TYPE_LINEAR_ACCELERATION:
                return R.string.description_linear_acceleration;
            case android.hardware.Sensor.TYPE_ROTATION_VECTOR:
                return R.string.description_rotation_vector;
            case android.hardware.Sensor.TYPE_SIGNIFICANT_MOTION:
                return R.string.description_significant_motion;
            case android.hardware.Sensor.TYPE_STEP_COUNTER:
                return R.string.description_step_counter;
            case android.hardware.Sensor.TYPE_STEP_DETECTOR:
                return R.string.description_step_detector;
            case android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE:
                return R.string.description_ambient_temperature;
            case android.hardware.Sensor.TYPE_LIGHT:
                return R.string.description_light;
            case android.hardware.Sensor.TYPE_PRESSURE:
                return R.string.description_pressure;
            case android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY:
                return R.string.description_relative_humidity;
            case android.hardware.Sensor.TYPE_HEART_RATE:
                return R.string.description_heart_rate;
            case android.hardware.Sensor.TYPE_PROXIMITY:
                return R.string.description_proximity;
        }
        return -1;

    }

    public String getUnitsStringFormat(Resources res) {

        switch (mType) {
            case android.hardware.Sensor.TYPE_ACCELEROMETER:
            case android.hardware.Sensor.TYPE_LINEAR_ACCELERATION:
                return res.getString(R.string.unit_acceleration);

            case android.hardware.Sensor.TYPE_GYROSCOPE:
            case android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return res.getString(R.string.unit_rotation_speed);

            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD:
            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return res.getString(R.string.unit_magnetic_field);

            case android.hardware.Sensor.TYPE_LIGHT:
                return res.getString(R.string.unit_light);

            case android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE:
                return res.getString(R.string.unit_temperature);

            case android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY:
                return res.getString(R.string.unit_relative);

            case android.hardware.Sensor.TYPE_PRESSURE:
                return res.getString(R.string.unit_pressure);

            case android.hardware.Sensor.TYPE_PROXIMITY:
                return res.getString(R.string.unit_proximity);

            case android.hardware.Sensor.TYPE_STEP_COUNTER:
                return res.getString(R.string.unit_steps);

            default:
                return "%f";
        }
    }

}
