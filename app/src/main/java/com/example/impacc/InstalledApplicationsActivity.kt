package com.example.impacc

import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
        val installedApplications =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }

        return installedApplications
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
    var searchVisible by remember {
        mutableStateOf(false)
    }

    var searchText by remember {
        mutableStateOf("")
    }

    val filteredApplications = remember(
        applications,
        searchText
    ) {
        val normalizedQuery = searchText.trim()

        if (normalizedQuery.isBlank()) {
            applications
        } else {
            applications.filter { application ->
                application.name.contains(
                    normalizedQuery,
                    ignoreCase = true
                ) || application.packageName.contains(
                    normalizedQuery,
                    ignoreCase = true
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select application",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (searchVisible) {
            ApplicationSearchField(
                value = searchText,
                onValueChanged = { newValue ->
                    searchText = newValue
                },
                onClose = {
                    searchText = ""
                    searchVisible = false
                }
            )
        } else {
            Button(
                onClick = {
                    searchVisible = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 8.dp
                    )
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonColor,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Search application",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (filteredApplications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No applications found",
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
                    items = filteredApplications,
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
private fun ApplicationSearchField(
    value: String,
    onValueChanged: (String) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember {
        FocusRequester()
    }

    val keyboardController =
        LocalSoftwareKeyboardController.current

    val focusManager =
        LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    text = "Enter application name",
                    color = Color.Gray
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )
        )

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Button(
            onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onClose()
            },
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF424242),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Close",
                fontSize = 14.sp
            )
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
                fontSize = 12.sp
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

private val ButtonColor =
    Color(0xFF3949AB)