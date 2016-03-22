package fr.inria.tyrex.senslogs.model;

import android.location.Location;
import android.os.Build;
import android.os.SystemClock;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

/**
 * Created by thibaud on 17/03/16.
 */
public class RecordProperties {

    public String buildModel;

    public double startTime; // in seconds from unix time
    public double endTime; // in seconds from unix time
    public double bootTime; // in seconds from unix time
    public double monotonicAtStart; // in seconds

    public Location gpsLastKnownLocation;
    public Location networkLastKnownLocation;
    public Location passiveLastKnownLocation;


    public RecordProperties() {
        buildModel = Build.MODEL;
        bootTime = (System.currentTimeMillis() - SystemClock.elapsedRealtime()) / 1e3d;
    }

    public void init() {
        startTime = System.currentTimeMillis() / 1e3d;
        monotonicAtStart = System.nanoTime() / 1e9d;

        gpsLastKnownLocation = null;
        networkLastKnownLocation = null;
        passiveLastKnownLocation = null;
    }

    public Wini generateIniFile(File file) {

        Wini ini;
        try {
            if(!file.createNewFile())
                return null;
            ini = new Wini(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        ini.put("Device", "Manufacturer", Build.MANUFACTURER);
        ini.put("Device", "Model", Build.MODEL);
        ini.put("Device", "OSVersion", Build.VERSION.SDK_INT);


        ini.put("Time", "StartTime", String.format("%.3f", startTime));
        ini.put("Time", "EndTime", String.format("%.3f", endTime));
        ini.put("Time", "BootTime", String.format("%.3f", bootTime));
        ini.put("Time", "MonotonicAtStart", String.format("%.3f", monotonicAtStart));

        fillIniWithLocation(ini, "LastKnownGPSLocation", gpsLastKnownLocation);
        fillIniWithLocation(ini, "LastKnownNetworkLocation", networkLastKnownLocation);
        fillIniWithLocation(ini, "LastKnownPassiveLocation", passiveLastKnownLocation);

        return ini;
    }

    private void fillIniWithLocation(Wini ini, String sectionName, Location location) {
        if(location != null) {
            ini.put(sectionName, "Latitude", location.getLatitude());
            ini.put(sectionName, "Longitude", location.getLongitude());
            ini.put(sectionName, "Altitude", location.getAltitude());
            ini.put(sectionName, "UnixTime", String.format("%.3f", location.getTime()/1e3d));
            ini.put(sectionName, "Accuracy", location.getAccuracy());
            ini.put(sectionName, "Bearing", location.getBearing());
        }
    }
}
