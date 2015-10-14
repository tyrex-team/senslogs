package fr.inria.tyrex.sensors.monitoring;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;


/**
 * Monitor sensors to log them
 * <p/>
 * Created by Thibaud Michel on 13/01/15.
 */
public class RecordMonitor {

	private SensorManager mSensorManager;
	private LocationManager mLocationManager;
	private RecordLogs mRecordLogs;

	public final static int SENSOR_TYPE_LOCATION = -0x299;

	public final static Integer[] sensorsToRecord = new Integer[] {
			Sensor.TYPE_ACCELEROMETER,
			Sensor.TYPE_GYROSCOPE,
			Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
			Sensor.TYPE_MAGNETIC_FIELD,
			Sensor.TYPE_LINEAR_ACCELERATION,
			Sensor.TYPE_GRAVITY,
			Sensor.TYPE_ROTATION_VECTOR,
			Sensor.TYPE_STEP_DETECTOR,
			Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED
	};


	private static RecordMonitor instance = null;
	public static RecordMonitor getInstance(SensorManager sensorManager, LocationManager locationManager) {
		if(instance == null) {
			instance = new RecordMonitor(sensorManager, locationManager);

		}

		return instance;
	}


	private RecordMonitor(SensorManager sensorManager, LocationManager locationManager) {

		mSensorManager = sensorManager;
		mLocationManager = locationManager;

		mRecordLogs = RecordLogs.getInstance();

	}

	public void start(int frequencyAccelerometer, int frequencyGyroscope, int frequencyMagnetometer) {

		int maxFrequency = Math.max(frequencyAccelerometer, Math.max(frequencyGyroscope, frequencyMagnetometer));

		for (Integer sensorId : sensorsToRecord) {

			Sensor sensor = mSensorManager.getDefaultSensor(sensorId);

			if(sensor == null) {
				continue;
			}

			int sensorDelay = maxFrequency;
			switch (sensor.getType()) {
				case Sensor.TYPE_ACCELEROMETER:
					sensorDelay = frequencyAccelerometer;
					break;
				case Sensor.TYPE_GYROSCOPE:
				case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
					sensorDelay = frequencyGyroscope;
					break;
				case Sensor.TYPE_MAGNETIC_FIELD:
				case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
					sensorDelay = frequencyMagnetometer;
					break;
			}

			//The desired delay between two consecutive events in microseconds
			sensorDelay = 1000000/sensorDelay;

			mSensorManager.registerListener(mSensorEventListener, sensor, sensorDelay);
		}

		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, mLocationListener);
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 0, mLocationListener);
		mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 1, 0, mLocationListener);
	}

	public void stop() {

		mSensorManager.unregisterListener(mSensorEventListener);
		mLocationManager.removeUpdates(mLocationListener);
	}


	private SensorEventListener mSensorEventListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			mRecordLogs.onNewSensorValue(event.sensor.getType(), event.timestamp, event.values);
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};


	private LocationListener mLocationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			mRecordLogs.onNewLocation(location);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

		@Override
		public void onProviderEnabled(String provider) {

		}

		@Override
		public void onProviderDisabled(String provider) {

		}
	};

}
