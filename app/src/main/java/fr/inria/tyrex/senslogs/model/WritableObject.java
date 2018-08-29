package fr.inria.tyrex.senslogs.model;

import android.content.Context;
import android.content.res.Resources;

import fr.inria.tyrex.senslogs.control.RecorderWriter;

/**
 * Object writable by {@link RecorderWriter}
 */
public interface WritableObject {
    String getStorageFileName(Context context);

    String getWebPage(Resources resources);

    String getFileExtension();
}