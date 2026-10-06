package com.example.impacc

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.impacc.data.ScreenApplication
import com.example.impacc.data.ScreenApplicationStorage
import com.example.impacc.service.ScreenControlAccessibilityService

/** Launches the target while the accessibility service owns the editor. */
class ApplicationSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val launchIntent = target?.let { packageManager.getLaunchIntentForPackage(it) }
        val service = ScreenControlAccessibilityService.connectedInstance
        if (target == null || launchIntent == null || service == null) {
            Toast.makeText(this, "Не удалось открыть редактор. Проверьте приложение и службу ImpAcc в специальных возможностях.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val application = ScreenApplicationStorage.getApplication(this, target)
            ?: ScreenApplication(
                name = intent.getStringExtra(InstalledApplicationsActivity.EXTRA_APP_NAME) ?: target,
                packageName = target,
                upperPart = resources.displayMetrics.heightPixels / 10,
                lowerPart = resources.displayMetrics.heightPixels / 10,
                leftPart = resources.displayMetrics.widthPixels / 10,
                rightPart = resources.displayMetrics.widthPixels / 10,
                transparency = 50
            )
        service.beginEditing(application)
        try {
            startActivity(launchIntent)
            setResult(RESULT_OK)
        } catch (_: RuntimeException) {
            service.cancelEditing()
            Toast.makeText(this, "Не удалось открыть приложение", Toast.LENGTH_LONG).show()
        }
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "application_settings_package_name"
    }
}
