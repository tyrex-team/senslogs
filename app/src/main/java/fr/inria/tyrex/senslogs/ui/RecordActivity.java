package fr.inria.tyrex.senslogs.ui;

import android.content.Intent;

import androidx.fragment.app.Fragment;

import fr.inria.tyrex.senslogs.ui.utils.SingleFragmentActivity;

/**
 * This activity handles only {@link SingleFragmentActivity}
 */
public class RecordActivity extends SingleFragmentActivity {

    RecordFragment fragment = new RecordFragment();

    @Override
    protected Fragment createFragment() {
        return fragment = new RecordFragment();
    }


    @Override
    public void onBackPressed() {
        fragment.cancelAction();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        fragment.onNewIntent(intent);
    }

}