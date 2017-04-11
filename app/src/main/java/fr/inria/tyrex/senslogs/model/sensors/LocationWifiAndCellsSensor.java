package fr.inria.tyrex.senslogs.model.sensors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Build;

import fr.inria.tyrex.senslogs.R;


/**
 * Wifi and Cells Sensor provides a computed value from wifi and cells signals
 * http://developer.android.com/guide/topics/location/strategies.html
 */
public class LocationWifiAndCellsSensor extends LocationSensor {

    transient private static LocationWifiAndCellsSensor instance;
    public static LocationWifiAndCellsSensor getInstance() {
        if(instance == null) {
            instance = new LocationWifiAndCellsSensor();
        }
        return instance;
    }

    private LocationWifiAndCellsSensor() {
        super(TYPE_LOCATION_CELL_WIFI);
    }

    @Override
    public String getName() {
        return "Cell and Wifi Location";
    }


    @Override
    public String getStorageFileName(Context context) {
        return context.getString(R.string.file_name_location_wifi_and_cells);
    }

    @Override
    public String getFieldsDescription(Resources res) {
        return res.getString(R.string.description_location_wifi_and_cells);
    }

    @Override
    protected String getLocationProvider() {
        return LocationManager.NETWORK_PROVIDER;
    }

    @Override
    public boolean checkPermission(Context context) {
        return !(Build.VERSION.SDK_INT >= 23 &&
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED);
    }

}
