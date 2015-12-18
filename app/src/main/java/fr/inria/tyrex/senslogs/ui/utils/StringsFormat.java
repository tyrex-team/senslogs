package fr.inria.tyrex.senslogs.ui.utils;

import android.content.res.Resources;

import java.text.DecimalFormat;

import fr.inria.tyrex.senslogs.R;

/**
 * An helper to format strings for this app
 */
public class StringsFormat {

    private final static DecimalFormat defaultDecimalFormat = new DecimalFormat("#0.00");


    public static String getSize(Resources res, double size) {
        return getSize(res, size, defaultDecimalFormat);
    }

    public static String getSize(Resources res, double size, DecimalFormat format) {
        int resourceId;
        double value;
        if(size > 1048576) {
            resourceId = R.string.mb_size;
            value = size/(1048576.);
        } else if(size > 1024) {
            resourceId = R.string.kb_size;
            value = size/1024.;
        } else {
            resourceId = R.string.byte_size;
            value = size;
        }

        return String.format(res.getString(resourceId), format.format(value));
    }


    public static String getDuration(long duration /* s */) {

        return String.format("%d:%02d:%02d", duration / 3600,
                (duration % 3600) / 60,
                (duration % 60));

    }

    public static String getDurationMs(long durationMs /* ms */) {

        if(durationMs < 3600000) {
            long duration = durationMs/1000;
            return String.format("%02d:%02d:%03d", (duration % 3600) / 60,
                    (duration % 60),
                    durationMs % 1000);
        }

        return getDuration(durationMs/1000) + String.format(":%03d", durationMs % 1000);

    }

}
