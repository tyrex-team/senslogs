package fr.inria.tyrex.senslogs.ui;

import android.support.v4.app.Fragment;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.ui.utils.SingleFragmentActivity;

/**
 * This activity handles only {@link LogsFragment}
 */
public class LogsActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new LogsFragment();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
