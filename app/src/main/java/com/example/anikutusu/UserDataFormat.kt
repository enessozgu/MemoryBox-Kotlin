package com.example.anikutusu

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserDataFormat(var userName:String?="",var userMail:String?="",var userMailApproval:Int?=0) {
}