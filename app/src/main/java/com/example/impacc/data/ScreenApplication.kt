package com.example.impacc.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ScreenApplication(
    val name: String,
    val packageName: String,
    val upperPart: Int = 0,
    val lowerPart: Int = 0,
    val leftPart: Int = 0,
    val rightPart: Int = 0,
    val transparency: Int = 0
)

object ScreenApplicationStorage {

    private const val PREFERENCES_NAME =
        "screen_control_preferences"

    private const val KEY_APPLICATIONS =
        "screen_applications"

    fun getApplications(
        context: Context
    ): List<ScreenApplication> {
        val preferences = context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

        val storedJson = preferences.getString(
            KEY_APPLICATIONS,
            null
        ) ?: return emptyList()

        return try {
            val jsonArray = JSONArray(storedJson)
            val applications = mutableListOf<ScreenApplication>()

            for (index in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(index)

                applications.add(
                    ScreenApplication(
                        name = jsonObject.optString("name"),
                        packageName = jsonObject.optString(
                            "packageName"
                        ),
                        upperPart = jsonObject.optInt(
                            "upperPart",
                            0
                        ),
                        lowerPart = jsonObject.optInt(
                            "lowerPart",
                            0
                        ),
                        leftPart = jsonObject.optInt(
                            "leftPart",
                            0
                        ),
                        rightPart = jsonObject.optInt(
                            "rightPart",
                            0
                        ),
                        transparency = jsonObject.optInt(
                            "transparency",
                            0
                        ).coerceIn(0, 100)
                    )
                )
            }

            applications
                .filter { application ->
                    application.name.isNotBlank() &&
                            application.packageName.isNotBlank()
                }
                .sortedBy { application ->
                    application.name.lowercase()
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getApplication(
        context: Context,
        packageName: String
    ): ScreenApplication? {
        return getApplications(context).firstOrNull { application ->
            application.packageName == packageName
        }
    }

    fun addApplication(
        context: Context,
        name: String,
        packageName: String
    ) {
        val applications = getApplications(context).toMutableList()

        val alreadyExists = applications.any { application ->
            application.packageName == packageName
        }

        if (alreadyExists) {
            return
        }

        applications.add(
            ScreenApplication(
                name = name,
                packageName = packageName
            )
        )

        saveApplications(
            context = context,
            applications = applications
        )
    }

    fun updateApplication(
        context: Context,
        application: ScreenApplication
    ) {
        val applications = getApplications(context).toMutableList()

        val applicationIndex = applications.indexOfFirst { storedApplication ->
            storedApplication.packageName == application.packageName
        }

        if (applicationIndex >= 0) {
            applications[applicationIndex] = application
        } else {
            applications.add(application)
        }

        saveApplications(
            context = context,
            applications = applications
        )
    }

    fun removeApplication(
        context: Context,
        packageName: String
    ) {
        val applications = getApplications(context)
            .filterNot { application ->
                application.packageName == packageName
            }

        saveApplications(
            context = context,
            applications = applications
        )
    }

    private fun saveApplications(
        context: Context,
        applications: List<ScreenApplication>
    ) {
        val jsonArray = JSONArray()

        applications.forEach { application ->
            val jsonObject = JSONObject().apply {
                put("name", application.name)
                put("packageName", application.packageName)
                put(
                    "upperPart",
                    application.upperPart.coerceAtLeast(0)
                )
                put(
                    "lowerPart",
                    application.lowerPart.coerceAtLeast(0)
                )
                put(
                    "leftPart",
                    application.leftPart.coerceAtLeast(0)
                )
                put(
                    "rightPart",
                    application.rightPart.coerceAtLeast(0)
                )
                put(
                    "transparency",
                    application.transparency.coerceIn(0, 100)
                )
            }

            jsonArray.put(jsonObject)
        }

        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putString(
                KEY_APPLICATIONS,
                jsonArray.toString()
            )
            .apply()
    }
}