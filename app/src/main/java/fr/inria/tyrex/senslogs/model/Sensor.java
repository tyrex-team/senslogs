package fr.inria.tyrex.senslogs.model;

import android.content.Context;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;

import fr.inria.tyrex.senslogs.R;
import fr.inria.tyrex.senslogs.control.RecorderWriter;

/**
 * This class is a factorization of all sensors of an Android system.
 * A sensor is described by its name and a category.
 * It can be started and stopped.
 */
public abstract class Sensor implements Serializable, RecorderWriter.WritableObject {

    public final static int TYPE_LOCATION_GPS = 0x300;
    public final static int TYPE_LOCATION_CELL_WIFI = 0x301;
    public final static int TYPE_LOCATION_PASSIVE = 0x302;
    public final static int TYPE_NMEA = 0x303;
    public final static int TYPE_WIFI = 0x304;
    public final static int TYPE_BLUETOOTH = 0x305;
    public final static int TYPE_NFC = 0x306;


    protected Category mCategory;
    protected int mType;
    protected transient Listener mListener;

    protected Sensor(int type, Category category) {
        mCategory = category;
        mType = type;
    }

    public abstract String getName();
    public int getType() {
        return mType;
    }

    public abstract String getStringType();

    public abstract boolean exists(Context context);

    public abstract boolean checkPermission(Context context);

    public boolean mustRunOnUiThread() {
        return false;
    }

    public abstract void start(Context context, Settings settings, Log.RecordTimes mRecordTimes);

    public abstract void stop(Context context);


    @Override
    public boolean equals(Object o) {

        if (!(o instanceof Sensor)) {
            return false;
        }

        Sensor sensor = (Sensor) o;
        return sensor.getName() != null && getName() != null &&
                getName().equals(sensor.getName());
    }


    /*
    Category
     */

    public Category getCategory() {
        return mCategory;
    }

    public enum Category {
        IMU(R.string.sensor_category_imu),
        IMU_CALIBRATED(R.string.sensor_category_imu_calibrated),
        IMU_COMPUTED(R.string.sensor_category_imu_computed),
        ENVIRONMENT(R.string.sensor_category_environment),
        RADIO(R.string.sensor_category_radio),
        RADIO_COMPUTED(R.string.sensor_category_radio_computed),
        OTHER(R.string.sensor_category_other),
        UNKNOWN(R.string.sensor_category_unknown);

        private int mResource;

        Category(int resource) {
            mResource = resource;
        }

        public String getName(Context context) {
            return context.getString(mResource);
        }

        public static int compareTo(Category category1, Category category2) {
            return category1.ordinal() - category2.ordinal();
        }
    }


    /*
    Settings
     */

    public abstract boolean hasSettings();

    public Settings getDefaultSettings() {
        return Settings.DEFAULT;
    }

    public static class Settings {
        public static Settings DEFAULT = new Settings();

        @Override
        public String toString() {
            return "Default Settings";
        }
    }



    /*
    Listener
     */
    public void setListener(Listener listener) {
        mListener = listener;
    }

    public interface Listener {
        /**
         * Called for each new value from a sensor
         * @param diffTimeSystem difference time between beginning of capture and event received
         *                       by system from sensor
         * @param diffTimeSensor difference time between begining of capture and sensor event. This
         *                       value is equal to diffTimeSystem if there is no specific timestamp
         *                       from sensor
         * @param objects data
         */
        void onNewValues(double diffTimeSystem, double diffTimeSensor, Object[] objects);
    }



    public static class Serializer implements JsonDeserializer<Sensor>,
            JsonSerializer<Sensor> {

        private final static String JSON_ATTRIBUTE_NAME = "name";

        private final List<Sensor> mSensors;

        public Serializer(List<Sensor> sensors){
            mSensors = sensors;
        }

        @Override
        public Sensor deserialize(JsonElement json, Type typeOfT,
                                  JsonDeserializationContext context)
                throws JsonParseException {

            String sensorName = json.getAsJsonObject().get(JSON_ATTRIBUTE_NAME).getAsString();
            if(sensorName == null) {
                return null;
            }

            for(Sensor sensor : mSensors) {
                if(sensorName.equals(sensor.getName())) {
                    return sensor;
                }
            }
            return null;
        }

        @Override
        public JsonElement serialize(Sensor src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject output = new JsonObject();
            output.addProperty(JSON_ATTRIBUTE_NAME, src.getName());
            return output;

        }
    }

}

