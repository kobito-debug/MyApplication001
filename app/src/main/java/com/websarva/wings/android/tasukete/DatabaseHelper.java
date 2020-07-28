package com.websarva.wings.android.tasukete;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME="location.db";
    private static final int DATABASE_VERSION=1;
    private static final String SQL_CREATE_ENTRIES_POST=
            "create table location(" +
            "id integer primary key," +
            "user_id integer not null," +
            "laitude Double not null," +
            "longitude Double not null," +
            "date text not null," +
            "time text not null)";
    private static final String SQL_DELETE_ENTRIES="DROP TABLE IF EXISTS location";

    DatabaseHelper(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(SQL_CREATE_ENTRIES_POST);
        Log.d("debug","onCreate(SQLiteDatabase db)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,int ildVersion,int newVersion){
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion,int newVersion){
        onUpgrade(db,oldVersion,newVersion);
    }

}
