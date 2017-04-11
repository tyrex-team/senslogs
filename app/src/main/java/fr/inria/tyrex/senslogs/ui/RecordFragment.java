package fr.inria.tyrex.senslogs.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;

import fr.inria.tyrex.senslogs.Application;
import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.control.LogsManager;
import fr.inria.tyrex.senslogs.control.Recorder;
import fr.inria.tyrex.senslogs.control.SensorsManager;
import fr.inria.tyrex.senslogs.model.Log;
import fr.inria.tyrex.senslogs.ui.dialog.FinishRecordDialog;
import fr.inria.tyrex.senslogs.ui.utils.StringsFormat;
import fr.inria.tyrex.senslogs.ui.utils.transitions.EnterSharedElementTextSizeHandler;

/**
 * This fragment is a layout for the {@link Recorder} with a timer and data size growing.
 */
public class RecordFragment extends Fragment {

    public final static String RESULT_LOG = "log";

    private SensorsManager mSensorsManager;
    private Recorder mRecorder;
    private LogsManager mLogManager;

    private ImageView mStartPauseButton;
    private TextView mTimerTextView;
    private TextView mDataSizeTextView;
    private TextView mRecordCancelTextView;
    private TextView mRecordFinishTextView;
    private Button mRecordTimestampButton;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mSensorsManager = ((Application) getActivity().getApplication()).getSensorsManager();
        mRecorder = ((Application) getActivity().getApplication()).getRecorder();
        mLogManager = ((Application) getActivity().getApplication()).getLogsManager();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_record, parent, false);

        mStartPauseButton = (ImageView) rootView.findViewById(R.id.start_pause);
        mTimerTextView = (TextView) rootView.findViewById(R.id.timer);
        mDataSizeTextView = (TextView) rootView.findViewById(R.id.data_size);
        mRecordCancelTextView = (TextView) rootView.findViewById(R.id.record_cancel);
        mRecordFinishTextView = (TextView) rootView.findViewById(R.id.record_finish);
        mRecordTimestampButton = (Button) rootView.findViewById(R.id.record_timestamp);


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


        mRecordTimestampButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRecorder.addReference(0, 0, null);
            }
        });

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

    }

    @Override
    public void onPause() {
        super.onPause();
        mRecorder.setTimerListener(null);
        mRecorder.setDataSizeListener(null);
    }


    private void onPlayClick() {

        mStartPauseButton.setBackgroundResource(R.drawable.ic_record_pause);
        mStartPauseButton.setContentDescription(getString(R.string.record_pause));

        mRecordFinishTextView.setEnabled(false);
        mRecordCancelTextView.setEnabled(false);
        mRecordTimestampButton.setEnabled(true);

        try {
            mRecorder.play();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void onPauseClick() {

        mStartPauseButton.setBackgroundResource(R.drawable.ic_record);
        mStartPauseButton.setContentDescription(getString(R.string.record_start));

        mRecordFinishTextView.setEnabled(true);
        mRecordCancelTextView.setEnabled(true);
        mRecordTimestampButton.setEnabled(false);

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


    public void cancelAction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage(R.string.record_cancelled_dialog_message)
                .setPositiveButton(R.string.record_cancelled_dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        cancelRecorderConfirmed();
                    }
                })
                .setNegativeButton(R.string.record_cancelled_dialog_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();

    }

    private void cancelRecorderConfirmed() {
        try {
            mRecorder.cancel();
        } catch (IOException e) {
            e.printStackTrace();
            android.util.Log.e(Application.LOG_TAG, "Something bad happened with file creation");
        }

        getActivity().setResult(Activity.RESULT_CANCELED);
        getActivity().supportFinishAfterTransition();
    }

    private void finishAction() {

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
                    android.util.Log.e(Application.LOG_TAG, "Something bad happened with file creation");
                    e.printStackTrace();
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

}