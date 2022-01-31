package fr.inria.tyrex.senslogs.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import fr.inria.tyrex.senslogs.Application;
import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.control.Recorder;
import fr.inria.tyrex.senslogs.control.SensorsManager;
import fr.inria.tyrex.senslogs.model.log.CalibrationLog;
import fr.inria.tyrex.senslogs.model.sensors.Sensor;
import fr.inria.tyrex.senslogs.model.sensors.AndroidSensor;

/**
 * This dialog is called when user set settings of a sensor
 */
public class CalibrationSensorDialog extends DialogFragment {

    private final static String BUNDLE_SENSOR = "sensor";

    private SensorsManager mSensorsManager;
    private Recorder mRecorder;
    private Sensor mSensor;

    private View mStartCalibration;
    private View mPauseCalibration;
    private boolean isSaving = false;

    public static CalibrationSensorDialog newInstance(Sensor sensor) {
        CalibrationSensorDialog f = new CalibrationSensorDialog();
        f.mSensor = sensor;
        return f;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if (!(mSensor instanceof AndroidSensor)) {
            return builder.create();
        }

        View rootView = View.inflate(getActivity(), R.layout.dialog_sensor_calibration, null);

        TextView helpMessage = rootView.findViewById(R.id.calibration_help_message);
        switch (mSensor.getType()) {
            case android.hardware.Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
                helpMessage.setText(R.string.calibration_accelerometer_help);
                break;
            case android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                helpMessage.setText(R.string.calibration_gyroscope_help);
                break;
            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                helpMessage.setText(R.string.calibration_magnetometer_help);
                break;
        }

        mStartCalibration = rootView.findViewById(R.id.calibration_start);
        mPauseCalibration = rootView.findViewById(R.id.calibration_pause);

        mStartCalibration.setOnClickListener(v -> {
            mStartCalibration.setVisibility(View.GONE);
            mPauseCalibration.setVisibility(View.VISIBLE);

            startCalibration();
        });

        mPauseCalibration.setOnClickListener(v -> {
            AlertDialog dialog = (AlertDialog) getDialog();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);

            mPauseCalibration.setVisibility(View.GONE);

            pauseCalibration();
        });


        builder.setTitle(String.format(getString(R.string.calibration_title), mSensor.getName()));
        builder.setView(rootView);
        builder.setPositiveButton(R.string.calibration_save, null);
        builder.setNegativeButton(R.string.calibration_cancel, null);
        builder.setNeutralButton(R.string.calibration_restart, null);

        return builder.create();
    }


    private int mPreviousScreenOrientation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreviousScreenOrientation = getActivity().getRequestedOrientation();
        getActivity().setRequestedOrientation(
                getActivity().getResources().getConfiguration().orientation);

    }

    @Override
    public void onStart() {
        super.onStart();

        mRecorder = ((Application) getActivity().getApplication()).getRecorder();
        mSensorsManager = ((Application) getActivity().getApplication()).getSensorsManager();

        AlertDialog dialog = (AlertDialog) getDialog();

        if (dialog == null) {
            return;
        }

//        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);

        final Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        final Button restartButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        saveButton.setEnabled(false);
        restartButton.setEnabled(false);

        saveButton.setOnClickListener(v -> {
            saveCalibration();
            dismiss();
        });
        restartButton.setOnClickListener(v -> {
            mStartCalibration.setVisibility(View.VISIBLE);
            mPauseCalibration.setVisibility(View.GONE);

            saveButton.setEnabled(false);
            restartButton.setEnabled(false);

            restartCalibration();
        });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (isSaving || !mRecorder.isRecording()) {
            return;
        }

        try {
            mRecorder.cancel();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().setRequestedOrientation(mPreviousScreenOrientation);
    }


    private void startCalibration() {
        Map<Sensor, Sensor.Settings> sensorsToRecord = new HashMap<>();

        sensorsToRecord.put(mSensor, new AndroidSensor.Settings(SensorManager.SENSOR_DELAY_FASTEST));

        CalibrationLog.Type calibration;

        if (mSensor.getType() == android.hardware.Sensor.TYPE_ACCELEROMETER_UNCALIBRATED) {
            Sensor mGyroSensor = mSensorsManager.getSensorByType(android.hardware.Sensor.TYPE_GYROSCOPE);
            sensorsToRecord.put(mGyroSensor, new AndroidSensor.Settings(SensorManager.SENSOR_DELAY_FASTEST));
            calibration = CalibrationLog.Type.ACCELEROMETER;
        } else if (mSensor.getType() == android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
            calibration = CalibrationLog.Type.GYROSCOPE;
        } else if (mSensor.getType() == android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED) {
            calibration = CalibrationLog.Type.MAGNETOMETER;
        } else {
            throw new RuntimeException("Should never be called");
        }

        try {
            mRecorder.play(sensorsToRecord, calibration);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void pauseCalibration() {
        mRecorder.pause();
    }

    private void restartCalibration() {
        try {
            isSaving = false;
            mRecorder.cancel();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveCalibration() {
        String date = new SimpleDateFormat(getString(R.string.date_format_file_default),
                Locale.US).format(new Date());
        int folderId;
        switch (mSensor.getType()) {
            case android.hardware.Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
                folderId = R.string.folder_calibration_accelerometer;
                break;
            case android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                folderId = R.string.folder_calibration_gyroscope;
                break;
            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                folderId = R.string.folder_calibration_magnetometer;
                break;
            default:
                return;
        }

        try {
            mRecorder.save(String.format(getString(folderId), date));
            isSaving = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
