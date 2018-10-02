package fr.inria.tyrex.senslogs.ui;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import fr.inria.tyrex.senslogs.model.log.Log;
import fr.inria.tyrex.senslogs.model.sensors.NfcSensor;
import fr.inria.tyrex.senslogs.ui.dialog.FinishRecordDialog;
import fr.inria.tyrex.senslogs.ui.utils.StringsFormat;
import fr.inria.tyrex.senslogs.ui.utils.transitions.EnterSharedElementTextSizeHandler;

/**
 * This fragment is a layout for the {@link Recorder} with a timer and data size growing.
 */
public class RecordFragment extends Fragment {

    public final static String RESULT_LOG = "log";

    private Recorder mRecorder;
    private LogsManager mLogManager;

    private ImageView mStartPauseButton;
    private TextView mTimerTextView;
    private TextView mDataSizeTextView;
    private TextView mRecordCancelTextView;
    private TextView mRecordFinishTextView;
    private Button mRecordTimestampButton;

    private Handler mDataSizeHandler;
    private Handler mTimerHandler;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mRecorder = ((Application) getActivity().getApplication()).getRecorder();
        mLogManager = ((Application) getActivity().getApplication()).getLogsManager();

        mDataSizeHandler = new Handler();
        mTimerHandler = new Handler();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_record, parent, false);

        mStartPauseButton = rootView.findViewById(R.id.start_pause);
        mTimerTextView = rootView.findViewById(R.id.timer);
        mDataSizeTextView = rootView.findViewById(R.id.data_size);
        mRecordCancelTextView = rootView.findViewById(R.id.record_cancel);
        mRecordFinishTextView = rootView.findViewById(R.id.record_finish);
        mRecordTimestampButton = rootView.findViewById(R.id.record_timestamp);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            EnterSharedElementTextSizeHandler handler = new EnterSharedElementTextSizeHandler(getActivity());
            handler.addTextViewSizeResource(mTimerTextView, R.dimen.timer_small_text_size, R.dimen.timer_large_text_size);
        }

        mStartPauseButton.setOnClickListener(v -> {
            if (mRecorder.isRecording()) {
                onPauseClick();
            } else {
                onPlayClick();
            }
        });

        mRecordCancelTextView.setOnClickListener(v -> cancelAction());
        mRecordFinishTextView.setOnClickListener(v -> finishAction());

        mRecordFinishTextView.setEnabled(false);
        mRecordCancelTextView.setEnabled(false);


        mRecordTimestampButton.setOnClickListener(v -> mRecorder.addReference(0, 0, null));

        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();

        if (mRecorder.isRecording()) {
            mDataSizeHandler.post(mDataSizeRunnable);
            mTimerHandler.post(mTimerRunnable);
            if (mRecorder.isRecording(NfcSensor.getInstance()) && getActivity() != null) {
                NfcSensor.setupForegroundDispatch(getActivity());
            }
        }

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        mDataSizeHandler.removeCallbacks(mDataSizeRunnable);
        mTimerHandler.removeCallbacks(mTimerRunnable);
        if (mRecorder.isRecording(NfcSensor.getInstance()) && getActivity() != null) {
            NfcSensor.stopForegroundDispatch(getActivity());
        }
    }


    private void onPlayClick() {

        mStartPauseButton.setBackgroundResource(R.drawable.ic_record_pause);
        mStartPauseButton.setContentDescription(getString(R.string.record_pause));

        mRecordFinishTextView.setEnabled(false);
        mRecordCancelTextView.setEnabled(false);
        mRecordTimestampButton.setEnabled(true);

        try {
            mRecorder.play();
            mDataSizeHandler.post(mDataSizeRunnable);
            mTimerHandler.post(mTimerRunnable);
            if (mRecorder.isRecording(NfcSensor.getInstance()) && getActivity() != null) {
                NfcSensor.setupForegroundDispatch(getActivity());
            }
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

        mDataSizeHandler.removeCallbacks(mDataSizeRunnable);
        mTimerHandler.removeCallbacks(mTimerRunnable);
    }

    private Runnable mDataSizeRunnable = new Runnable() {
        private final DecimalFormat decimalFormat = new DecimalFormat("#0");

        @Override
        public void run() {
            mDataSizeTextView.setText(StringsFormat.getSize(getResources(),
                    mRecorder.getDataSize(), decimalFormat));

            mDataSizeHandler.postDelayed(this, 666);
        }
    };

    private Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            mTimerTextView.setText(StringsFormat.getDurationMs(mRecorder.getCurrentTime()));
            mTimerHandler.postDelayed(this, 73);
        }
    };


    public void cancelAction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage(R.string.record_cancelled_dialog_message)
                .setPositiveButton(R.string.record_cancelled_dialog_yes, (dialog, id) -> cancelRecorderConfirmed())
                .setNegativeButton(R.string.record_cancelled_dialog_no, (dialog, id) -> dialog.cancel())
                .show();

    }

    private void cancelRecorderConfirmed() {
        try {
            mDataSizeHandler.removeCallbacks(mDataSizeRunnable);
            mTimerHandler.removeCallbacks(mTimerRunnable);
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

        newFragment.setListener(value -> {
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
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            cancelAction();
        }

        return true;
    }

    public void onNewIntent(Intent intent) {
        // Handle NFC intents
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())
                && mRecorder.isRecording()) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            NfcSensor.getInstance().handleTag(tag);
        }
    }
}