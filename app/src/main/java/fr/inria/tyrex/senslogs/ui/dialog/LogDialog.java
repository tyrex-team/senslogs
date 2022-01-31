package fr.inria.tyrex.senslogs.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.util.Locale;

import fr.inria.tyrex.senslogs.Application;
import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.control.ZipCreationTask;
import fr.inria.tyrex.senslogs.model.log.Log;
import fr.inria.tyrex.senslogs.ui.utils.StringsFormat;

/**
 * This dialog is called when user want details on a log
 */
public class LogDialog extends DialogFragment {

    private final static String BUNDLE_LOG = "log";

    private Log mLog;
    private ZipCreationTask.ZipCreationListener mCreationTaskListener;

    private OnDialogResultListener mListener;

    public static LogDialog newInstance(Log log) {
        LogDialog f = new LogDialog();

        Bundle args = new Bundle();
        args.putSerializable(BUNDLE_LOG, log);
        f.setArguments(args);

        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        mLog = (Log) getArguments().getSerializable(BUNDLE_LOG);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View v = View.inflate(getActivity(), R.layout.dialog_log_information, null);

        ((TextView) v.findViewById(R.id.log_file_name)).setText(mLog.getZipFile().getName());
        final TextView logCompressedTextView = v.findViewById(R.id.log_compressed);
        logCompressedTextView.setText(StringsFormat.getSize(getResources(), mLog.getCompressedSize()));
        ((TextView) v.findViewById(R.id.log_uncompressed)).setText(StringsFormat.getSize(getResources(),
                mLog.getUncompressedSize()));

        long diff = (long) (mLog.getRecordTimes().endTime - mLog.getRecordTimes().startTime);
        ((TextView) v.findViewById(R.id.log_duration)).setText(StringsFormat.getDuration(diff));

        String startTime = DateFormat.getDateTimeInstance().format(
                mLog.getRecordTimes().startTime * 1000);
        ((TextView) v.findViewById(R.id.log_start_time)).setText(startTime);


        int usedSensors = mLog.getSensors().size();
        int totalSensors = ((Application) getActivity().getApplication())
                .getSensorsManager().getAvailableSensors().size();
        ((TextView) v.findViewById(R.id.log_num_sensors)).setText(
                String.format(Locale.US, "%d/%d", usedSensors, totalSensors));


        v.findViewById(R.id.log_share).setOnClickListener(v11 -> {
            dismiss();
            if (mListener != null) {
                mListener.onShareResult(mLog);
            }
        });

        v.findViewById(R.id.log_delete).setOnClickListener(v12 -> {
            dismiss();
            if (mListener != null) {
                mListener.onDeleteResult(mLog);
            }
        });
        v.findViewById(R.id.log_copy_to_sd_card).setOnClickListener(v13 -> {
            dismiss();
            if (mListener != null) {
                mListener.onCopyToSdCardResult(mLog);
            }
        });

        if (mLog.getCreationTask() != null) {

            final ProgressBar progressBar = v.findViewById(R.id.log_progress_bar);

            progressBar.setVisibility(View.VISIBLE);
            logCompressedTextView.setVisibility(View.GONE);
            mLog.getCreationTask().addListener(mCreationTaskListener =
                    new ZipCreationTask.ZipCreationListener() {
                        @Override
                        public void onProgress(File currentFile, float ratio) {
                            progressBar.setProgress((int) (ratio * 100));
                        }

                        @Override
                        public void onTaskFinished(File outputFile, long fileSize) {
                            if (!isAdded()) {
                                return;
                            }
                            progressBar.setVisibility(View.GONE);
                            logCompressedTextView.setVisibility(View.VISIBLE);
                            logCompressedTextView.setText(StringsFormat.getSize(getResources(), fileSize));
                        }
                    });
        }


        builder.setView(v);
        builder.setTitle(mLog.getName());
        return builder.create();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCreationTaskListener != null && mLog.getCreationTask() != null) {
            mLog.getCreationTask().removeListener(mCreationTaskListener);
        }
    }

    public void setListener(OnDialogResultListener listener) {
        mListener = listener;
    }

    public interface OnDialogResultListener {
        void onDeleteResult(Log log);

        void onShareResult(Log log);

        void onCopyToSdCardResult(Log log);
    }
}