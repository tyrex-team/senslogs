package fr.inria.tyrex.senslogs.control;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.inria.tyrex.senslogs.model.Preference;
import fr.inria.tyrex.senslogs.model.Sensor;
import fr.inria.tyrex.senslogs.model.sensors.AndroidSensor;
import fr.inria.tyrex.senslogs.model.sensors.LocationSensor;

/**
 * This class handle if a sensor is checked and preferences for each sensor
 */
public class SensorsPreferences {

    private final static String PREF_FILE = "Sensors";

    private final Context mContext;
    private final Sensor.Serializer mSensorSerializer;
    private final PreferenceTypeAdapterFactory mPreferenceSerializer;
    private List<Preference> mCache;

    public SensorsPreferences(Context context, ArrayList<Sensor> availableSensorsList) {
        mContext = context;
        mCache = new ArrayList<>();
        mSensorSerializer = new Sensor.Serializer(availableSensorsList);
        mPreferenceSerializer = new PreferenceTypeAdapterFactory();
        loadPreferences();
    }

    public void setChecked(Sensor sensor, boolean checked) {

        Preference preference = getPreference(sensor);

        if (mCache.contains(preference) && preference != null) {
            preference.checked = checked;
        } else {
            mCache.add(new Preference(sensor, checked));
        }

        savePreferences();
    }

    public boolean isChecked(Sensor sensor) {
        Preference preference = getPreference(sensor);
        return preference != null && preference.checked;
    }

    public Map<Sensor, Sensor.Settings> getCheckedSensors(List<Sensor> sensorList) {

        Map<Sensor, Sensor.Settings> output = new HashMap<>();

        for (Sensor sensor : sensorList) {
            if (!isChecked(sensor)) {
                continue;
            }
            output.put(sensor, getSettings(sensor));
        }

        return output;

    }

    public void setSettings(Sensor sensor, Sensor.Settings settings) {

        Preference preference = getPreference(sensor);

        if (mCache.contains(preference) && preference != null) {
            preference.settings = settings;
        } else {
            mCache.add(new Preference(sensor, settings));
        }

        savePreferences();
    }

    public Sensor.Settings getSettings(Sensor sensor) {
        Preference preference = getPreference(sensor);
        return preference == null ? sensor.getDefaultSettings() : preference.settings;
    }

    private Preference getPreference(Sensor sensor) {
        for (Preference preference : mCache) {
            if (preference.sensor.equals(sensor)) {
                return preference;
            }
        }
        return null;
    }


    private void savePreferences() {


        String value = new GsonBuilder().
                registerTypeAdapterFactory(mPreferenceSerializer).
                registerTypeAdapter(Sensor.class, mSensorSerializer).
                create().
                toJson(mCache);
        SharedPreferences prefs = mContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        e.putString("list", value);
        e.apply();
    }

    private void clearPreferences() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        e.putString("list", null);
        e.apply();
        mCache = new ArrayList<>();
    }

    private void loadPreferences() {

        SharedPreferences prefs = mContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        String value = prefs.getString("list", null);
        if (value == null) {
            mCache = new ArrayList<>();
            return;
        }

        mCache = new ArrayList<>();

        Gson gson = new GsonBuilder().
                registerTypeAdapterFactory(mPreferenceSerializer).
                registerTypeAdapter(Sensor.class, mSensorSerializer).
                create();
        mCache = new ArrayList<>(Arrays.asList(gson.fromJson(value, Preference[].class)));
    }

    public void clearAll() {
        clearPreferences();
    }


    // https://github.com/google/gson/issues/43#issuecomment-83700348
    static class PreferenceTypeAdapterFactory implements TypeAdapterFactory {

        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!Preference.class.isAssignableFrom(type.getRawType())) {
                return null;
            }

            final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

            final TypeAdapter<Preference> preferenceTypeAdapter
                    = gson.getDelegateAdapter(this, TypeToken.get(Preference.class));

            final TypeAdapter<LocationSensor.Settings> settingsLocationAdapter
                    = gson.getDelegateAdapter(this, TypeToken.get(LocationSensor.Settings.class));
            final TypeAdapter<AndroidSensor.Settings> settingsAndroidAdapter
                    = gson.getDelegateAdapter(this, TypeToken.get(AndroidSensor.Settings.class));


            TypeAdapter<Preference> result = new TypeAdapter<Preference>() {
                @Override
                public void write(JsonWriter out, Preference value) throws IOException {
                    JsonObject object = preferenceTypeAdapter.toJsonTree(value).getAsJsonObject();
                    object.add("settings-class", new JsonPrimitive(value.settings.getClass().getName()));
                    elementAdapter.write(out, object);
                }

                @Override
                public Preference read(JsonReader in) throws IOException {
                    JsonObject object = elementAdapter.read(in).getAsJsonObject();
                    Preference preference = preferenceTypeAdapter.fromJsonTree(object);

                    Sensor.Settings settings = new Sensor.Settings();
                    JsonElement settingsClassObject = object.get("settings-class");
                    JsonElement settingsObject = object.get("settings");

                    if (settingsClassObject != null) {
                        String settingsClass = settingsClassObject.getAsString();
                        if(settingsClass.equals(LocationSensor.Settings.class.getName())) {
                            settings = settingsLocationAdapter.fromJsonTree(settingsObject);
                        }
                        else if(settingsClass.equals(AndroidSensor.Settings.class.getName())) {
                            settings = settingsAndroidAdapter.fromJsonTree(settingsObject);
                        }
                    }

                    preference.settings = settings;
                    return preference;
                }
            }.nullSafe();

            return (TypeAdapter<T>) result;
        }
    }
}
