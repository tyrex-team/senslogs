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
 * Bluetooth Sensor
 * // TODO Not working, should be implemented
 * http://developer.android.com/guide/topics/connectivity/bluetooth-le.html
 */
public class BluetoothSensor extends Sensor {

    transient private static BluetoothSensor instance;

    public static BluetoothSensor getInstance() {
        if (instance == null) {
            instance = new BluetoothSensor();
        }
        return instance;
    }

    private BluetoothSensor() {
        super(TYPE_BLUETOOTH, Category.RADIO);
    }


    @Override
    public String getName() {
        return "Bluetooth signals";
    }

    @Override
    public String getStringType() {
        return null;
    }

    @Override
    public String getStorageFileName(Context context) {
        return context.getString(R.string.file_name_bluetooth);
    }

    @Override
    public String getFieldsDescription(Resources res) {
        return res.getString(R.string.description_bluetooth);
    }

    @Override
    public String[] getFields(Resources resources) {
        return resources.getStringArray(R.array.fields_bluetooth);
    }

    @Override
    public String getWebPage(Resources res) {
        return res.getString(R.string.webpage_bluetooth);
    }

    @Override
    public boolean exists(Context context) {
        return false;
//        return Build.VERSION.SDK_INT >= 21; //TODO
//        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    @Override
    public boolean checkPermission(Context context) {
        return !(Build.VERSION.SDK_INT >= 23 &&
                context.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void start(Context context, Settings settings, Log.RecordTimes recordTimes) {
        //TODO
//        BluetoothManager bluetoothManager = (BluetoothManager)
//                context.getSystemService(Context.BLUETOOTH_SERVICE);

//        BluetoothLeScanner scanner = bluetoothManager.getAdapter().getBluetoothLeScanner();

//        scanner.startScan(mScanCallback);
    }

    @Override
    public void stop(Context context) {
        //TODO
//        BluetoothManager bluetoothManager = (BluetoothManager)
//                context.getSystemService(Context.BLUETOOTH_SERVICE);

//        BluetoothLeScanner scanner = bluetoothManager.getAdapter().getBluetoothLeScanner();

//        scanner.stopScan(mScanCallback);
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

//    transient private ScanCallback mScanCallback = new ScanCallback() {
//        @Override
//        public void onScanResult(int callbackType, final ScanResult result) {
//            long nanoTime = System.nanoTime();
//            super.onScanResult(callbackType, result);
//
//            if (mListener == null) {
//                return;
//            }
//
//            mListener.onNewValues(nanoTime, new Object[]{ result.getDevice().getAddress(),
//                    result.getRssi(), result.getScanRecord().getBytes()});
//        }
//
//        @Override
//        public void onBatchScanResults(List<ScanResult> results) {
//            super.onBatchScanResults(results);
//        }
//
//        @Override
//        public void onScanFailed(int errorCode) {
//            super.onScanFailed(errorCode);
//        }
//    };


}
