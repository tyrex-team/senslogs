package fr.inria.tyrex.senslogs.model.sensors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Build;

import fr.inria.tyrex.senslogs.R;

import static fr.inria.tyrex.senslogs.model.Sensor.TYPE_LOCATION_GPS;

/**
 * GPS Sensor provides a computed value from NMEA data
 * http://developer.android.com/guide/topics/location/strategies.html
 */
public class LocationGpsSensor extends LocationSensor {

    transient private static LocationGpsSensor instance;
    public static LocationGpsSensor getInstance() {
        if (instance == null) {
            instance = new LocationGpsSensor();
        }
        return instance;
    }

    private LocationGpsSensor() {
        super(TYPE_LOCATION_GPS);
    }

    @Override
    public String getName() {
        return "GPS Location";
    }

    @Override
    public String getStorageFileName(Context context) {
        return context.getString(R.string.file_name_location_gps);
    }

    @Override
    public String getFieldsDescription(Resources res) {
        return res.getString(R.string.description_location_gps);
    }

    @Override
    protected String getLocationProvider() {
        return LocationManager.GPS_PROVIDER;
    }

    @Override
    public boolean checkPermission(Context context) {
        return !(Build.VERSION.SDK_INT >= 23 &&
                context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED);
    }

}
