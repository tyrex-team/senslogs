package fr.inria.tyrex.senslogs.model.preferences;

import fr.inria.tyrex.senslogs.model.sensors.Sensor;

/**
 * A triple of preferences: sensor, if sensor should be recorded, and sensor's settings
 */
public class Preference {

    public Sensor sensor;
    public boolean selected;
    public Sensor.Settings settings;

    public Preference(Sensor sensor, boolean selected, Sensor.Settings settings) {
        this.sensor = sensor;
        this.selected = selected;
        this.settings = settings;
    }

    @Override
    public String toString() {
        return "Preference{" +
                "sensor=" + sensor +
                ", selected=" + selected +
                ", settings=" + settings +
                '}';
    }
}
