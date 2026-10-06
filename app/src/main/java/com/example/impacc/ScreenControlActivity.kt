package com.example.impacc

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.impacc.data.ScreenApplication
import com.example.impacc.data.ScreenApplicationStorage
import com.example.impacc.service.ScreenControlAccessibilityService
import com.example.impacc.ui.theme.ImpAccTheme

class ScreenControlActivity : ComponentActivity() {

    private val accessibilityEnabledState =
        mutableStateOf(false)

    private val usageAccessEnabledState =
        mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ImpAccTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundColor
                ) {
                    ScreenControlScreen(
                        accessibilityEnabled =
                            accessibilityEnabledState.value,
                        usageAccessEnabled =
                            usageAccessEnabledState.value
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        accessibilityEnabledState.value =
            isScreenControlAccessibilityEnabled(this)

        usageAccessEnabledState.value =
            hasUsageStatsAccess(this)
    }
}

@Composable
fun ScreenControlScreen(
    accessibilityEnabled: Boolean,
    usageAccessEnabled: Boolean
) {
    val context = LocalContext.current

    val selectedApplications = remember {
        mutableStateListOf<ScreenApplication>().apply {
            addAll(
                ScreenApplicationStorage.getApplications(
                    context
                )
            )
        }
    }

    val showAccessibilityDialog = remember {
        mutableStateOf(false)
    }

    val showUsageAccessDialog = remember {
        mutableStateOf(false)
    }

    fun refreshApplications() {
        selectedApplications.clear()

        selectedApplications.addAll(
            ScreenApplicationStorage.getApplications(
                context
            )
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshApplications()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun requestRequiredPermission(): Boolean {
        if (!accessibilityEnabled) {
            showAccessibilityDialog.value = true
            return false
        }

        if (!usageAccessEnabled) {
            showUsageAccessDialog.value = true
            return false
        }

        return true
    }

    val applicationSettingsLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) {
            refreshApplications()
        }

    val applicationPickerLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                return@rememberLauncherForActivityResult
            }

            val resultIntent =
                result.data
                    ?: return@rememberLauncherForActivityResult

            val applicationName =
                resultIntent.getStringExtra(
                    InstalledApplicationsActivity.EXTRA_APP_NAME
                ) ?: return@rememberLauncherForActivityResult

            val packageName =
                resultIntent.getStringExtra(
                    InstalledApplicationsActivity.EXTRA_PACKAGE_NAME
                ) ?: return@rememberLauncherForActivityResult

            val settingsIntent = Intent(
                context,
                ApplicationSettingsActivity::class.java
            ).apply {
                putExtra(
                    ApplicationSettingsActivity.EXTRA_PACKAGE_NAME,
                    packageName
                )
                putExtra(InstalledApplicationsActivity.EXTRA_APP_NAME, applicationName)
            }

            applicationSettingsLauncher.launch(
                settingsIntent
            )
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Screen Control",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }

        ScreenControlStatus(
            accessibilityEnabled = accessibilityEnabled,
            usageAccessEnabled = usageAccessEnabled,
            onEnableAccessibility = {
                showAccessibilityDialog.value = true
            },
            onEnableUsageAccess = {
                showUsageAccessDialog.value = true
            }
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = {
                if (!requestRequiredPermission()) {
                    return@Button
                }

                applicationPickerLauncher.launch(
                    Intent(
                        context,
                        InstalledApplicationsActivity::class.java
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(60.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonColor,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Add application",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        if (selectedApplications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No applications added",
                    color = Color.Gray,
                    fontSize = 17.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(
                    items = selectedApplications,
                    key = { application ->
                        application.packageName
                    }
                ) { application ->
                    ScreenApplicationRow(
                        application = application,
                        onOpenSettings = {
                            if (!requestRequiredPermission()) {
                                return@ScreenApplicationRow
                            }

                            val settingsIntent = Intent(
                                context,
                                ApplicationSettingsActivity::class.java
                            ).apply {
                                putExtra(
                                    ApplicationSettingsActivity.EXTRA_PACKAGE_NAME,
                                    application.packageName
                                )
                            }

                            applicationSettingsLauncher.launch(
                                settingsIntent
                            )
                        },
                        onRemove = {
                            ScreenApplicationStorage.removeApplication(
                                context = context,
                                packageName = application.packageName
                            )

                            refreshApplications()
                        }
                    )

                    HorizontalDivider(
                        color = Color(0xFF303030)
                    )
                }
            }
        }
    }

    if (showAccessibilityDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showAccessibilityDialog.value = false
            },
            title = {
                Text(
                    text = "Accessibility permission required"
                )
            },
            text = {
                Text(
                    text = "Open Accessibility settings, select ImpAcc Screen Control and enable it. Then return to ImpAcc."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAccessibilityDialog.value = false

                        try {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_ACCESSIBILITY_SETTINGS
                                )
                            )
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS)
                            )
                        }
                    }
                ) {
                    Text(text = "Open settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAccessibilityDialog.value = false
                    }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (showUsageAccessDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showUsageAccessDialog.value = false
            },
            title = {
                Text(
                    text = "Usage access required"
                )
            },
            text = {
                Text(
                    text = "ImpAcc needs usage access to determine when the selected application is really open. Open the settings, find ImpAcc and enable usage access."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUsageAccessDialog.value = false

                        try {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_USAGE_ACCESS_SETTINGS
                                )
                            )
                        } catch (_: Exception) {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS)
                            )
                        }
                    }
                ) {
                    Text(text = "Open settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUsageAccessDialog.value = false
                    }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
private fun ScreenControlStatus(
    accessibilityEnabled: Boolean,
    usageAccessEnabled: Boolean,
    onEnableAccessibility: () -> Unit,
    onEnableUsageAccess: () -> Unit
) {
    val everythingEnabled =
        accessibilityEnabled && usageAccessEnabled

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 32.dp,
                vertical = 8.dp
            ),
        shape = RoundedCornerShape(4.dp),
        color = if (everythingEnabled) {
            Color(0xFF1B5E20)
        } else {
            Color(0xFF5D4037)
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (everythingEnabled) {
                    "Screen Control is active"
                } else {
                    "Additional permissions required"
                },
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            if (!accessibilityEnabled) {
                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Button(
                    onClick = onEnableAccessibility,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonColor
                    )
                ) {
                    Text(
                        text = "Enable accessibility"
                    )
                }
            }

            if (!usageAccessEnabled) {
                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Button(
                    onClick = onEnableUsageAccess,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonColor
                    )
                ) {
                    Text(
                        text = "Enable usage access"
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenApplicationRow(
    application: ScreenApplication,
    onOpenSettings: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current

    val applicationIcon = remember(
        application.packageName
    ) {
        try {
            context.packageManager.getApplicationIcon(
                application.packageName
            )
        } catch (_: Exception) {
            context.applicationInfo.loadIcon(
                context.packageManager
            )
        }
    }

    val iconPainter = rememberDrawablePainter(
        drawable = applicationIcon
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSettings)
            .padding(
                horizontal = 20.dp,
                vertical = 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = iconPainter,
            contentDescription = application.name,
            modifier = Modifier.size(48.dp)
        )

        Spacer(
            modifier = Modifier.size(16.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = application.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = buildSettingsSummary(application),
                color = Color.Gray,
                fontSize = 13.sp
            )
        }

        Text(
            text = "Remove",
            modifier = Modifier
                .clickable(onClick = onRemove)
                .padding(12.dp),
            color = Color(0xFFFF5252),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun buildSettingsSummary(
    application: ScreenApplication
): String {
    return "U ${application.upperPart}px · " +
            "D ${application.lowerPart}px · " +
            "L ${application.leftPart}px · " +
            "R ${application.rightPart}px · " +
            "${application.transparency}%"
}

@Composable
private fun rememberDrawablePainter(
    drawable: Drawable
): Painter {
    return remember(drawable) {
        BitmapPainter(
            drawable.toBitmap().asImageBitmap()
        )
    }
}

private fun isScreenControlAccessibilityEnabled(
    context: Context
): Boolean {
    val accessibilityManager =
        context.getSystemService(
            AccessibilityManager::class.java
        )

    val enabledServices =
        accessibilityManager
            .getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )

    val expectedServiceName =
        ScreenControlAccessibilityService::class.java.name

    return enabledServices.any { serviceInfo ->
        val service =
            serviceInfo.resolveInfo.serviceInfo

        service.packageName == context.packageName &&
                service.name == expectedServiceName
    }
}

private fun hasUsageStatsAccess(
    context: Context
): Boolean {
    val appOpsManager =
        context.getSystemService(
            AppOpsManager::class.java
        )

    val mode = appOpsManager.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )

    return mode == AppOpsManager.MODE_ALLOWED
}

private val BackgroundColor =
    Color(0xFF121212)

private val ButtonColor =
    Color(0xFF3949AB)