package fr.inria.tyrex.senslogs.model.sensors;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.Log;
import fr.inria.tyrex.senslogs.model.Sensor;

/**
 * Wifi Sensor provides wifi signals strength, ssid, bssid... on each channels
 * http://developer.android.com/reference/android/net/wifi/WifiManager.html
 */
public class WifiSensor extends Sensor {

    transient private WifiScanReceiver mWifiScanReceiver = null;

    transient private static WifiSensor instance;
    transient private double mStartTime;
    transient private double mStartTimeMinusBoot;

    transient private long mTimeStartScan;

    public static WifiSensor getInstance() {
        if (instance == null) {
            instance = new WifiSensor();
        }
        return instance;
    }

    private WifiSensor() {
        super(TYPE_WIFI, Category.RADIO);
    }

    @Override
    public String getName() {
        return "Wifi signals";
    }

    @Override
    public String getStringType() {
        return null;
    }

    @Override
    public String getStorageFileName(Context context) {
        return context.getString(R.string.file_name_wifi);
    }

    @Override
    public String getFieldsDescription(Resources res) {
        return res.getString(R.string.description_wifi);
    }

    @Override
    public String[] getFields(Resources resources) {
        return resources.getStringArray(R.array.fields_wifi);
    }

    @Override
    public String getWebPage(Resources res) {
        return res.getString(R.string.webpage_wifi);
    }

    @Override
    public boolean exists(Context context) {

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager != null;
    }

    @Override
    public boolean checkPermission(Context context) {
        return !(Build.VERSION.SDK_INT >= 23 &&
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void start(Context context, Settings settings, Log.RecordTimes recordTimes) {

        if (!checkPermission(context)) {
            return;
        }

        mWifiScanReceiver = new WifiScanReceiver();
        context.registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));


        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
        mTimeStartScan = System.currentTimeMillis();

        mStartTime = recordTimes.startTime;
        mStartTimeMinusBoot = recordTimes.startTime - recordTimes.bootTime;

        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable runnableRescan = new Runnable() {
            public void run() {
                handler.postDelayed(this, 1000);

                if (System.currentTimeMillis() - mTimeStartScan > 6000) {
                    wifiManager.startScan();
                }
            }
        };
        handler.post(runnableRescan);
    }

    @Override
    public void stop(Context context) {

        if (!checkPermission(context)) {
            return;
        }

        context.unregisterReceiver(mWifiScanReceiver);
        mWifiScanReceiver = null;
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

    private class WifiScanReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {
            double systemTimestamp = System.currentTimeMillis() / 1e3d - mStartTime;

            final WifiManager wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
            if (mWifiScanReceiver != null) {
                wifiManager.startScan();
                mTimeStartScan = System.currentTimeMillis();
            }

            if (mListener == null) {
                return;
            }


            for (ScanResult scan : wifiManager.getScanResults()) {

                double diffTime;
                if (Build.VERSION.SDK_INT >= 17) {
                    diffTime = scan.timestamp / 1e6d - mStartTimeMinusBoot;
                } else {
                    diffTime = systemTimestamp;
                }

                mListener.onNewValues(systemTimestamp, diffTime,
                        new Object[]{scan.BSSID, "\"" + scan.SSID + "\"",
                                scan.frequency, scan.level, scan.capabilities});
            }
        }
    }
}
