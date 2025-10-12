package com.example.mangocam.utils

import android.content.SharedPreferences
import com.example.mangocam.model.Farm
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

object SharedPrefUtil {
    val gson = Gson()
    fun getFarms(sharedPref: SharedPreferences): MutableList<Farm> {
        val type = object : TypeToken<MutableList<Farm>>() {}.type

        val farmList: MutableList<Farm> =
            gson.fromJson(sharedPref.getString(Constant.SHARED_PREF_FARM, null), type) ?: mutableListOf()

        return farmList
    }

    fun setFarms(sharedPref: SharedPreferences, farms : MutableList<Farm>)
    {
        sharedPref.edit { putString(Constant.SHARED_PREF_FARM, gson.toJson(farms)) }
    }
}