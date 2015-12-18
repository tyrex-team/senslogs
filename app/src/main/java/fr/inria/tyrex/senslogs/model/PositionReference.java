package fr.inria.tyrex.senslogs.model;

import java.io.Serializable;

/**
 * Reference positions allows user to confirm it's position during a record.
 * In logs files this value is associated with a timestamp and it useful to compare this position
 * with computed one particularly for indoor.
 */
public class PositionReference implements Serializable {

    public int id;
    public String name;

    public long latitude;
    public long longitude;
    public float floor;
    public float altitude;

    public PositionReference(int id, String name) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String toString() {
        return name;
    }

    public Object[] toObject() {
        return new Object[]{id, "\"" + name + "\""};
    }
}
