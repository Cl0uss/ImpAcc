package com.example.impacc

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.impacc.data.ScreenApplication
import com.example.impacc.data.ScreenApplicationStorage
import com.example.impacc.ui.theme.ImpAccTheme
import kotlin.math.roundToInt

class ApplicationSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(
            EXTRA_PACKAGE_NAME
        )

        if (packageName.isNullOrBlank()) {
            finish()
            return
        }

        val application = ScreenApplicationStorage.getApplication(
            context = this,
            packageName = packageName
        )

        if (application == null) {
            finish()
            return
        }

        val applicationIcon = try {
            packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            applicationInfo.loadIcon(packageManager)
        }

        setContent {
            ImpAccTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundColor
                ) {
                    ApplicationSettingsScreen(
                        application = application,
                        applicationIcon = applicationIcon,
                        onSave = { updatedApplication ->
                            ScreenApplicationStorage.updateApplication(
                                context = this,
                                application = updatedApplication
                            )

                            setResult(RESULT_OK)
                            finish()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME =
            "application_settings_package_name"
    }
}

@Composable
private fun ApplicationSettingsScreen(
    application: ScreenApplication,
    applicationIcon: Drawable,
    onSave: (ScreenApplication) -> Unit
) {
    var upperPartText by remember {
        mutableStateOf(application.upperPart.toString())
    }

    var lowerPartText by remember {
        mutableStateOf(application.lowerPart.toString())
    }

    var leftPartText by remember {
        mutableStateOf(application.leftPart.toString())
    }

    var rightPartText by remember {
        mutableStateOf(application.rightPart.toString())
    }

    var transparency by remember {
        mutableFloatStateOf(
            application.transparency.toFloat()
        )
    }

    val iconPainter = rememberDrawablePainter(
        drawable = applicationIcon
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Application settings",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 24.dp,
                    vertical = 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = iconPainter,
                contentDescription = application.name,
                modifier = Modifier.size(80.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = application.name,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = application.packageName,
                color = Color.Gray,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(40.dp)
            )

            PixelSettingRow(
                label = "Upper part",
                value = upperPartText,
                onValueChanged = { newValue ->
                    upperPartText = sanitizePixelValue(newValue)
                }
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            PixelSettingRow(
                label = "Lower part",
                value = lowerPartText,
                onValueChanged = { newValue ->
                    lowerPartText = sanitizePixelValue(newValue)
                }
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            PixelSettingRow(
                label = "Left part",
                value = leftPartText,
                onValueChanged = { newValue ->
                    leftPartText = sanitizePixelValue(newValue)
                }
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            PixelSettingRow(
                label = "Right part",
                value = rightPartText,
                onValueChanged = { newValue ->
                    rightPartText = sanitizePixelValue(newValue)
                }
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            TransparencySetting(
                transparency = transparency.roundToInt(),
                onTransparencyChanged = { newValue ->
                    transparency = newValue
                }
            )

            Spacer(
                modifier = Modifier.height(40.dp)
            )

            Button(
                onClick = {
                    onSave(
                        application.copy(
                            upperPart = upperPartText
                                .toIntOrNull()
                                ?.coerceAtLeast(0)
                                ?: 0,
                            lowerPart = lowerPartText
                                .toIntOrNull()
                                ?.coerceAtLeast(0)
                                ?: 0,
                            leftPart = leftPartText
                                .toIntOrNull()
                                ?.coerceAtLeast(0)
                                ?: 0,
                            rightPart = rightPartText
                                .toIntOrNull()
                                ?.coerceAtLeast(0)
                                ?: 0,
                            transparency = transparency
                                .roundToInt()
                                .coerceIn(0, 100)
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonColor,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Save",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun PixelSettingRow(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier.weight(1f),
            singleLine = true,
            suffix = {
                Text(
                    text = "px",
                    color = Color.Gray
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
    }
}

@Composable
private fun TransparencySetting(
    transparency: Int,
    onTransparencyChanged: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transparency",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "$transparency%",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Slider(
            value = transparency.toFloat(),
            onValueChange = onTransparencyChanged,
            valueRange = 0f..100f,
            steps = 99,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = ButtonColor,
                inactiveTrackColor = Color.DarkGray
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0% black",
                color = Color.Gray,
                fontSize = 13.sp
            )

            Text(
                text = "100% transparent",
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

private fun sanitizePixelValue(
    value: String
): String {
    return value
        .filter { character ->
            character.isDigit()
        }
        .take(MAX_PIXEL_INPUT_LENGTH)
}

private const val MAX_PIXEL_INPUT_LENGTH = 5

private val BackgroundColor =
    Color(0xFF121212)

private val ButtonColor =
    Color(0xFF3949AB)