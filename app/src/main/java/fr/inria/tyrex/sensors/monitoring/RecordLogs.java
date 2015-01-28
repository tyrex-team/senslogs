package fr.inria.tyrex.sensors.monitoring;

import android.location.Location;
import android.util.SparseArray;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Organize sensor logs
 * <p/>
 * Created by Thibaud Michel on 13/01/15.
 */
public class RecordLogs {

	private Integer[] mSensors = RecordMonitor.sensorsToRecord;

	private SparseArray<Map<Double, Object[]>> mLogs;

	private double startTime = 0;
	private double startTimeGPS = 0;

	private static RecordLogs instance = null;

	public static RecordLogs getInstance() {
		if (instance == null) {
			instance = new RecordLogs();
		}

		return instance;
	}

	private RecordLogs() {

		mLogs = new SparseArray<Map<Double, Object[]>>();
		mLogs.put(RecordMonitor.SENSOR_TYPE_LOCATION, new LinkedHashMap<Double, Object[]>());

		for (Integer sensorId : mSensors) {
			mLogs.put(sensorId, new LinkedHashMap<Double, Object[]>());
		}


	}

	public void reset() {
		startTime = 0;
		startTimeGPS = 0;

		for (Integer sensorId : mSensors) {
			mLogs.get(sensorId).clear();
		}
	}

	public void onNewSensorValue(int sensorType, long timestamp, float[] values) {

		int dataSize = values.length;
		Object[] data = new Object[dataSize];
		for (int i = 0; i < dataSize; i++) {
			data[i] = values[i];
		}

		if (startTime == 0) {
			startTime = timestamp / 1000000000.;
			startTimeGPS = System.currentTimeMillis() / 1000.;
		}

		double time = timestamp / 1000000000. - startTime;

		mLogs.get(sensorType).put(time, data);

	}

	public void onNewLocation(Location location) {

		Object[] data = new Object[]{location.getLatitude(), location.getLongitude(), location.getAltitude(),
				location.getBearing(), location.getAccuracy(), location.getSpeed()};

		mLogs.get(RecordMonitor.SENSOR_TYPE_LOCATION).put(location.getTime() / 1000. - startTimeGPS, data);
	}

	public SparseArray<Map<Double, Object[]>> getLogs() {
		return mLogs;
	}
}
