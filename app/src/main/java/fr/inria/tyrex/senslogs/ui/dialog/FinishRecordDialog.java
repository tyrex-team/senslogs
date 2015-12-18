package fr.inria.tyrex.senslogs.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.EditText;

import fr.inria.tyrex.senslogs.R;

/**
 * This dialog is called when user finished to record sensors
 */
public class FinishRecordDialog extends DialogFragment {

    private OnDialogResultListener mListener;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final EditText editText = new EditText(getActivity());
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.requestFocus();

        builder.setView(editText);

        builder.setTitle(R.string.record_finish_dialog_title);

        builder.setPositiveButton(R.string.record_finish_dialog_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener != null) {
                            mListener.onPositiveResult(editText.getText().toString().trim());
                        }
                    }
                });

        builder.setNegativeButton(R.string.record_finish_dialog_cancel,
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