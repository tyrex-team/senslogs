package fr.inria.tyrex.senslogs.control;

import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.inria.tyrex.senslogs.model.Preference;
import fr.inria.tyrex.senslogs.model.Sensor;
import fr.inria.tyrex.senslogs.model.preferences.PreferencesDataSource;

/**
 * This class handle if a sensor is selected and preferences for each sensor
 */
public class PreferencesManager {

    private final PreferencesDataSource mDataSource;
    private List<Preference> mSensorsPreferences;
    private Map<Sensor.Category, Boolean> mCacheCategories;

    public PreferencesManager(Context context, SensorsManager sensorsManager) {

        mDataSource = new PreferencesDataSource(context, sensorsManager);
        loadPreferences();
        loadCategories();
    }

    public void setChecked(Sensor sensor, boolean checked) {

        Preference preference = getPreference(sensor);

        // Should never be called
        if (preference == null) {
            return;
        }

        preference.selected = checked;
        mDataSource.updateSelection(sensor, checked);
    }

    public boolean isChecked(Sensor sensor) {
        Preference preference = getPreference(sensor);
        return preference != null && preference.selected;
    }

    public Map<Sensor, Sensor.Settings> getSelectedSensors() {

        Map<Sensor, Sensor.Settings> output = new HashMap<>();

        for (Preference pref : mSensorsPreferences) {
            if (!pref.selected) {
                continue;
            }
            output.put(pref.sensor, pref.settings);
        }

        return output;

    }

    public void setSettings(Sensor sensor, Sensor.Settings settings) {

        Preference preference = getPreference(sensor);

        // Should never be called
        if (preference == null) {
            return;
        }

        preference.settings = settings;
        mDataSource.updateSettings(sensor, settings);
    }

    public Sensor.Settings getSettings(Sensor sensor) {
        Preference preference = getPreference(sensor);
        return preference == null ? sensor.getDefaultSettings() : preference.settings;
    }

    private Preference getPreference(Sensor sensor) {
        for (Preference preference : mSensorsPreferences) {
            if (preference.sensor.equals(sensor)) {
                return preference;
            }
        }
        return null;
    }


    private void loadPreferences() {
        mSensorsPreferences = mDataSource.getPreferences();
    }


    public boolean isExpended(Sensor.Category category) {
        if (!mCacheCategories.containsKey(category)) {
            return true;
        }
        return mCacheCategories.get(category);
    }

    public void setCategoryExpended(Sensor.Category category, boolean expended) {
        mCacheCategories.put(category, expended);
        mDataSource.setCategoryExpanded(category, expended);
    }

    private void loadCategories() {
        mCacheCategories = mDataSource.getCategories();
    }


    public void clearAll() {
        mSensorsPreferences.clear();
        mDataSource.removeAll();
    }

}
