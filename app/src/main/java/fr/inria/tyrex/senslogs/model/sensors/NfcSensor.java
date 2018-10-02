package fr.inria.tyrex.senslogs.model.sensors;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;

import java.io.UnsupportedEncodingException;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.FieldsWritableObject;
import fr.inria.tyrex.senslogs.model.log.Log;

/**
 * Nfc Sensor records nfc messages
 *
 * With the help of: https://code.tutsplus.com/tutorials/reading-nfc-tags-with-android--mobile-17278
 * http://developer.android.com/guide/topics/connectivity/nfc/index.html
 */
public class NfcSensor extends Sensor implements FieldsWritableObject {

    transient private static NfcSensor instance;
    transient private double mStartTime;

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
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);
        return adapter != null && adapter.isEnabled();
    }

    @Override
    public boolean checkPermission(Context context) {
        return !(Build.VERSION.SDK_INT >= 23 &&
                context.checkSelfPermission(Manifest.permission.NFC) != PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void start(Context context, Settings settings, Log.RecordTimes recordTimes) {
        mStartTime = recordTimes.startTime;
        // TODO
    }

    @Override
    public void stop(Context context) {
        // TODO
    }


    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(
                activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding {@link Activity} requesting to stop the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity) {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
        adapter.disableForegroundDispatch(activity);
    }


    @Override
    public boolean hasSettings() {
        return false;
    }

    public void handleTag(Tag tag) {

        Ndef ndef = Ndef.get(tag);

        // NDEF is not supported by this Tag.
        if (ndef == null) return;

        NdefMessage ndefMessage = ndef.getCachedNdefMessage();

        NdefRecord[] records = ndefMessage.getRecords();
        for (NdefRecord ndefRecord : records) {
            if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN) {
                try {
                    broadcastText(readText(ndefRecord));
                } catch (UnsupportedEncodingException ignored) {
                }
            }
        }


    }

    private void broadcastText(String text) {

        double systemTimestamp = System.currentTimeMillis() / 1e3d - mStartTime;
        if (mListener == null) {
            return;
        }
        mListener.onNewValues(systemTimestamp, systemTimestamp, new Object[]{text});
    }

    private String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

        byte[] payload = record.getPayload();

        // Get the Text Encoding
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

        // Get the Language Code
        int languageCodeLength = payload[0] & 0063;

        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
        // e.g. "en"

        // Get the Text
        String text = new String(payload, languageCodeLength + 1,
                payload.length - languageCodeLength - 1, textEncoding);

        return text;
    }
}
