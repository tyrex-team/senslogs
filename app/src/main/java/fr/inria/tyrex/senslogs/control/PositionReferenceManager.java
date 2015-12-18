package fr.inria.tyrex.senslogs.control;

import android.content.Context;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.List;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.PositionReference;


/**
 * Handle position references points
 */
public class PositionReferenceManager {

    private static final List<PositionReference> mPositionReferences;

    static {
        // TODO Should be dynamic
        mPositionReferences = new ArrayList<>();
        mPositionReferences.add(new PositionReference(1, "Unknown Position"));
    }

    private static RecorderWriter.WritableObject mWritableObject =
            new RecorderWriter.WritableObject() {
                @Override
                public String getStorageFileName(Context context) {
                    return context.getString(R.string.file_name_reference_positions);
                }

                @Override
                public String getWebPage(Resources resources) {
                    return resources.getString(R.string.webpage_position_references);
                }

                @Override
                public String getDataDescription(Resources resources) {
                    return resources.getString(R.string.description_position_references);
                }
            };


    /**
     * Get list of available position references
     *
     * @return list of available position references
     */
    public static List<PositionReference> getPositionReferences() {
        return mPositionReferences;
    }

    /**
     * Get a WritableObject for RecorderWriter
     */
    public static RecorderWriter.WritableObject getWritableObject() {
        return mWritableObject;
    }
}
