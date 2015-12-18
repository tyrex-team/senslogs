package fr.inria.tyrex.senslogs.control.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import fr.inria.tyrex.senslogs.model.Log;
import fr.inria.tyrex.senslogs.model.Sensor;

/**
 * Interface to store data logs into a database (only the properties)
 */
public class LogsDataSource {

    private SQLiteDatabase database;
    private SQLiteHelper dbHelper;

    private final List<Sensor> mAvailableSensors;

    private static String[] ALL_COLUMNS_LOGS = {
            SQLiteHelper.COLUMN_LOG_NAME,
            SQLiteHelper.COLUMN_LOG_FILE_PATH,
            SQLiteHelper.COLUMN_LOG_COMPRESSED,
            SQLiteHelper.COLUMN_LOG_UNCOMPRESSED,
            SQLiteHelper.COLUMN_LOG_START,
            SQLiteHelper.COLUMN_LOG_END,
    };

    public LogsDataSource(Context context,
                          List<Sensor> availableSensors) {
        dbHelper = new SQLiteHelper(context);
        mAvailableSensors = availableSensors;
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }


    public void addLog(Log log) {

        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_LOG_NAME, log.getName());
        values.put(SQLiteHelper.COLUMN_LOG_FILE_PATH, log.getFile().getAbsolutePath());
        values.put(SQLiteHelper.COLUMN_LOG_COMPRESSED, log.getCompressedSize());
        values.put(SQLiteHelper.COLUMN_LOG_UNCOMPRESSED, log.getUncompressedSize());
        values.put(SQLiteHelper.COLUMN_LOG_START, log.getStartCapture().getTime());
        values.put(SQLiteHelper.COLUMN_LOG_END, log.getEndCapture().getTime());


        database.insert(SQLiteHelper.TABLE_LOGS, null, values);

        values.clear();
        for (Sensor sensor : log.getSensors()) {
            values.put(SQLiteHelper.COLUMN_LOG_FILE_PATH, log.getFile().getAbsolutePath());
            values.put(SQLiteHelper.COLUMN_SENSOR_NAME, sensor.getName());
        }
        database.insert(SQLiteHelper.TABLE_LOGS_SENSORS, null, values);

    }

    public void updateLog(Log log) {

        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_LOG_NAME, log.getName());
        values.put(SQLiteHelper.COLUMN_LOG_COMPRESSED, log.getCompressedSize());
        values.put(SQLiteHelper.COLUMN_LOG_UNCOMPRESSED, log.getUncompressedSize());
        values.put(SQLiteHelper.COLUMN_LOG_START, log.getStartCapture().getTime());
        values.put(SQLiteHelper.COLUMN_LOG_END, log.getEndCapture().getTime());

        database.update(SQLiteHelper.TABLE_LOGS, values,
                SQLiteHelper.COLUMN_LOG_FILE_PATH + " = \"" + log.getFile().getAbsolutePath() + "\"",
                null
        );
    }


    public void deleteLog(Log log) {

        database.delete(SQLiteHelper.TABLE_LOGS,
                SQLiteHelper.COLUMN_LOG_FILE_PATH + " = \"" + log.getFile().getAbsolutePath() + "\"",
                null);
        database.delete(SQLiteHelper.TABLE_LOGS_SENSORS,
                SQLiteHelper.COLUMN_LOG_FILE_PATH + " = \"" + log.getFile().getAbsolutePath() + "\"",
                null);
    }

    public List<Log> getAllLogs() {
        List<Log> logs = new ArrayList<>();

        Cursor cursor = database.query(SQLiteHelper.TABLE_LOGS,
                ALL_COLUMNS_LOGS, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            logs.add(cursorToLog(cursor));
            cursor.moveToNext();
        }

        cursor.close();
        return logs;
    }

    private Log cursorToLog(Cursor cursor) {

        return new Log(cursor.getString(0),
                new File(cursor.getString(1)),
                cursor.getLong(2),
                cursor.getLong(3),
                new Date(cursor.getLong(4)),
                new Date(cursor.getLong(5)),
                getSensorsOfLog(cursor.getString(1)));
    }

    private HashSet<Sensor> getSensorsOfLog(String filePath) {

        HashSet<Sensor> sensors = new HashSet<>();

        Cursor cursor = database.query(SQLiteHelper.TABLE_LOGS_SENSORS,
                new String[]{SQLiteHelper.COLUMN_SENSOR_NAME},
                SQLiteHelper.COLUMN_LOG_FILE_PATH + " = \"" + filePath + "\"",
                null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {

            Sensor sensor = getAvailableSensorByName(cursor.getString(0));
            if (sensor != null) {
                sensors.add(sensor);
            }
            cursor.moveToNext();
        }

        cursor.close();
        return sensors;
    }

    private Sensor getAvailableSensorByName(String name) {
        for (Sensor sensor : mAvailableSensors) {
            if (sensor.getName().equals(name)) {
                return sensor;
            }
        }
        return null;
    }

    public void removeAll() {
        database.delete(SQLiteHelper.TABLE_LOGS, null, null);
        database.delete(SQLiteHelper.TABLE_LOGS_SENSORS, null, null);
    }
}
