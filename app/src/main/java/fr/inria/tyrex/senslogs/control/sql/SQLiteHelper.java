package fr.inria.tyrex.senslogs.control.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Database for Logs.
 */
public class SQLiteHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "senslogs.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_LOGS = "logs";
    public static final String COLUMN_LOG_NAME = "log_name";
    public static final String COLUMN_LOG_FILE_PATH = "log_file_path";
    public static final String COLUMN_LOG_COMPRESSED = "log_compressed";
    public static final String COLUMN_LOG_UNCOMPRESSED = "log_uncompressed";
    public static final String COLUMN_LOG_START = "log_start";
    public static final String COLUMN_LOG_END = "log_end";

    public static final String TABLE_LOGS_SENSORS = "logs_sensors";
    public static final String COLUMN_SENSOR_NAME = "sensor_name";


    private static final String CREATE_TABLE_LOGS = "create table "
            + TABLE_LOGS + "(" +
            COLUMN_LOG_NAME + " text not null, " +
            COLUMN_LOG_FILE_PATH + " text not null primary key, " +
            COLUMN_LOG_COMPRESSED + " integer, " +
            COLUMN_LOG_UNCOMPRESSED + " integer, " +
            COLUMN_LOG_START + " integer, " +
            COLUMN_LOG_END + " integer " +
            ");";


    private static final String CREATE_TABLE_LOGS_SENSORS = "create table "
            + TABLE_LOGS_SENSORS + "(" +
            COLUMN_LOG_FILE_PATH + " text not null, " +
            COLUMN_SENSOR_NAME + " text not null" +
            ");";

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_TABLE_LOGS);
        database.execSQL(CREATE_TABLE_LOGS_SENSORS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(SQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + CREATE_TABLE_LOGS_SENSORS);
        onCreate(db);
    }

}
