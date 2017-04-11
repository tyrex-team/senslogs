package fr.inria.tyrex.senslogs.model.sensors;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.Log;
import fr.inria.tyrex.senslogs.model.Sensor;

/**
 * NMEA Sensor provides NMEA sentences from the GPS
 * http://developer.android.com/reference/android/location/GpsStatus.NmeaListener.html
 */
public class NmeaSensor extends Sensor {

    transient private static NmeaSensor instance;
    transient private double mStartTime;

    public static NmeaSensor getInstance() {
        if (instance == null) {
            instance = new NmeaSensor();
        }
        return instance;
    }

    private NmeaSensor() {
        super(TYPE_NMEA, Category.RADIO);
    }

    @Override
    public String getName() {
        return "NMEA data";
    }

    @Override
    public String getStringType() {
        return null;
    }

    @Override
    public String getStorageFileName(Context context) {
        return context.getString(R.string.file_name_nmea);
    }

    @Override
    public String getFieldsDescription(Resources res) {
        return res.getString(R.string.description_nmea);
    }

    @Override
    public String[] getFields(Resources resources) {
        return resources.getStringArray(R.array.fields_nmea);
    }

    @Override
    public String getWebPage(Resources res) {
        return res.getString(R.string.webpage_nmea);
    }

    @Override
    public boolean exists(Context context) {

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager != null && wifiManager.isWifiEnabled();

    }

    @Override
    public boolean checkPermission(Context context) {
        return !(Build.VERSION.SDK_INT >= 23 &&
                context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED);
    }


    @Override
    public void start(Context context, Settings settings, Log.RecordTimes recordTimes) {
        LocationManager locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);

        if (!checkPermission(context)) {
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        locationManager.addNmeaListener(mNmeaListener);

        mStartTime = recordTimes.startTime;
    }

    @Override
    public void stop(Context context) {
        LocationManager locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);

        if (!checkPermission(context)) {
            return;
        }

        locationManager.removeNmeaListener(mNmeaListener);
        locationManager.removeUpdates(mLocationListener);
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

    transient private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    transient private GpsStatus.NmeaListener mNmeaListener = new GpsStatus.NmeaListener() {
        @Override
        public void onNmeaReceived(final long timestamp, final String nmea) {
            double systemTimestamp = System.currentTimeMillis() / 1e3d - mStartTime;
            if (mListener == null) {
                return;
            }
            mListener.onNewValues(systemTimestamp, timestamp / 1e3d - mStartTime, new Object[]{nmea});
        }
    };

    @Override
    public boolean mustRunOnUiThread() {
        return true;
    }
}
