package fr.inria.tyrex.senslogs.ui.utils;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import fr.inria.tyrex.senslogs.R;

/**
 * An activity with a single fragment
 * https://github.com/tkunstek/android-big-nerd-ranch/blob/master/20_CameraImage_CriminalIntent/src/com/bignerdranch/android/criminalintent/SingleFragmentActivity.java
 */
public abstract class SingleFragmentActivity extends AppCompatActivity {
    protected abstract Fragment createFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragment = manager.findFragmentById(R.id.fragmentContainer);

        if (fragment == null) {
            fragment = createFragment();
            manager.beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .commit();
        }
    }
}
