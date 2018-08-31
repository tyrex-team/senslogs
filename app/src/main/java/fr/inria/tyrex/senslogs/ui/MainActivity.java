package fr.inria.tyrex.senslogs.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.ui.utils.SingleFragmentActivity;

/**
 * This activity handles only {@link MainFragment}
 */
public class MainActivity extends SingleFragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(R.string.activity_main);
    }

    @Override
    protected Fragment createFragment() {
        return new MainFragment();
    }
}