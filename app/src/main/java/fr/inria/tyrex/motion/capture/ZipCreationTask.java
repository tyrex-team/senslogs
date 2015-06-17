package fr.inria.tyrex.motion.capture;

import android.content.Context;
import android.hardware.Sensor;
import android.os.AsyncTask;
import android.os.Build;
import android.util.SparseArray;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Creation of log files asynchronously and put it on a zip
 *
 * Created by Thibaud Michel on 13/01/15.
 */
public class ZipCreationTask extends AsyncTask<String, Integer, String> {


	private final static String descriptionFileName = "description.txt";
	private final static SparseArray<String> sensorsOutputFiles;
	public final static int totalTasks;

	static {
		sensorsOutputFiles = new SparseArray<String>();
		sensorsOutputFiles.append(Sensor.TYPE_ACCELEROMETER, "accelerometer.txt");
		sensorsOutputFiles.append(Sensor.TYPE_GYROSCOPE, "gyroscope.txt");
		sensorsOutputFiles.append(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, "gyroscope-uncalibrated.txt");
		sensorsOutputFiles.append(Sensor.TYPE_MAGNETIC_FIELD, "magnetic-field.txt");
		sensorsOutputFiles.append(Sensor.TYPE_LINEAR_ACCELERATION, "linear-acceleration.txt");
		sensorsOutputFiles.append(Sensor.TYPE_GRAVITY, "gravity.txt");
		sensorsOutputFiles.append(Sensor.TYPE_ROTATION_VECTOR, "rotation-vector.txt");
		sensorsOutputFiles.append(Sensor.TYPE_STEP_DETECTOR, "step-detector.txt");
		sensorsOutputFiles.append(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, "magnetic-field-uncalibrated.txt");
		sensorsOutputFiles.append(Sensor.TYPE_PRESSURE, "pressure.txt");
		sensorsOutputFiles.append(RecordMonitor.SENSOR_TYPE_LOCATION, "location.txt");

		totalTasks = sensorsOutputFiles.size()+1;
	}


	private final Context mContext;
	private final ZipCreationListener mListener;

	private int taskNumber;


	public ZipCreationTask(Context context, ZipCreationListener listener) {

		mContext = context;
		mListener = listener;
		taskNumber = 1;

	}


	@Override
	protected String doInBackground(String... filePaths) {

		if(filePaths.length == 0) {
			return null;
		}

		String filePath = filePaths[0];

		SparseArray<Map<Double, Object[]>> logs = RecordLogs.getInstance().getLogs();

		try {

			FileOutputStream fos = new FileOutputStream(filePath);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ZipOutputStream zos = new ZipOutputStream(bos);


			for(int i = 0; i < sensorsOutputFiles.size(); i++){

				if(logs.get(sensorsOutputFiles.keyAt(i)) == null)
					continue;

				writeInZip(zos, sensorsOutputFiles.valueAt(i), logs.get(sensorsOutputFiles.keyAt(i)));
				publishProgress(taskNumber++);

			}

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			zos.putNextEntry(new ZipEntry(descriptionFileName));
			zos.write(mContext.getString(R.string.description_file_begin, sdf.format(new Date()), Build.MODEL).getBytes());
			InputStream descriptionFile = mContext.getResources().getAssets().open("description.txt");

			byte[] buffer = new byte[65536 * 2];
			int read;
			while ((read = descriptionFile.read(buffer)) != -1) {
				zos.write(buffer, 0, read);
			}

			zos.closeEntry();

			zos.close();
			bos.close();
			fos.close();

			publishProgress(taskNumber);

			return filePath;

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}


	private void writeInZip(ZipOutputStream zos, String fileName, Map<Double, Object[]> data) {

		try {
			zos.putNextEntry(new ZipEntry(fileName));
			for (Map.Entry<Double, Object[]> entry : data.entrySet()) {
				Object[] values = entry.getValue();
				String output = String.valueOf(entry.getKey());
				for (Object value : values) {
					output += " " + value;
				}
				output += "\n";
				zos.write(output.getBytes());
			}
			zos.closeEntry();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);

		if(mListener != null && values.length > 0) {
			mListener.onSubTaskFinished(values[0]);
		}
	}

	@Override
	protected void onPostExecute(String filePath) {
		super.onPostExecute(filePath);

		if(mListener != null) {
			mListener.onTaskFinished(filePath);
		}
	}

	public interface ZipCreationListener {

		public void onSubTaskFinished(int taskNumber);
		public void onTaskFinished(String filePath);

	}

}
