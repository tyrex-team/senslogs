package fr.inria.tyrex.senslogs.model.preferences;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.inria.tyrex.senslogs.control.SensorsManager;
import fr.inria.tyrex.senslogs.model.Preference;
import fr.inria.tyrex.senslogs.model.Sensor;
import fr.inria.tyrex.senslogs.model.sensors.AndroidSensor;
import fr.inria.tyrex.senslogs.model.sensors.LocationSensor;

/**
 * Interface to store sensors preferences into a database
 */
public class PreferencesDataSource {

    private final static String PREF_FILE = "Sensors";
    private final static String KEY_PREF_LIST = "preferences-list";
    private final static String KEY_CATEGORY_LIST = "category-list";

    private SharedPreferences mPreferences;
    private SensorsManager mSensorsManager;
    private final Gson mGson;


    public PreferencesDataSource(Context context, SensorsManager sensorsManager) {

        mPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        mSensorsManager = sensorsManager;

        mGson = new GsonBuilder().
                registerTypeAdapterFactory(new PreferenceTypeAdapterFactory()).
                registerTypeAdapter(Sensor.class, new Sensor.Serializer(sensorsManager.getAvailableSensors())).
                create();
    }


    public List<Preference> getPreferences() {
        List<Preference> preferences = new ArrayList<>();

        Set<String> preferencesStrings = new HashSet<>(
                mPreferences.getStringSet(KEY_PREF_LIST, new HashSet<String>()));

        for (String preferenceString : preferencesStrings) {
            preferences.add(mGson.fromJson(preferenceString, Preference.class));
        }

        for (Sensor sensor : mSensorsManager.getAvailableSensors()) {
            if (!isPreferencesListContainsSensor(preferences, sensor)) {
                preferences.add(new Preference(sensor, false, sensor.getDefaultSettings()));
            }
        }


        return preferences;
    }


    private boolean isPreferencesListContainsSensor(List<Preference> preferences, Sensor sensor) {
        for (Preference preference : preferences) {
            if (preference.sensor.equals(sensor)) {
                return true;
            }
        }
        return false;
    }


    public void updateSelection(Sensor sensor, boolean selected) {
        List<Preference> preferences = getPreferences();
        for(Preference pref : preferences) {
            if(pref.sensor.equals(sensor)) {
                pref.selected = selected;
                savePreferences(preferences);
                return;
            }
        }
    }


    public void updateSettings(Sensor sensor, Sensor.Settings settings) {
        List<Preference> preferences = getPreferences();
        for(Preference pref : preferences) {
            if(pref.sensor.equals(sensor)) {
                pref.settings = settings;
                savePreferences(preferences);
                return;
            }
        }
    }


    private void savePreferences(List<Preference> preferences) {
        Set<String> input = new HashSet<>();
        for(Preference pref : preferences) {
            input.add(mGson.toJson(pref));
        }
        mPreferences.edit().putStringSet(KEY_PREF_LIST, input).apply();
    }



    public void setCategoryExpanded(Sensor.Category category, Boolean expanded) {
        Map<Sensor.Category, Boolean> categories = getCategories();
        categories.put(category, expanded);
        saveCategories(categories);
    }

    public Map<Sensor.Category, Boolean> getCategories() {

        Map<Sensor.Category, Boolean> output = new HashMap<>();
        Sensor.Category[] categories = Sensor.Category.values();

        for(Sensor.Category category : categories) {
            output.put(category, true);
        }

        Set<String> preferencesStrings = new HashSet<>(
                mPreferences.getStringSet(KEY_CATEGORY_LIST, new HashSet<String>()));

        for (String categoryString : preferencesStrings) {
            CategoryExpanded categoryExpanded =
                    mGson.fromJson(categoryString, CategoryExpanded.class);
            output.put(categoryExpanded.category, categoryExpanded.expended);
        }

        return output;
    }

    public void saveCategories(Map<Sensor.Category, Boolean> categories) {

        Set<String> input = new HashSet<>();
        for(Map.Entry<Sensor.Category, Boolean> kv : categories.entrySet()) {
            input.add(mGson.toJson(new CategoryExpanded(kv.getKey(), kv.getValue())));
        }
        mPreferences.edit().putStringSet(KEY_CATEGORY_LIST, input).apply();
    }


    public void removeAll() {
        mPreferences.edit().putString(KEY_PREF_LIST, null).apply();
        mPreferences.edit().putString(KEY_CATEGORY_LIST, null).apply();
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
                        if (settingsClass.equals(LocationSensor.Settings.class.getName())) {
                            settings = settingsLocationAdapter.fromJsonTree(settingsObject);
                        } else if (settingsClass.equals(AndroidSensor.Settings.class.getName())) {
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

    private class CategoryExpanded {
        public Sensor.Category category;
        public Boolean expended;

        public CategoryExpanded(Sensor.Category category, Boolean expended) {
            this.category = category;
            this.expended = expended;
        }
    }
}
