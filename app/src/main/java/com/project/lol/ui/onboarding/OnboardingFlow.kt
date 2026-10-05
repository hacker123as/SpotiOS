package com.project.lol.ui.onboarding

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lol.R
import compose.icons.TablerIcons
import compose.icons.tablericons.Battery
import compose.icons.tablericons.Bell
import compose.icons.tablericons.Bluetooth
import compose.icons.tablericons.CircleCheck
import compose.icons.tablericons.Download
import compose.icons.tablericons.Message
import compose.icons.tablericons.ShieldCheck
import compose.icons.tablericons.Wand

private val Green = Color(0xFF1ED760)
private val Ink2 = Color.White.copy(alpha = 0.62f)
private val GlassFill = Color.White.copy(alpha = 0.07f)
private val GlassRim = Color.White.copy(alpha = 0.12f)

const val ONBOARDING_STEPS = 3
const val STEP_WELCOME = 0
const val STEP_PERMISSIONS = 1
const val STEP_BATTERY = 2

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

/** The system "Allow" dialog for this app, falling back to the battery optimization list. */
@SuppressLint("BatteryLife")
fun batteryOptimizationIntent(context: Context): Intent {
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        .setData(Uri.parse("package:${context.packageName}"))
    return if (direct.resolveActivity(context.packageManager) != null) direct
    else Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
}

/**
 * First-launch flow: welcome, permissions, then turning off battery
 * optimization so playback and downloads keep running with the screen off.
 * [onlyBattery] shows just the last step (for people who set up an older version).
 */
@Composable
fun OnboardingFlow(
    modifier: Modifier = Modifier,
    step: Int,
    onlyBattery: Boolean,
    onNext: () -> Unit,
    onRequestPermissions: () -> Unit,
    onFinish: () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color(0xFF07070A))
            .background(
                Brush.radialGradient(
                    listOf(Green.copy(alpha = 0.20f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(0f, 0f),
                    radius = 1400f
                )
            )
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF3B5BFF).copy(alpha = 0.12f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    radius = 1500f
                )
            )
            .systemBarsPadding()
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            if (!onlyBattery) {
                Spacer(Modifier.height(16.dp))
                PageDots(step)
            }
            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(320, easing = LinearOutSlowInEasing)) +
                        slideInHorizontally(tween(320, easing = LinearOutSlowInEasing)) { it / 6 }) togetherWith
                        (fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 8 })
                },
                label = "onboarding"
            ) { s ->
                when (s) {
                    STEP_WELCOME -> WelcomePage(onNext)
                    STEP_PERMISSIONS -> PermissionsPage(onRequestPermissions, onNext)
                    else -> BatteryPage(onFinish)
                }
            }
        }
    }
}

@Composable
private fun PageDots(step: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(ONBOARDING_STEPS) { i ->
            val w by animateDpAsState(if (i == step) 22.dp else 7.dp, tween(260), label = "dot")
            val c by animateColorAsState(if (i <= step) Green else Color.White.copy(alpha = 0.18f), tween(260), label = "dotc")
            Box(Modifier.padding(horizontal = 3.dp).height(7.dp).width(w).clip(CircleShape).background(c))
        }
    }
}

