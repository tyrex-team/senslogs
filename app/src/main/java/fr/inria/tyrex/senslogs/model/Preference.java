package fr.inria.tyrex.senslogs.model;

/**
 * A triple of: sensor, if sensor should be recorded, and sensor's settings
 */
public class Preference {

    public Sensor sensor;
    public boolean checked = false;
    public Sensor.Settings settings;

    public Preference(Sensor sensor, boolean checked) {
        this.sensor = sensor;
        this.checked = checked;
        this.settings = sensor.getDefaultSettings();
    }

    public Preference(Sensor sensor, Sensor.Settings settings) {
        this.sensor = sensor;
        this.checked = false;
        this.settings = settings;
    }

    @Override
    public String toString() {
        return "Preference{" +
                "sensor=" + sensor +
                ", checked=" + checked +
                ", settings=" + settings +
                '}';
    }
}
