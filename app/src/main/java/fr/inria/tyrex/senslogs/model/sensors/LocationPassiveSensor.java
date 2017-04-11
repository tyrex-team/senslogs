package fr.inria.tyrex.senslogs.model.sensors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Build;

import fr.inria.tyrex.senslogs.R;


/**
 * A special location provider for receiving locations without actually initiating a location fix.
 * http://developer.android.com/guide/topics/location/strategies.html
 */
public class LocationPassiveSensor extends LocationSensor {

    transient private static LocationPassiveSensor instance;
    public static LocationPassiveSensor getInstance() {
        if(instance == null) {
            instance = new LocationPassiveSensor();
        }
        return instance;
    }

    private LocationPassiveSensor() {
        super(TYPE_LOCATION_PASSIVE);
    }

    @Override
    public String getName() {
        return "Passive Location";
    }

    @Override
    public String getStorageFileName(Context context) {
        return context.getString(R.string.file_name_location_passive);
    }

    @Override
    public String getFieldsDescription(Resources res) {
        return res.getString(R.string.description_location_passive);
    }

    @Override
    protected String getLocationProvider() {
        return LocationManager.PASSIVE_PROVIDER;
    }

    @Override
    public boolean checkPermission(Context context) {
        return !(Build.VERSION.SDK_INT >= 23 &&
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED);
    }

}
