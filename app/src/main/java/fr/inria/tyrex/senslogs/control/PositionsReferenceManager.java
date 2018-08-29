package fr.inria.tyrex.senslogs.control;

import android.content.Context;
import android.content.res.Resources;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.FieldsWritableObject;


/**
 * Handle position references points
 */
public class PositionsReferenceManager {

    private static FieldsWritableObject mWritableObject =
            new FieldsWritableObject() {
                @Override
                public String getStorageFileName(Context context) {
                    return context.getString(R.string.file_name_reference_timestamps);
                }

                @Override
                public String getWebPage(Resources resources) {
                    return resources.getString(R.string.webpage_position_references);
                }

                @Override
                public String getFieldsDescription(Resources resources) {
                    return resources.getString(R.string.description_position_references);
                }

                @Override
                public String[] getFields(Resources resources) {
                    return resources.getStringArray(R.array.fields_position_references);
                }

                @Override
                public String getFileExtension() {
                    return "txt";
                }
            };

    /**
     * Get a WritableObject for RecorderWriter
     */
    public static FieldsWritableObject getFieldsWritableObject() {
        return mWritableObject;
    }
}
