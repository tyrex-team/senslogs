package fr.inria.tyrex.senslogs.model.sensors;

import android.content.Context;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.model.FieldsWritableObject;
import fr.inria.tyrex.senslogs.model.log.Log;


/**
 * Describes a generic sensor from LocationManager
 * A special location provider for receiving locations without actually initiating a location fix.
 */
public abstract class LocationSensor extends Sensor
        implements Serializable, FieldsWritableObject {

    private final static String INI_OPTION_LATITUDE = "Latitude";
    private final static String INI_OPTION_LONGITUDE = "Longitude";
    private final static String INI_OPTION_ALTITUDE = "Altitude";
    private final static String INI_OPTION_UNIXTIME = "UnixTime";
    private final static String INI_OPTION_ACCURACY = "Accuracy";
    private final static String INI_OPTION_BEARING = "Bearing";

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
        return getLocationManager(context).getAllProviders().contains(getLocationProvider());
    }

    @Override
    public void start(Context context, Sensor.Settings settings, Log.RecordTimes recordTimes) {

        if (!(settings instanceof Settings)) {
            settings = getDefaultSettings();
        }

        if (!checkPermission(context)) {
            return;
        }

        Settings ls = (Settings) settings;
        getLocationManager(context).requestLocationUpdates(getLocationProvider(),
                ls.minTime, ls.minDistance, mLocationListener);

        mStartTime = recordTimes.startTime;
    }

    @Override
    public void stop(Context context) {

        if (!checkPermission(context)) {
            return;
        }

        getLocationManager(context).removeUpdates(mLocationListener);
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


    protected LocationManager getLocationManager(Context context) {
        return (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public List<Log.IniRecord> getExtraIniRecords(Context context, String sectionName, String provider) {

        if (!checkPermission(context)) return new ArrayList<>();

        Location location = getLocationManager(context).getLastKnownLocation(provider);
        if (location == null) return new ArrayList<>();

        List<Log.IniRecord> records = new ArrayList<>();
        records.add(new Log.IniRecord(sectionName, INI_OPTION_LATITUDE, location.getLatitude()));
        records.add(new Log.IniRecord(sectionName, INI_OPTION_LONGITUDE, location.getLongitude()));
        records.add(new Log.IniRecord(sectionName, INI_OPTION_ALTITUDE, location.getAltitude()));
        records.add(new Log.IniRecord(sectionName, INI_OPTION_UNIXTIME, String.format(Locale.US, "%.3f", location.getTime() / 1e3d)));
        records.add(new Log.IniRecord(sectionName, INI_OPTION_ACCURACY, location.getAccuracy()));
        records.add(new Log.IniRecord(sectionName, INI_OPTION_BEARING, location.getBearing()));
        return records;
    }
}
