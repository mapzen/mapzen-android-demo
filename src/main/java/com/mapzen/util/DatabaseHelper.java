package com.mapzen.util;

import com.mapzen.activity.BaseActivity;
import com.mapzen.osrm.Instruction;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import java.util.UUID;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String COLUMN_PROVIDER = "provider";
    public static final String COLUMN_LAT = "lat";
    public static final String COLUMN_CORRECTED_LAT = "corrected_lat";
    public static final String COLUMN_INSTRUCTION_LAT = "instruction_lat";
    public static final String COLUMN_LNG = "lng";
    public static final String COLUMN_CORRECTED_LNG = "corrected_lng";
    public static final String COLUMN_INSTRUCTION_LNG = "instruction_lng";
    public static final String COLUMN_INSTRUCTION_BEARING = "instruction_bearing";
    public static final String COLUMN_ALT = "alt";
    public static final String COLUMN_ACC = "acc";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_SPEED = "speed";
    public static final String COLUMN_DUMP = "dump";
    public static final String DB_NAME = "locations.db";
    public static final String TABLE_LOCATIONS = "locations";
    public static final String TABLE_ROUTES = "routes";
    public static final String COLUMN_RAW = "raw";
    public static final String COLUMN_ROUTE_ID = "route_id";
    public static final String TABLE_LOG_ENTRIES = "log_entries";
    public static final String COLUMN_TAG = "tag";
    public static final String COLUMN_MSG = "msg";
    public static final String COLUMN_POSITION = "position";
    public static final String TABLE_ROUTE_GEOMETRY = "route_geometry";
    public static final String COLUMN_TABLE_ID = "_id";
    public static final String COLUMN_UPLOADED = "uploaded";
    public static final String COLUMN_READY_FOR_UPLOAD = "ready_for_upload";
    public static final int VERSION = 9;

    private final String createLocationsSql = "create table " + TABLE_LOCATIONS + " ("
            + COLUMN_TABLE_ID + " text primary key,"
            + COLUMN_PROVIDER + " text not null,"
            + COLUMN_LAT + " text not null,"
            + COLUMN_CORRECTED_LAT + " text,"
            + COLUMN_INSTRUCTION_LAT + " text,"
            + COLUMN_LNG + " text not null,"
            + COLUMN_CORRECTED_LNG + " text,"
            + COLUMN_INSTRUCTION_LNG + " text,"
            + COLUMN_INSTRUCTION_BEARING + " numberic,"
            + COLUMN_ALT + " text not null,"
            + COLUMN_ACC + " integer not null,"
            + COLUMN_TIME + " numeric not null,"
            + COLUMN_ROUTE_ID + " text not null,"
            + COLUMN_SPEED + " numeric not null,"
            + COLUMN_DUMP + " text not null)";

    private final String createRoutesSql = "create table " + TABLE_ROUTES + " ("
            + COLUMN_TABLE_ID + " text primary key,"
            + COLUMN_UPLOADED + " integer,"
            + COLUMN_READY_FOR_UPLOAD + " integer,"
            + COLUMN_MSG + " text not null,"
            + COLUMN_RAW + " text not null)";

    private final String createLogEntriesSql = "create table " + TABLE_LOG_ENTRIES + " ("
            + COLUMN_TABLE_ID + " text primary key,"
            + COLUMN_TAG + " text not null,"
            + COLUMN_MSG + " text not null)";

    private final String createRouteGeometrySql = "create table " + TABLE_ROUTE_GEOMETRY + " ("
            + COLUMN_TABLE_ID + " text primary key,"
            + COLUMN_ROUTE_ID + " text not null,"
            + COLUMN_POSITION + " integer not null,"
            + COLUMN_LAT + " text not null,"
            + COLUMN_LNG + " text not null)";

    private final String createRouteGeometryIndexSql = "CREATE UNIQUE INDEX route_lat_lng "
                + "on " + TABLE_ROUTE_GEOMETRY
                + " (" + COLUMN_ROUTE_ID + "," + COLUMN_LAT + ", " + COLUMN_LNG + ");";

    public DatabaseHelper(Context context) {
        super(context, context.getExternalFilesDir(null).getAbsolutePath() + "/" + DB_NAME,
                null, VERSION);
    }

    private void createDatabases(SQLiteDatabase db) {
        db.execSQL(createLocationsSql);
        db.execSQL(createRoutesSql);
        db.execSQL(createLogEntriesSql);
        db.execSQL(createRouteGeometrySql);
        db.execSQL(createRouteGeometryIndexSql);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createDatabases(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table " + TABLE_LOCATIONS);
        db.execSQL("drop table " + TABLE_ROUTES);
        db.execSQL("drop table " + TABLE_LOG_ENTRIES);
        db.execSQL("drop table " + TABLE_ROUTE_GEOMETRY);
        createDatabases(db);
    }

    public static ContentValues valuesForLocationCorrection(Location location,
            Location correctedLocation,
            Instruction instruction, String routeId) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROVIDER, location.getProvider());
        values.put(COLUMN_LAT, location.getLatitude());
        values.put(COLUMN_LNG, location.getLongitude());
        values.put(COLUMN_DUMP, location.toString());
        values.put(COLUMN_ALT, location.getAltitude());
        values.put(COLUMN_ACC, location.getAccuracy());
        values.put(COLUMN_TIME, System.currentTimeMillis());
        values.put(COLUMN_SPEED, location.getSpeed());
        values.put(COLUMN_CORRECTED_LAT, correctedLocation.getLatitude());
        values.put(COLUMN_CORRECTED_LNG, correctedLocation.getLongitude());
        values.put(COLUMN_INSTRUCTION_LAT, instruction.getLocation().getLatitude());
        values.put(COLUMN_INSTRUCTION_LNG, instruction.getLocation().getLongitude());
        values.put(COLUMN_INSTRUCTION_BEARING, instruction.getBearing());
        values.put(COLUMN_ROUTE_ID, routeId);
        values.put(COLUMN_TABLE_ID, UUID.randomUUID().toString());
        return values;
    }

    public static void truncateDatabase(BaseActivity activity) {
        activity.getDb().beginTransaction();
        activity.getDb().delete(TABLE_ROUTES, null, null);
        activity.getDb().delete(TABLE_LOCATIONS, null, null);
        activity.getDb().delete(TABLE_ROUTE_GEOMETRY, null, null);
        activity.getDb().delete(TABLE_LOG_ENTRIES, null, null);
        activity.getDb().setTransactionSuccessful();
        activity.getDb().endTransaction();
    }

}