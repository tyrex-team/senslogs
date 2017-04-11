package fr.inria.tyrex.senslogs.model.sensors;

import android.content.Context;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.io.Serializable;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.Log;
import fr.inria.tyrex.senslogs.model.Sensor;


/**
 * Describes a generic sensor from LocationManager
 * A special location provider for receiving locations without actually initiating a location fix.
 */
public abstract class LocationSensor extends Sensor implements Serializable {

    transient private double mStartTime;

    protected LocationSensor(int type) {
        super(type, Category.RADIO_COMPUTED);
    }

    protected abstract String getLocationProvider();

    @Override
    public String getStringType() {
        return null;
    }

    @Override
    public boolean exists(Context context) {

        LocationManager locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);

        return locationManager.getAllProviders().contains(getLocationProvider());
    }

    @Override
    public void start(Context context, Sensor.Settings settings, Log.RecordTimes recordTimes) {
        LocationManager locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);

        if (!(settings instanceof Settings)) {
            settings = getDefaultSettings();
        }

        if (!checkPermission(context)) {
            return;
        }

        Settings ls = (Settings) settings;
        locationManager.requestLocationUpdates(getLocationProvider(),
                ls.minTime, ls.minDistance, mLocationListener);
        
        mStartTime = recordTimes.startTime;
    }

    @Override
    public void stop(Context context) {
        LocationManager locationManager = (LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);

        if (!checkPermission(context)) {
            return;
        }

        locationManager.removeUpdates(mLocationListener);
    }


    @Override
    public String getWebPage(Resources res) {
        return res.getString(R.string.webpage_location);
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public Settings getDefaultSettings() {
        return Settings.DEFAULT;
    }

    transient private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            double systemTimestamp = System.currentTimeMillis() / 1e3d - mStartTime;

            if (mListener == null) {
                return;
            }

            mListener.onNewValues(systemTimestamp, location.getTime() / 1e3d - mStartTime,
                    new Object[]{location.getLatitude(), location.getLongitude(),
                    location.getAltitude(), location.getBearing(), location.getAccuracy(),
                    location.getSpeed()});
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

    public static class Settings extends Sensor.Settings {
        public long minTime;
        public float minDistance;

        public static Settings DEFAULT = new Settings(0, 0);

        public Settings(long minTime, float minDistance) {
            this.minTime = minTime;
            this.minDistance = minDistance;
        }

        @Override
        public String toString() {
            return "LocationSensor.Settings{" +
                    "minTime=" + minTime +
                    ", minDistance=" + minDistance +
                    '}';
        }
    }

    @Override
    public boolean mustRunOnUiThread() {
        return true;
    }

    @Override
    public String[] getFields(Resources resources) {
        return resources.getStringArray(R.array.fields_location);
    }
}
