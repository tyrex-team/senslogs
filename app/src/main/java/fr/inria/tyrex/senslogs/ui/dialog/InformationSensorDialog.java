package fr.inria.tyrex.senslogs.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.sensors.AndroidSensor;

;

/**
 * This dialog is called from sensor list to show sensor information
 */
public class InformationSensorDialog extends DialogFragment {

    private final static String BUNDLE_SENSOR = "sensor";

    public static InformationSensorDialog newInstance(AndroidSensor sensor) {
        InformationSensorDialog f = new InformationSensorDialog();

        Bundle args = new Bundle();
        args.putSerializable(BUNDLE_SENSOR, sensor);
        f.setArguments(args);

        return f;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AndroidSensor androidSensor = (AndroidSensor) getArguments().getSerializable(BUNDLE_SENSOR);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View v = View.inflate(getActivity(), R.layout.dialog_sensor_android_information, null);

        if (androidSensor != null) {

            Sensor sensor = androidSensor.getSensor();

            ((TextView) v.findViewById(R.id.sensor_vendor)).setText(sensor.getVendor());

            ((TextView) v.findViewById(R.id.sensor_min_delay)).setText(
                    String.format(getString(R.string.micro_seconds), sensor.getMinDelay()));

            ((TextView) v.findViewById(R.id.sensor_power)).setText(
                    String.format(getString(R.string.milli_ampere), sensor.getPower()));

            if (Build.VERSION.SDK_INT >= 20) {
                ((TextView) v.findViewById(R.id.sensor_type)).setText(sensor.getStringType());
            } else {
                v.findViewById(R.id.sensor_type_layout).setVisibility(View.GONE);
            }

            if (Build.VERSION.SDK_INT >= 21 && sensor.getMaxDelay() != 0) {
                ((TextView) v.findViewById(R.id.sensor_max_delay)).setText(
                        String.format(getString(R.string.micro_seconds), sensor.getMaxDelay()));
            } else {
                v.findViewById(R.id.sensor_max_delay_layout).setVisibility(View.GONE);
            }


            String sensorResolutionFormat = androidSensor.getUnitsStringFormat(getResources());

            ((TextView) v.findViewById(R.id.sensor_resolution)).setText(
                    Html.fromHtml(String.format(sensorResolutionFormat, sensor.getResolution())));

            builder.setTitle(androidSensor.getName());

        }

        builder.setView(v);
        builder.setPositiveButton(R.string.information_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        return builder.create();
    }
}