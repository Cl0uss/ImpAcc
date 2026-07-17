package com.example.impacc

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.impacc.ui.theme.ImpAccTheme

data class InstalledApp(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

class InstalledApplicationsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val applications = getInstalledApplications()

        setContent {
            ImpAccTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundColor
                ) {
                    InstalledApplicationsScreen(
                        applications = applications,
                        onApplicationSelected = { application ->
                            val resultIntent = Intent().apply {
                                putExtra(
                                    EXTRA_APP_NAME,
                                    application.name
                                )
                                putExtra(
                                    EXTRA_PACKAGE_NAME,
                                    application.packageName
                                )
                            }

                            setResult(
                                RESULT_OK,
                                resultIntent
                            )

                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun getInstalledApplications(): List<InstalledApp> {
        val applicationInfoList =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }

        return applicationInfoList
            .asSequence()
            .filter { applicationInfo ->
                applicationInfo.packageName != packageName
            }
            .filter { applicationInfo ->
                packageManager.getLaunchIntentForPackage(
                    applicationInfo.packageName
                ) != null
            }
            .map { applicationInfo ->
                InstalledApp(
                    name = applicationInfo.loadLabel(
                        packageManager
                    ).toString(),
                    packageName = applicationInfo.packageName,
                    icon = applicationInfo.loadIcon(
                        packageManager
                    )
                )
            }
            .sortedBy { application ->
                application.name.lowercase()
            }
            .toList()
    }

    companion object {
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_PACKAGE_NAME = "package_name"
    }
}

@Composable
private fun InstalledApplicationsScreen(
    applications: List<InstalledApp>,
    onApplicationSelected: (InstalledApp) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select application",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (applications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No applications found",
                    color = Color.LightGray,
                    fontSize = 18.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = applications,
                    key = { application ->
                        application.packageName
                    }
                ) { application ->
                    InstalledApplicationRow(
                        application = application,
                        onClick = {
                            onApplicationSelected(application)
                        }
                    )

                    HorizontalDivider(
                        color = Color(0xFF303030)
                    )
                }
            }
        }
    }
}

@Composable
private fun InstalledApplicationRow(
    application: InstalledApp,
    onClick: () -> Unit
) {
    val iconPainter = rememberDrawablePainter(
        drawable = application.icon
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                text = application.packageName,
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
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

private val BackgroundColor =
    Color(0xFF121212)