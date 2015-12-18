package fr.inria.tyrex.senslogs.ui;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DecimalFormat;

import fr.inria.tyrex.senslogs.Application;
import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.control.LogsManager;
import fr.inria.tyrex.senslogs.control.PositionReferenceManager;
import fr.inria.tyrex.senslogs.control.Recorder;
import fr.inria.tyrex.senslogs.model.Log;
import fr.inria.tyrex.senslogs.model.PositionReference;
import fr.inria.tyrex.senslogs.ui.dialog.FinishRecordDialog;
import fr.inria.tyrex.senslogs.ui.utils.StringsFormat;
import fr.inria.tyrex.senslogs.ui.utils.transitions.EnterSharedElementTextSizeHandler;

/**
 * This fragment is a layout for the {@link Recorder} with a timer and data size growing.
 */
public class RecordFragment extends Fragment {

    private final static int mNotificationId = 1;
    public final static String RESULT_LOG = "log";

    private Recorder mRecorder;
    private LogsManager mLogManager;

    private boolean hasNotification;

    private ImageView mStartPauseButton;
    private TextView mTimerTextView;
    private TextView mDataSizeTextView;
    private TextView mRecordCancelTextView;
    private TextView mRecordFinishTextView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mRecorder = ((Application) getActivity().getApplication()).getRecorder();
        mLogManager = ((Application) getActivity().getApplication()).getLogsManager();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_record, parent, false);

        mStartPauseButton = (ImageView) rootView.findViewById(R.id.start_pause);
        mTimerTextView = (TextView) rootView.findViewById(R.id.timer);
        mDataSizeTextView = (TextView) rootView.findViewById(R.id.data_size);
        final Spinner positionsListSpinner = (Spinner) rootView.findViewById(R.id.record_position_list);
        mRecordCancelTextView = (TextView) rootView.findViewById(R.id.record_cancel);
        mRecordFinishTextView = (TextView) rootView.findViewById(R.id.record_finish);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            EnterSharedElementTextSizeHandler handler = new EnterSharedElementTextSizeHandler(getActivity());
            handler.addTextViewSizeResource(mTimerTextView, R.dimen.timer_small_text_size, R.dimen.timer_large_text_size);
        }

        mStartPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecorder.isRecording()) {
                    onPauseClick();
                } else {
                    onPlayClick();
                }
            }
        });

        mRecordCancelTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAction();
            }
        });
        mRecordFinishTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAction();
            }
        });

        mRecordFinishTextView.setEnabled(false);
        mRecordCancelTextView.setEnabled(false);

        ArrayAdapter<PositionReference> positionListAdapter = new ArrayAdapter<>
                (getActivity(), android.R.layout.simple_spinner_item,
                        PositionReferenceManager.getPositionReferences());
        positionsListSpinner.setAdapter(positionListAdapter);


        rootView.findViewById(R.id.record_position_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PositionReference ref =
                                (PositionReference) positionsListSpinner.getSelectedItem();
                        mRecorder.addReference(System.currentTimeMillis(), ref);
                    }
                });

        TextView numRecordingSensorsTextView = (TextView) rootView.findViewById(R.id.num_recording_sensors);
        numRecordingSensorsTextView.setText(String.format(
                getString(R.string.record_num_recording_sensors),
                mRecorder.getSelectedSensors().size(),
                ((Application) getActivity().getApplication()).getAvailableSensors().size()
        ));


        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();
        mRecorder.setTimerListener(timerListener);
        mRecorder.setDataSizeListener(dataSizeListener);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (hasNotification) {
            removeNotification();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        mRecorder.setTimerListener(null);
        mRecorder.setDataSizeListener(null);

        if (mRecorder.isRecording() && !hasNotification) {
            createNotification();
        }
    }

    private void onPlayClick() {

        mStartPauseButton.setBackgroundResource(R.drawable.pause_button);
        mStartPauseButton.setContentDescription(getString(R.string.record_pause));

        mRecordFinishTextView.setEnabled(false);
        mRecordCancelTextView.setEnabled(false);

        mRecorder.resume();
    }

    private void onPauseClick() {

        mStartPauseButton.setBackgroundResource(R.drawable.start_button);
        mStartPauseButton.setContentDescription(getString(R.string.record_start));

        mRecordFinishTextView.setEnabled(true);
        mRecordCancelTextView.setEnabled(true);

        mRecorder.pause();

    }

    private Recorder.DataSizeListener dataSizeListener = new Recorder.DataSizeListener() {

        private final DecimalFormat decimalFormat = new DecimalFormat("#0");

        @Override
        public void onNewTotalSize(long totalSize) {
            mDataSizeTextView.setText(StringsFormat.getSize(getResources(), totalSize, decimalFormat));
        }
    };

    private Recorder.TimerListener timerListener = new Recorder.TimerListener() {


        @Override
        public void onNewTime(long diffTime) {
            mTimerTextView.setText(StringsFormat.getDurationMs(diffTime));
        }

        @Override
        public void onReset() {
            mTimerTextView.setText(R.string.default_timer_millisec);
        }
    };


    private void cancelAction() {
        cancelRecorder();
        getActivity().supportFinishAfterTransition();
    }

    public void cancelRecorder() {
        try {
            mRecorder.stop();
            mRecorder.cancel();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Something bad happened with file creation",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void finishAction() {
        try {
            mRecorder.stop();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Something bad happened with file creation",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        FragmentManager fm = getActivity().getSupportFragmentManager();
        FinishRecordDialog newFragment = new FinishRecordDialog();
        newFragment.show(fm, "fragment_record_finish");

        newFragment.setListener(new FinishRecordDialog.OnDialogResultListener() {
            @Override
            public void onPositiveResult(String value) {
                try {
                    Log log = mRecorder.save(value);

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(RESULT_LOG, mLogManager.getLogs().indexOf(log));
                    getActivity().setResult(Activity.RESULT_OK, resultIntent);

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "Something bad happened with file creation",
                            Toast.LENGTH_SHORT).show();
                }

                getActivity().supportFinishAfterTransition();
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            cancelAction();
        }

        return true;
    }


    private void createNotification() {

        Intent intent = new Intent(getActivity(), RecordActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getActivity())
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.record_notification))
                        .setContentIntent(pendingIntent);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;

        NotificationManager mNotificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(mNotificationId, notification);

        hasNotification = true;
    }

    private void removeNotification() {
        NotificationManager mNotificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(mNotificationId);

        hasNotification = false;
    }


}