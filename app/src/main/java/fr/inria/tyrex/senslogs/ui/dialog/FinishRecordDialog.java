package fr.inria.tyrex.senslogs.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import fr.inria.tyrex.senslogs.R;

/**
 * This dialog is called when user finished to record sensors
 */
public class FinishRecordDialog extends DialogFragment {

    private OnDialogResultListener mListener;


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final EditText editText =
                (EditText) View.inflate(getActivity(), R.layout.dialog_finish_record, null);

        editText.requestFocus();
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});

        builder.setView(editText);

        builder.setTitle(R.string.record_finished_dialog_title);

        builder.setPositiveButton(R.string.record_finished_dialog_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener != null) {
                            mListener.onPositiveResult(editText.getText().toString().trim());
                        }
                    }
                });

        builder.setNegativeButton(R.string.record_finished_dialog_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        final AlertDialog dialog = builder.create();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        return dialog;
    }

    public void setListener(OnDialogResultListener listener) {
        mListener = listener;
    }

    public interface OnDialogResultListener {
        void onPositiveResult(String value);
    }
}