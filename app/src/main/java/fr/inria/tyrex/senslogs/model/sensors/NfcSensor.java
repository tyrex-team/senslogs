package fr.inria.tyrex.senslogs.model.sensors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.Log;
import fr.inria.tyrex.senslogs.model.Sensor;

/**
 * Nfc Sensor records nfc messages
 * // TODO Not working, should be implemented
 * http://developer.android.com/guide/topics/connectivity/nfc/index.html
 */
public class NfcSensor extends Sensor {

    transient private static NfcSensor instance;

    public static NfcSensor getInstance() {
        if (instance == null) {
            instance = new NfcSensor();
        }
        return instance;
    }

    private NfcSensor() {
        super(TYPE_NFC, Category.RADIO);
    }


    @Override
    public String getName() {
        return "NFC";
    }

    @Override
    public String getStorageFileName(Context context) {
        return context.getString(R.string.file_name_nfc);
    }

    @Override
    public String getFieldsDescription(Resources res) {
        return res.getString(R.string.description_nfc);
    }

    @Override
    public String[] getFields(Resources resources) {
        return resources.getStringArray(R.array.fields_nfc);
    }

    @Override
    public String getWebPage(Resources res) {
        return res.getString(R.string.webpage_nfc);
    }

    @Override
    public String getStringType() {
        return null;
    }

    @Override
    public boolean exists(Context context) {
        return false;
//        NfcManager manager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
//        NfcAdapter adapter = manager.getDefaultAdapter();
//        return adapter != null && adapter.isEnabled();
    }

    @Override
    public boolean checkPermission(Context context) {
        return !(Build.VERSION.SDK_INT >= 23 &&
                context.checkSelfPermission(Manifest.permission.NFC) != PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void start(Context context, Settings settings, Log.RecordTimes recordTimes) {
        // TODO
    }

    @Override
    public void stop(Context context) {
        // TODO
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

}