@Composable
private fun Page(
    hero: @Composable () -> Unit,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(28.dp))
            hero()
            Spacer(Modifier.height(22.dp))
            Text(
                title,
                color = Color.White,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.6).sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(subtitle, color = Ink2, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(26.dp))
            content()
            Spacer(Modifier.height(16.dp))
        }
        actions()
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun HeroIcon(icon: ImageVector, tint: Color = Green) {
    Box(
        Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(GlassFill)
            .border(1.dp, GlassRim, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(42.dp))
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Page(
        hero = {
            Image(painterResource(R.drawable.ic_spotios_logo), contentDescription = null, modifier = Modifier.size(104.dp))
        },
        title = stringResource(R.string.onb_welcome_title),
        subtitle = stringResource(R.string.onb_welcome_subtitle),
        content = {
            GlassGroup {
                Feature(TablerIcons.Wand, stringResource(R.string.onb_feat_glass_title), stringResource(R.string.onb_feat_glass_desc))
                Divider()
                Feature(TablerIcons.Download, stringResource(R.string.onb_feat_offline_title), stringResource(R.string.onb_feat_offline_desc))
                Divider()
                Feature(TablerIcons.Message, stringResource(R.string.onb_feat_lyrics_title), stringResource(R.string.onb_feat_lyrics_desc))
                Divider()
                Feature(TablerIcons.ShieldCheck, stringResource(R.string.onb_feat_ads_title), stringResource(R.string.onb_feat_ads_desc))
            }
        },
        actions = { PrimaryButton(stringResource(R.string.onb_get_started), onNext) }
    )
}

@Composable
private fun PermissionsPage(onAllow: () -> Unit, onSkip: () -> Unit) {
    Page(
        hero = { HeroIcon(TablerIcons.Bell) },
        title = stringResource(R.string.onb_perm_title),
        subtitle = stringResource(R.string.onb_perm_subtitle),
        content = {
            GlassGroup {
                Feature(TablerIcons.Bell, stringResource(R.string.splash_onboarding_notifications_title), stringResource(R.string.splash_onboarding_notifications_desc))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Divider()
                    Feature(TablerIcons.Bluetooth, stringResource(R.string.splash_onboarding_bluetooth_title), stringResource(R.string.splash_onboarding_bluetooth_desc))
                }
            }
        },
        actions = {
            PrimaryButton(stringResource(R.string.onb_allow), onAllow)
            SecondaryButton(stringResource(R.string.onb_not_now), onSkip)
        }
    )
}

@Composable
private fun BatteryPage(onFinish: () -> Unit) {
    val context = LocalContext.current
    var ignoring by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        ignoring = isIgnoringBatteryOptimizations(context)
    }
    Page(
        hero = { HeroIcon(if (ignoring) TablerIcons.CircleCheck else TablerIcons.Battery) },
        title = stringResource(R.string.onb_battery_title),
        subtitle = stringResource(R.string.onb_battery_subtitle),
        content = {
            GlassGroup {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(if (ignoring) Green else Color(0xFFFFB020)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(if (ignoring) R.string.onb_battery_off else R.string.onb_battery_on),
                            color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(if (ignoring) R.string.onb_battery_off_desc else R.string.onb_battery_on_desc),
                            color = Ink2, fontSize = 13.sp, lineHeight = 17.sp
                        )
                    }
                }
            }
            if (!ignoring) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.onb_battery_hint),
                    color = Color.White.copy(alpha = 0.42f), fontSize = 12.5.sp, lineHeight = 17.sp, textAlign = TextAlign.Center
                )
            }
        },
        actions = {
            if (ignoring) {
                PrimaryButton(stringResource(R.string.onb_start_listening), onFinish)
            } else {
                PrimaryButton(stringResource(R.string.onb_battery_open)) {
                    runCatching { launcher.launch(batteryOptimizationIntent(context)) }
                        .onFailure { runCatching { launcher.launch(Intent(Settings.ACTION_SETTINGS)) } }
                }
                SecondaryButton(stringResource(R.string.onb_not_now), onFinish)
            }
        }
    )
}

@Composable
private fun GlassGroup(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(GlassFill)
            .border(1.dp, GlassRim, RoundedCornerShape(22.dp))
    ) { content() }
}

@Composable
private fun Divider() {
    Box(Modifier.padding(start = 64.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
}

@Composable
private fun Feature(icon: ImageVector, title: String, desc: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Green.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = Green, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(desc, color = Ink2, fontSize = 13.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(CircleShape)
            .background(Green)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}

@Composable
private fun SecondaryButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .height(48.dp)
            .clip(CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Ink2, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
}
