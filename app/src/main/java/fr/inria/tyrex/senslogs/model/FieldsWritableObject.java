package fr.inria.tyrex.senslogs.model;

import android.content.res.Resources;

import fr.inria.tyrex.senslogs.control.RecorderWriter;

/**
 * Object writable by {@link RecorderWriter}
 */
public interface FieldsWritableObject extends WritableObject {

    String getFieldsDescription(Resources resources);

    String[] getFields(Resources resources);

}