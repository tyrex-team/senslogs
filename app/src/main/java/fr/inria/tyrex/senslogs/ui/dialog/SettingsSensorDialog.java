package fr.inria.tyrex.senslogs.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import fr.inria.tyrex.senslogs.Application;
import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.control.PreferencesManager;
import fr.inria.tyrex.senslogs.model.sensors.AndroidSensor;
import fr.inria.tyrex.senslogs.model.sensors.CameraRecorder;
import fr.inria.tyrex.senslogs.model.sensors.LocationSensor;
import fr.inria.tyrex.senslogs.model.sensors.Sensor;

/**
 * This dialog is called when user set settings of a sensor
 */
public class SettingsSensorDialog extends DialogFragment {

    private final static String BUNDLE_SENSOR = "sensor";

    private View v;

    public static SettingsSensorDialog newInstance(Sensor sensor) {
        SettingsSensorDialog f = new SettingsSensorDialog();

        Bundle args = new Bundle();
        args.putSerializable(BUNDLE_SENSOR, sensor);
        f.setArguments(args);

        return f;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final PreferencesManager preferencesManager = ((Application) getActivity().getApplication()).
                getPreferences();

        final Sensor sensor = (Sensor) getArguments().getSerializable(BUNDLE_SENSOR);
        Sensor.Settings settings = preferencesManager.getSettings(sensor);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if (sensor instanceof AndroidSensor) {


            v = View.inflate(getActivity(), R.layout.dialog_sensor_settings, null);

            Spinner spinner = (Spinner) v.findViewById(R.id.settings_sensor_delay);
//            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
//                    android.R.layout.simple_spinner_dropdown_item,
//                    getResources().getStringArray(R.array.sensor_settings_delay));
//            spinner.setAdapter(adapter);

            AndroidSensor.Settings mobileSensorSettings = (AndroidSensor.Settings) settings;

            if (mobileSensorSettings != null) {
                SpinnerAdapter spinnerAdapter = spinner.getAdapter();
                int numberOfItems = spinnerAdapter.getCount();
                for (int i = 0; i < numberOfItems; i++) {
                    if (AndroidSensor.Settings.getDelayStringFromInteger(
                            mobileSensorSettings.sensorDelay).equals(spinnerAdapter.getItem(i))) {
                        spinner.setSelection(i);
                        break;
                    }
                }
            }

        } else if (sensor instanceof LocationSensor) {
            v = View.inflate(getActivity(), R.layout.dialog_sensor_location_settings, null);

            LocationSensor.Settings locationSensorSettings = (LocationSensor.Settings) settings;

            ((TextView) v.findViewById(R.id.settings_sensor_min_time)).
                    setText(String.format("%d", locationSensorSettings.minTime));
            ((TextView) v.findViewById(R.id.settings_sensor_min_distance)).
                    setText(String.format("%.1f", locationSensorSettings.minDistance));

        } else if (sensor instanceof CameraRecorder) {
            v = View.inflate(getActivity(), R.layout.dialog_sensor_camera_settings, null);

            CameraRecorder.Settings cameraSettings = (CameraRecorder.Settings) settings;

            if (cameraSettings != null) {

                Spinner spinnerQuality = (Spinner) v.findViewById(R.id.settings_sensor_camera_quality);
                CameraRecorder.OutputQuality[] qualities = CameraRecorder.OutputQuality.values();
                ArrayAdapter<CameraRecorder.OutputQuality> dataAdapterQuality =
                        new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, qualities);
                dataAdapterQuality.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerQuality.setAdapter(dataAdapterQuality);

                int numberOfItemsQuality = spinnerQuality.getCount();
                for (int i = 0; i < numberOfItemsQuality; i++) {
                    if (cameraSettings.outputQuality.equals(spinnerQuality.getItemAtPosition(i))) {
                        spinnerQuality.setSelection(i);
                        break;
                    }
                }


                Spinner spinnerAF = (Spinner) v.findViewById(R.id.settings_sensor_camera_af);
                CameraRecorder.AutoFocus[] afs = CameraRecorder.AutoFocus.values();
                ArrayAdapter<CameraRecorder.AutoFocus> dataAdapterAutoFocus =
                        new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, afs);
                dataAdapterAutoFocus.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerAF.setAdapter(dataAdapterAutoFocus);

                int numberOfItemsAF = spinnerAF.getCount();
                for (int i = 0; i < numberOfItemsAF; i++) {
                    if (cameraSettings.autoFocus.equals(spinnerAF.getItemAtPosition(i))) {
                        spinnerAF.setSelection(i);
                        break;
                    }
                }
            }


        } else {
            return builder.create();

        }

        builder.setTitle(R.string.settings_sensor_delay_title);
        builder.setView(v);
        builder.setPositiveButton(R.string.settings_ok, (dialog, which) -> {

                    Sensor.Settings settings1;

                    if (sensor instanceof AndroidSensor) {

                        Spinner spinner = v.findViewById(R.id.settings_sensor_delay);
                        String result = spinner.getSelectedItem().toString();
                        int delay = AndroidSensor.Settings.getDelayIntegerFromString(result);
                        settings1 = new AndroidSensor.Settings(delay);

                    } else if (sensor instanceof LocationSensor) {

                        String minTimeString = ((EditText) v.findViewById(R.id.settings_sensor_min_time)).
                                getText().toString();
                        String minDistanceString = ((EditText) v.findViewById(R.id.settings_sensor_min_distance)).
                                getText().toString();

                        settings1 = new LocationSensor.Settings(Long.valueOf(minTimeString),
                                Float.valueOf(minDistanceString));
                    } else {
                        Spinner spinnerQuality = v.findViewById(R.id.settings_sensor_camera_quality);
                        Spinner spinnerAF = v.findViewById(R.id.settings_sensor_camera_af);

                        settings1 = new CameraRecorder.Settings(
                                (CameraRecorder.OutputQuality) spinnerQuality.getSelectedItem(),
                                (CameraRecorder.AutoFocus) spinnerAF.getSelectedItem());
                    }
                    preferencesManager.setSettings(sensor, settings1);
                }

        );
        builder.setNegativeButton(R.string.settings_cancel, (dialog, which) -> {
        });
        return builder.create();
    }
}
