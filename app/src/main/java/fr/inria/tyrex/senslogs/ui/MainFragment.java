package fr.inria.tyrex.senslogs.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import fr.inria.tyrex.senslogs.Application;
import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.control.LogsManager;
import fr.inria.tyrex.senslogs.control.PreferencesManager;
import fr.inria.tyrex.senslogs.control.Recorder;
import fr.inria.tyrex.senslogs.control.SensorsManager;
import fr.inria.tyrex.senslogs.model.Log;
import fr.inria.tyrex.senslogs.model.Sensor;
import fr.inria.tyrex.senslogs.model.sensors.AndroidSensor;
import fr.inria.tyrex.senslogs.ui.dialog.CalibrationSensorDialog;
import fr.inria.tyrex.senslogs.ui.dialog.InformationSensorDialog;
import fr.inria.tyrex.senslogs.ui.dialog.SettingsSensorDialog;
import fr.inria.tyrex.senslogs.ui.listadapter.SensorListAdapter;

import static fr.inria.tyrex.senslogs.ui.listadapter.SensorListAdapter.Group;

/**
 * This fragment shows a list of available sensors by their category. User can select any sensors
 * in order to record its data. User can also modify sensors properties.
 */
public class MainFragment extends Fragment {

    private final static int REQUEST_CODE_RECORDER = 1;
    private final static int REQUEST_CODE_LOGS = 2;

    private static final int MY_PERMISSIONS_REQUEST = 3;

    private SensorsManager mSensorsManager;
    private PreferencesManager mPreferencesManager;
    private Recorder mRecorder;
    private LogsManager mLogManager;

    private View mRootView;
    private MenuItem mActionLogMenuItem;

