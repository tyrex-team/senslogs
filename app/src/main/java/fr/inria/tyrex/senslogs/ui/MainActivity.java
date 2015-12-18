package fr.inria.tyrex.senslogs.ui;

import android.support.v4.app.Fragment;

import fr.inria.tyrex.senslogs.ui.utils.SingleFragmentActivity;

/**
 * This activity handles only {@link MainFragment}
 */
public class MainActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new MainFragment();
    }
}