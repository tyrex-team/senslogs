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

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Main Activity of Application
 * <p/>
 * Created by Thibaud Michel on 13/01/15.
 */
public class RecordActivity extends Activity {

	private final static UUID PEBBLE_APP_UUID = UUID.fromString("943f3571-0f86-45a8-84be-1e329f649e9e");
	private static final int PEBBLE_KEY_TIME = 3;
	private static final int PEBBLE_KEY_STATE = 4;
	private static final int PEBBLE_KEY_BTN = 5;

	private final static int MinFrequency = 15;

	private RecordLogs mRecordLogs;
	private RecordMonitor mRecordMonitor;


	private boolean isRunning = false;
	private String mZipFile;

	private SeekBar mAccelerometerFrequencySeekBar,
			mGyroscopeFrequencySeekBar,
			mMagnetometerFrequencySeekBar;

	private TextView mTimerTextView;

	private ImageView mStartPauseButton;
	private TextView mResetButton;
	private TextView mSaveButton;
	private TextView mSendButton;

	private long mCounterTime = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		PebbleKit.startAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
		PebbleKit.registerReceivedDataHandler(this, mPebbleListener);

		//      http://stackoverflow.com/questions/9982433/android-accelerometer-not-working-when-screen-is-turned-off
		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


		mRecordLogs = RecordLogs.getInstance();
		mRecordMonitor = RecordMonitor.getInstance((SensorManager) getSystemService(Context.SENSOR_SERVICE),
		                                           (LocationManager) this.getSystemService(Context.LOCATION_SERVICE));


		initSensorDetails();

		mTimerTextView = (TextView) findViewById(R.id.timer);

		mStartPauseButton = (ImageView) findViewById(R.id.start_pause);
		mResetButton = (TextView) findViewById(R.id.reset);
		mSaveButton = (TextView) findViewById(R.id.save);
		mSendButton = (TextView) findViewById(R.id.send);

		mStartPauseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if (isRunning) {
					pause();
				} else {
					start();
				}
			}
		});

		mResetButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				mRecordLogs.reset();
				resetTimer();

				mResetButton.setEnabled(false);
				mSaveButton.setEnabled(false);
				mSendButton.setEnabled(false);

			}
		});


		mSendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				send();
			}
		});

		mSaveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				save();
			}
		});

	}

	private void start() {

		int accelerometerFrequency = mAccelerometerFrequencySeekBar.getProgress() + MinFrequency;
		int gyroscopeFrequency = mGyroscopeFrequencySeekBar.getProgress() + MinFrequency;
		int magnetometerFrequency = mMagnetometerFrequencySeekBar.getProgress() + MinFrequency;

		mRecordMonitor.start(accelerometerFrequency, gyroscopeFrequency, magnetometerFrequency);
		startTimer();

		mStartPauseButton.setBackgroundResource(R.drawable.pause_button);
		mStartPauseButton.setContentDescription(getString(R.string.pause));
		mResetButton.setEnabled(false);
		mSaveButton.setEnabled(false);
		mSendButton.setEnabled(false);
		isRunning = true;

		sendPebbleState("started");

	}

	private void pause() {

		stopTimer();
		mRecordMonitor.stop();

		mStartPauseButton.setBackgroundResource(R.drawable.start_button);
		mStartPauseButton.setContentDescription(getString(R.string.start));
		mResetButton.setEnabled(true);
		mSaveButton.setEnabled(true);
		mSendButton.setEnabled(true);
		isRunning = false;

		sendPebbleState("paused");
	}

	private void sendPebbleState(String state) {
		PebbleDictionary data = new PebbleDictionary();
		data.addString(PEBBLE_KEY_STATE, state);
		data.addUint32(PEBBLE_KEY_TIME, (int)mCounterTime);
		PebbleKit.sendDataToPebble(getApplicationContext(), PEBBLE_APP_UUID, data);
	}

	private PebbleKit.PebbleDataReceiver mPebbleListener = new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {

		@Override
		public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
			PebbleKit.sendAckToPebble(getApplicationContext(), transactionId);

			if(data.getInteger(PEBBLE_KEY_BTN) == 1) {
				if (isRunning) {
					pause();
				} else {
					start();
				}
			}

		}

	};


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

			mCounterTime = computeTime + System.currentTimeMillis() - firstTime;

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mTimerTextView.setText(dateFormatTimer.format(new Date(mCounterTime)));
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		PebbleKit.closeAppOnPebble(getApplicationContext(), PEBBLE_APP_UUID);
		unregisterReceiver(mPebbleListener);
	}
}