    private CompoundButton mCheckboxAskingForPermission;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mSensorsManager = ((Application) getActivity().getApplication()).getSensorsManager();
        mPreferencesManager = ((Application) getActivity().getApplication()).getPreferences();
        mRecorder = ((Application) getActivity().getApplication()).getRecorder();
        mLogManager = ((Application) getActivity().getApplication()).getLogsManager();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_sensors, parent, false);

        fillList();

        mRootView.findViewById(R.id.start_pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlayClick();
            }
        });

        return mRootView;
    }


    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.main, menu);

        mActionLogMenuItem = menu.findItem(R.id.action_logs);

        mActionLogMenuItem.getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu.performIdentifierAction(mActionLogMenuItem.getItemId(), 0);
            }
        });

        mActionLogMenuItem.setVisible(mLogManager.getLogs().size() != 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_logs:
                startLogsActivity(null);
                break;
        }

        return true;
    }


    private void fillList() {

        List<Sensor> availableSensors = mSensorsManager.getAvailableSensors();

        Collections.sort(availableSensors, new Comparator<Sensor>() {
            @Override
            public int compare(Sensor sensor1, Sensor sensor2) {
                int compareCategoryResult = Sensor.Category.compareTo(
                        sensor1.getCategory(), sensor2.getCategory());
                if (compareCategoryResult != 0) {
                    return compareCategoryResult;
                }
                return sensor1.getName().
                        compareTo(sensor2.getName());
            }
        });


        LinkedList<Group> mGroupList = new LinkedList<>();

        Sensor.Category lastCategory = null;
        for (Sensor sensor : availableSensors) {
            if (lastCategory != sensor.getCategory()) {
                mGroupList.add(new Group(sensor.getCategory()));
            }
            mGroupList.getLast().sensors.add(sensor);
            lastCategory = sensor.getCategory();
        }

        final SensorListAdapter listAdapter = new SensorListAdapter(getActivity(), mGroupList,
                mPreferencesManager, mSensorsManager);
        final ExpandableListView listViewSensors = (ExpandableListView) mRootView.findViewById(R.id.sensors_list);
        listViewSensors.setAdapter(listAdapter);

        listViewSensors.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                onAndroidSensorClick((Sensor) listAdapter.getChild(groupPosition, childPosition));
                return true;
            }
        });

        listAdapter.setOnSettingsClickListener(new SensorListAdapter.SettingsClickListener() {
            @Override
            public void onSettingsClick(Sensor sensor) {
                MainFragment.this.onSettingsClick(sensor);
            }
        });

        listAdapter.setOnCalibrationClickListener(new SensorListAdapter.CalibrationClickListener() {
            @Override
            public void onCalibrationClick(Sensor sensor) {
                MainFragment.this.onCalibrationClick(sensor);
            }
        });

        listAdapter.setOnCheckboxClickListener(new SensorListAdapter.CheckboxClickListener() {
            @Override
            public void onCheckboxClick(Sensor sensor, boolean isChecked, CompoundButton view) {
                MainFragment.this.onCheckboxClick(sensor, isChecked, view);
            }
        });
    }

    private void onAndroidSensorClick(Sensor sensor) {
        if (!(sensor instanceof AndroidSensor)) {
            return;
        }
        FragmentManager fm = getFragmentManager();
        DialogFragment newFragment = InformationSensorDialog.newInstance(
                (AndroidSensor) sensor);
        newFragment.show(fm, "fragment_information_sensor");
    }

    private void onSettingsClick(Sensor sensor) {
        FragmentManager fm = getFragmentManager();
        DialogFragment newFragment = SettingsSensorDialog.newInstance(sensor);
        newFragment.show(fm, "fragment_settings_sensor");
    }

    private void onCalibrationClick(Sensor sensor) {
        FragmentManager fm = getFragmentManager();
        DialogFragment newFragment = CalibrationSensorDialog.newInstance(sensor);
        newFragment.show(fm, "fragment_calibration_sensor");
    }

    public void onCheckboxClick(Sensor sensor, boolean isChecked, CompoundButton view) {
        mPreferencesManager.setChecked(sensor, isChecked);

        mCheckboxAskingForPermission = null;
        if (isChecked && !verifyPermissions(sensor)) {
            view.setChecked(false);
            mCheckboxAskingForPermission = view;
        }
    }


    private void onPlayClick() {


        try {
            mRecorder.play();
        } catch (IOException e) {
            e.printStackTrace();
            android.util.Log.e(Application.LOG_TAG, "Something bad happened with file creation");
        }

        Intent intent = new Intent(getActivity(), RecordActivity.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            View playerView = mRootView.findViewById(R.id.player);
            View startPauseButton = mRootView.findViewById(R.id.start_pause);
            View timerView = mRootView.findViewById(R.id.timer);

            Pair<View, String> pair1 = Pair.create(playerView, playerView.getTransitionName());
            Pair<View, String> pair2 = Pair.create(startPauseButton, startPauseButton.getTransitionName());
            Pair<View, String> pair3 = Pair.create(timerView, timerView.getTransitionName());

            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                    pair1, pair2, pair3);
//            Utils.startActivityForResult(this, intent, REQUEST_CODE_RECORDER, options.toBundle());
            getActivity().startActivityFromFragment(this, intent, REQUEST_CODE_RECORDER, options.toBundle());

        } else {
            startActivityForResult(intent, REQUEST_CODE_RECORDER);
        }
    }



    private boolean verifyPermissions(Sensor sensor) {

        String permission = null;
        switch (sensor.getType()) {
            case Sensor.TYPE_WIFI:
            case Sensor.TYPE_LOCATION_CELL_WIFI:
                if (ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    permission = Manifest.permission.ACCESS_COARSE_LOCATION;
                }
                break;
            case Sensor.TYPE_LOCATION_GPS:
            case Sensor.TYPE_LOCATION_PASSIVE:
            case Sensor.TYPE_NMEA:
                if (ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    permission = Manifest.permission.ACCESS_FINE_LOCATION;
                }
                break;
            case Sensor.TYPE_BLUETOOTH:
                if (ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.BLUETOOTH)
                        != PackageManager.PERMISSION_GRANTED) {
                    permission = Manifest.permission.BLUETOOTH;
                }
                break;
            case Sensor.TYPE_NFC:
                if (ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.NFC)
                        != PackageManager.PERMISSION_GRANTED) {
                    permission = Manifest.permission.NFC;
                }
                break;
        }

        if (permission == null) {
            return true;
        }

        requestPermissions(new String[]{permission}, MY_PERMISSIONS_REQUEST);

        return false;

    }

    private void startLogsActivity(Log log) {

        Intent intent = new Intent(getActivity(), LogsActivity.class);

        if (log != null) {
            intent.putExtra(LogsFragment.INPUT_LOG, mLogManager.getLogs().indexOf(log));
        }

        startActivityForResult(intent, REQUEST_CODE_LOGS);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_CODE_RECORDER && requestCode != REQUEST_CODE_LOGS) {
            return;
        }

        if (requestCode == REQUEST_CODE_RECORDER &&
                resultCode == Activity.RESULT_OK &&
                data != null &&
                data.hasExtra(RecordFragment.RESULT_LOG)) {

            final Log log = mLogManager.getLogs().get(data.getIntExtra(RecordFragment.RESULT_LOG, -1));

            if (log != null) {

                Snackbar snackbar = Snackbar
                        .make(mRootView, Html.fromHtml(String.format(
                                        getString(R.string.record_data_saved), log.getName())),
                                Snackbar.LENGTH_LONG)
                        .setAction(R.string.record_data_saved_see, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                startLogsActivity(log);
                            }
                        });
                snackbar.show();
            }
        }

        if (mActionLogMenuItem != null &&
                !mActionLogMenuItem.isVisible() &&
                mLogManager.getLogs().size() > 0) {

            mActionLogMenuItem.setVisible(true);
            Animation fade_in = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
            fade_in.setInterpolator(new AccelerateInterpolator());
            fade_in.setDuration(1000);

            mActionLogMenuItem.getActionView().startAnimation(fade_in);

        } else if (mActionLogMenuItem != null && mLogManager.getLogs().size() == 0) {
            mActionLogMenuItem.setVisible(false);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST && grantResults.length > 0) {
            mCheckboxAskingForPermission.setChecked(grantResults[0] == PackageManager.PERMISSION_GRANTED);
        }
    }

}