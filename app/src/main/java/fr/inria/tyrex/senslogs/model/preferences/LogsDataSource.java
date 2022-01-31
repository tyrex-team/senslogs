package fr.inria.tyrex.senslogs.model.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.inria.tyrex.senslogs.control.SensorsManager;
import fr.inria.tyrex.senslogs.model.log.Log;
import fr.inria.tyrex.senslogs.model.sensors.Sensor;

/**
 * Interface to store data logs into a database (only the properties)
 */
public class LogsDataSource {

    private final static String PREF_FILE = "Logs";
    private final static String KEY_LOGS_LIST = "logs-list";

    private final SharedPreferences mPreferences;
    private final Gson mGson;

    private static class FileSerializer implements JsonDeserializer<File>,
            JsonSerializer<File> {

        @Override
        public File deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new File(json.getAsString());
        }

        @Override
        public JsonElement serialize(File src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getAbsolutePath());
        }
    }


    public LogsDataSource(Context context,
                          SensorsManager sensorsManager) {

        mPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        mGson = new GsonBuilder().
                registerTypeAdapter(Sensor.class,
                        new Sensor.Serializer(sensorsManager.getAvailableSensors())).
                registerTypeAdapter(File.class, new FileSerializer()).
                create();
    }


    public void addLog(Log log) {
        List<Log> logs = getLogs();
        logs.add(log);
        saveLogs(logs);
    }

    public void updateLog(Log log) {
        List<Log> logs = getLogs();
        logs.remove(log);
        logs.add(log);
        saveLogs(logs);
    }


    public void deleteLog(Log log) {
        List<Log> logs = getLogs();
        logs.remove(log);
        saveLogs(logs);
    }

    private void saveLogs(List<Log> logs) {
        Set<String> input = new HashSet<>();
        for (Log log : logs) {
            input.add(mGson.toJson(log));
        }
        mPreferences.edit().putStringSet(KEY_LOGS_LIST, input).apply();
    }

    public List<Log> getLogs() {
        List<Log> logs = new ArrayList<>();

        Set<String> preferencesStrings = new HashSet<>(
                mPreferences.getStringSet(KEY_LOGS_LIST, new HashSet<String>()));

        for (String preferenceString : preferencesStrings) {
            logs.add(mGson.fromJson(preferenceString, Log.class));
        }

        return logs;
    }

    public void removeAll() {

    }
}
