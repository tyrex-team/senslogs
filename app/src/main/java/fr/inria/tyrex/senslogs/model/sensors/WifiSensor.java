package fr.inria.tyrex.senslogs.model.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.Sensor;

/**
 * Wifi Sensor provides wifi signals strength, ssid, bssid... on each channels
 * http://developer.android.com/reference/android/net/wifi/WifiManager.html
 */
public class WifiSensor extends Sensor {

    private final long bootTime;
    transient private WifiScanReceiver mWifiScanReceiver = null;

    transient private static WifiSensor instance;

    public static WifiSensor getInstance() {
        if (instance == null) {
            instance = new WifiSensor();
        }
        return instance;
    }

    private WifiSensor() {
        super(TYPE_WIFI, Category.RADIO);
        bootTime = java.lang.System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime();
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
    public String getDataDescription(Resources res) {
        return res.getString(R.string.description_wifi);
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
                context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void start(Context context, Settings settings) {

        if (!checkPermission(context)) {
            return;
        }

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
        mWifiScanReceiver = new WifiScanReceiver();
        context.registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    public void stop(Context context) {
        context.unregisterReceiver(mWifiScanReceiver);
        mWifiScanReceiver = null;
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

    private class WifiScanReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {

            long timeMillis = System.currentTimeMillis();


            final WifiManager wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
            if (mWifiScanReceiver != null) {
                wifiManager.startScan();
            }

            if (mListener == null) {
                return;
            }

            for (ScanResult scan : wifiManager.getScanResults()) {

                long time;
                if (Build.VERSION.SDK_INT >= 17) {
                    // Scan timestamp is equivalent to elapsedTime in micro seconds
                    time = bootTime + scan.timestamp / 1000;
                } else {
                    time = timeMillis;
                }

                mListener.onNewValues(time,
                        new Object[]{scan.BSSID, "\"" + scan.SSID + "\"", scan.frequency,
                                scan.level, scan.capabilities});
            }
        }
    }
}
