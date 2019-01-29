package com.thin.com.tek_app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
  @PrimaryKey @ColumnInfo(name = "id") val userId: Int,
  val name: String,
  val authToken: String,
  val email: String,
  val avatar: String,
  val localAvatar: String = ""
){
}