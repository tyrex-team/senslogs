package fr.inria.tyrex.senslogs.model;

import java.io.Serializable;

/**
 * Reference positions allows user to confirm it's position during a record.
 * In logs files this value is associated with a timestamp and it useful to compare this position
 * with computed one particularly for indoor.
 */
public class PositionReference implements Serializable {

    public double elapsedTime;
    public double latitude;
    public double longitude;
    public Float level;


    public PositionReference(double elapsedTime, double latitude, double longitude, Float level) {
        this.elapsedTime = elapsedTime;
        this.longitude = longitude;
        this.latitude = latitude;
        this.level = level;
    }

    @Override
    public String toString() {
        return "PositionReference{" +
                "elapsedTime=" + elapsedTime +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", level=" + level +
                '}';
    }

    public Object[] toObject() {
        if(level == null)
            return new Object[] {latitude, longitude};
        return new Object[] {latitude, longitude, level};
    }
}
