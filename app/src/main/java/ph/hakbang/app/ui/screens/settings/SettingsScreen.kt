package ph.hakbang.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ph.hakbang.app.data.preferences.AppLanguage
import ph.hakbang.app.ui.AppViewModel
import ph.hakbang.app.util.LocalAppStrings
import ph.hakbang.app.util.StringKey

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    val intent = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}")
    )
    context.startActivity(intent)
}

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val profile by viewModel.userProfile.collectAsState()
    val strings = LocalAppStrings.current

    var goalText by remember(profile.dailyGoal) { mutableStateOf(profile.dailyGoal.toString()) }
    var weightText by remember(profile.weightKg) { mutableStateOf(profile.weightKg.toString()) }
    var strideText by remember(profile.strideMeters) { mutableStateOf(profile.strideMeters.toString()) }
    var showResetDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by remember { mutableIntStateOf(0) }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val batteryOptimizationDisabled = remember(resumeTick) { isIgnoringBatteryOptimizations(context) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        SettingsCard(title = strings[StringKey.DAILY_GOAL]) {
            OutlinedTextField(
                value = goalText,
                onValueChange = { goalText = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SettingsCard(title = strings[StringKey.WEIGHT]) {
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SettingsCard(title = strings[StringKey.STRIDE]) {
            OutlinedTextField(
                value = strideText,
                onValueChange = { strideText = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = {
                goalText.toIntOrNull()?.let { viewModel.setDailyGoal(it) }
                weightText.toDoubleOrNull()?.let { viewModel.setWeightKg(it) }
                strideText.toDoubleOrNull()?.let { viewModel.setStrideMeters(it) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(strings[StringKey.SAVE])
        }

        SettingsCard(title = strings[StringKey.LANGUAGE]) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("English")
                Switch(
                    checked = profile.language == AppLanguage.TAGLISH,
                    onCheckedChange = {
                        viewModel.setLanguage(if (it) AppLanguage.TAGLISH else AppLanguage.ENGLISH)
                    }
                )
                Text("Taglish")
            }
        }

        SettingsCard(title = strings[StringKey.BATTERY_OPTIMIZATION_TITLE]) {
            Text(
                text = if (batteryOptimizationDisabled) {
                    strings[StringKey.BATTERY_OPTIMIZATION_DESC_ON]
                } else {
                    strings[StringKey.BATTERY_OPTIMIZATION_DESC_OFF]
                }
            )
            if (!batteryOptimizationDisabled) {
                Button(
                    onClick = { requestIgnoreBatteryOptimizations(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(strings[StringKey.BATTERY_OPTIMIZATION_BUTTON])
                }
            }
        }

        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text(strings[StringKey.RESET_DATA], color = MaterialTheme.colorScheme.error)
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(strings[StringKey.RESET_DATA_TITLE]) },
            text = { Text(strings[StringKey.RESET_DATA_MESSAGE]) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAllData()
                    showResetDialog = false
                }) {
                    Text(strings[StringKey.CONFIRM])
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(strings[StringKey.CANCEL])
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            Column(modifier = Modifier.padding(top = 8.dp)) {
                content()
            }
        }
    }
}
