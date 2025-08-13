package com.example.anikutusu

class UserDatabaseFunc {

    fun getData(vt: UserDatabase): ArrayList<UserDatabaseDataClass> {
        val userList = ArrayList<UserDatabaseDataClass>()
        val db = vt.readableDatabase
        val c = db.rawQuery("SELECT * FROM UserData", null)

        while (c.moveToNext()) {
            val userMail = c.getString(c.getColumnIndexOrThrow("userMail"))
            val user = UserDatabaseDataClass(userMail)
            userList.add(user)
        }

        c.close()
        db.close()
        return userList
    }

    fun updateData(vt: UserDatabase, userMail: String) {
        val db = vt.writableDatabase
        // userName 'null' olan satırın sadece mailini günceller
        db.execSQL("UPDATE UserData SET userMail = '$userMail' ")
        db.close()
    }
}
