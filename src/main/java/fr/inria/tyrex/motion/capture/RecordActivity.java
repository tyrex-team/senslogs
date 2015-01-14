package fr.inria.tyrex.motion.capture;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Main Activity of Application
 * <p/>
 * Created by Thibaud Michel on 13/01/15.
 */
public class RecordActivity extends Activity {

	private final static int MinFrequency = 15;

	private RecordLogs mRecordLogs;
	private RecordMonitor mRecordMonitor;


	private boolean isRunning = false;
	private String mZipFile;

	private SeekBar mAccelerometerFrequencySeekBar,
			mGyroscopeFrequencySeekBar,
			mMagnetometerFrequencySeekBar;

	private TextView mTimerTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// http://stackoverflow.com/questions/9982433/android-accelerometer-not-working-when-screen-is-turned-off
		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


		mRecordLogs = RecordLogs.getInstance();
		mRecordMonitor = RecordMonitor.getInstance((SensorManager) getSystemService(Context.SENSOR_SERVICE),
		                                           (LocationManager) this.getSystemService(Context.LOCATION_SERVICE));


		initSensorDetails();

		mTimerTextView = (TextView) findViewById(R.id.timer);

		final ImageView startPauseButton = (ImageView) findViewById(R.id.start_pause);
		final TextView resetButton = (TextView) findViewById(R.id.reset);
		final TextView saveButton = (TextView) findViewById(R.id.save);
		final TextView sendButton = (TextView) findViewById(R.id.send);

		startPauseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if (isRunning) {

					stopTimer();
					mRecordMonitor.stop();

					startPauseButton.setBackgroundResource(R.drawable.start_button);
					startPauseButton.setContentDescription(getString(R.string.start));
					resetButton.setEnabled(true);
					saveButton.setEnabled(true);
					sendButton.setEnabled(true);
					isRunning = false;

				} else {

					int accelerometerFrequency = mAccelerometerFrequencySeekBar.getProgress() + MinFrequency;
					int gyroscopeFrequency = mGyroscopeFrequencySeekBar.getProgress() + MinFrequency;
					int magnetometerFrequency = mMagnetometerFrequencySeekBar.getProgress() + MinFrequency;

					mRecordMonitor.start(accelerometerFrequency, gyroscopeFrequency, magnetometerFrequency);
					startTimer();

					startPauseButton.setBackgroundResource(R.drawable.pause_button);
					startPauseButton.setContentDescription(getString(R.string.pause));
					resetButton.setEnabled(false);
					saveButton.setEnabled(false);
					sendButton.setEnabled(false);
					isRunning = true;
				}
			}
		});

		resetButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				mRecordLogs.reset();
				resetTimer();

				resetButton.setEnabled(false);
				saveButton.setEnabled(false);
				sendButton.setEnabled(false);

			}
		});


		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				send();
			}
		});

		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				save();
			}
		});

	}


	private Handler handler;
	private boolean timerStarted = false;
	private long firstTime;
	private long computeTime = 0;
	private SimpleDateFormat dateFormatTimer = new SimpleDateFormat("mm:ss:SSS");

	private class Timer implements Runnable {
		@Override
		public void run() {

			if (!timerStarted) {
				return;
			}

			final long diffTime = computeTime + System.currentTimeMillis() - firstTime;

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mTimerTextView.setText(dateFormatTimer.format(new Date(diffTime)));
				}
			});

			handler.postDelayed(this, 42);
		}
	}

	private void startTimer() {
		handler = new Handler();
		firstTime = System.currentTimeMillis();
		timerStarted = true;
		new Timer().run();
	}


	private void stopTimer() {
		timerStarted = false;
		computeTime += System.currentTimeMillis() - firstTime;
	}

	private void resetTimer() {
		computeTime = 0;
		mTimerTextView.setText("00:00:000");
	}

	private void initSensorDetails() {

		SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);


		final TextView accelerometerVendorView = (TextView) findViewById(R.id.accelerometer_vendor);
		final TextView gyroscopeVendorView = (TextView) findViewById(R.id.gyroscope_vendor);
		final TextView magnetometerVendorView = (TextView) findViewById(R.id.magnetometer_vendor);

		mAccelerometerFrequencySeekBar = (SeekBar) findViewById(R.id.accelerometer_frequency);
		mGyroscopeFrequencySeekBar = (SeekBar) findViewById(R.id.gyroscope_frequency);
		mMagnetometerFrequencySeekBar = (SeekBar) findViewById(R.id.magnetometer_frequency);

		final TextView accelerometerFrequencyTextView = (TextView) findViewById(R.id.accelerometer_frequency_text);
		final TextView gyroscopeFrequencyTextView = (TextView) findViewById(R.id.gyroscope_frequency_text);
		final TextView magnetometerFrequencyTextView = (TextView) findViewById(R.id.magnetometer_frequency_text);

		Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (accelerometer != null) {
			accelerometerVendorView.setText(accelerometer.getVendor() + " " + accelerometer.getName());

			mAccelerometerFrequencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					accelerometerFrequencyTextView.setText((progress + MinFrequency) + " Hz");
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {

				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {

				}
			});

			int maxFrequency = 1000000 / accelerometer.getMinDelay();
			mAccelerometerFrequencySeekBar.setMax(maxFrequency - MinFrequency);
			mAccelerometerFrequencySeekBar.setProgress(maxFrequency - MinFrequency);
		} else {
			accelerometerVendorView.setText("");
			mAccelerometerFrequencySeekBar.setVisibility(View.GONE);
			accelerometerFrequencyTextView.setVisibility(View.GONE);
		}


		Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		if (gyroscope != null) {
			gyroscopeVendorView.setText(gyroscope.getVendor() + " " + gyroscope.getName());

			mGyroscopeFrequencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					gyroscopeFrequencyTextView.setText((progress + MinFrequency) + " Hz");
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {

				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {

				}
			});

			int maxFrequency = 1000000 / gyroscope.getMinDelay();
			mGyroscopeFrequencySeekBar.setMax(maxFrequency - MinFrequency);
			mGyroscopeFrequencySeekBar.setProgress(maxFrequency - MinFrequency);
		} else {
			gyroscopeVendorView.setText("");
			mGyroscopeFrequencySeekBar.setVisibility(View.GONE);
			gyroscopeFrequencyTextView.setVisibility(View.GONE);
		}


		Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (magnetometer != null) {
			magnetometerVendorView.setText(magnetometer.getVendor() + " " + magnetometer.getName());

			mMagnetometerFrequencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					magnetometerFrequencyTextView.setText((progress + MinFrequency) + " Hz");
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {

				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {

				}
			});

			int maxFrequency = 1000000 / magnetometer.getMinDelay();
			mMagnetometerFrequencySeekBar.setMax(maxFrequency - MinFrequency);
			mMagnetometerFrequencySeekBar.setProgress(maxFrequency - MinFrequency);
		} else {
			magnetometerVendorView.setText("");
			mMagnetometerFrequencySeekBar.setVisibility(View.GONE);
			magnetometerFrequencyTextView.setVisibility(View.GONE);
		}
	}


	private String createZipFile(String directory, final ZipCreationTask.ZipCreationListener listener) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + directory + File.separator + "sensors-capture-" + sdf.format(new Date()) + ".zip";

		final ProgressDialog dialog = new ProgressDialog(this);
		dialog.setTitle("Zip creation");
		dialog.setMessage("Creating. Please wait...");
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setIndeterminate(false);
		dialog.setCancelable(true);
		dialog.setMax(ZipCreationTask.totalTasks);
		dialog.show();

		ZipCreationTask zipCreationTask = new ZipCreationTask(this, new ZipCreationTask.ZipCreationListener() {

			@Override
			public void onSubTaskFinished(int taskNumber) {
				dialog.setProgress(taskNumber);
				listener.onSubTaskFinished(taskNumber);
			}

			@Override
			public void onTaskFinished(String filePath) {
				dialog.dismiss();
				listener.onTaskFinished(filePath);
			}
		});
		zipCreationTask.execute(path);

		return path;
	}

	private void send() {

		createZipFile(".", new ZipCreationTask.ZipCreationListener() {
			@Override
			public void onSubTaskFinished(int taskNumber) {
				// Do nothing
			}

			@Override
			public void onTaskFinished(String filePath) {

				// Keep zip file path to delete it after send
				mZipFile = filePath;

				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				emailIntent.setType("plain/text");

				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Android Sensor logs from " + android.os.Build.MODEL);
				emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + new File(filePath).getAbsolutePath()));
				startActivityForResult(Intent.createChooser(emailIntent, "Send mail..."), 1);
			}
		});


	}

	private void save() {

		String directory = "SensorLogs";
		if (!new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + directory).mkdir()) {
			return;
		}


		createZipFile(directory, new ZipCreationTask.ZipCreationListener() {
			@Override
			public void onSubTaskFinished(int taskNumber) {
				// Do nothing
			}

			@Override
			public void onTaskFinished(String filePath) {
				Toast.makeText(getApplicationContext(), "File saved in " + filePath, Toast.LENGTH_LONG).show();
			}
		});
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode != 1) {
			return;
		}

		if (mZipFile == null || !new File(mZipFile).exists()) {
			return;
		}

		if (!new File(mZipFile).delete()) {
			Log.v("", "Should never be called");
			// should never be called
		}
	}


}
