package fr.inria.tyrex.senslogs.ui.utils;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;

import java.lang.reflect.Field;

public class Utils {

    // http://stackoverflow.com/questions/27004721/start-activity-from-fragment-using-transition-api-21-support#answer-28745491
    public static void startActivityForResult(Fragment fragment, Intent intent,
                                              int requestCode, Bundle options) {
        if (Build.VERSION.SDK_INT >= 19) {
            if ((requestCode & 0xffff0000) != 0) {
                throw new IllegalArgumentException("Can only use lower 16 bits" +
                        " for requestCode");
            }
            if (requestCode != -1) {
                try {
                    Field mIndex = Fragment.class.getDeclaredField("mIndex");
                    mIndex.setAccessible(true);
                    requestCode = ((mIndex.getInt(fragment) + 1) << 16) + (requestCode & 0xffff);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            ActivityCompat.startActivityForResult(fragment.getActivity(), intent,
                    requestCode, options);
        } else {
            fragment.getActivity().startActivityFromFragment(fragment, intent, requestCode);
        }
    }
}
