package com.example.anikutusu

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserDatabase(context:Context):SQLiteOpenHelper(context,"UserData",null,1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE  UserData(userMail TEXT);")
        db.execSQL("INSERT INTO UserData(userMail) VALUES ( 'null')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS UserData")
        onCreate(db)

    }
}