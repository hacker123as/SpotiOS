package com.project.lol.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import org.json.JSONObject

/**
 * Where the music is coming out right now (Server screen > Audio output): the phone speaker, a
 * Bluetooth device, wired headphones... Changing it goes through Android's own output picker
 * (SpotifyBridge.openAudioOutput), because apps can't move media audio themselves.
 */
object AudioOutput {

    /** `{"name":"Pixel Buds Pro","kind":"bt"}`; kind is speaker, bt, wired, usb, hdmi or other. */
    fun currentJson(context: Context): String {
        val dev = current(context)
        val kind = kindOf(dev?.type ?: AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
        return JSONObject().put("name", nameOf(dev, kind)).put("kind", kind).toString()
    }

    private fun current(context: Context): AudioDeviceInfo? {
        val am = context.getSystemService(AudioManager::class.java) ?: return null
        if (Build.VERSION.SDK_INT >= 33) {
            val media = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            runCatching { am.getAudioDevicesForAttributes(media).firstOrNull() }.getOrNull()?.let { return it }
        }
        // Older Android: media goes to the newest headset-like device, else the speaker.
        val outs = runCatching { am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList() }.getOrDefault(emptyList())
        return PREFERRED.firstNotNullOfOrNull { t -> outs.firstOrNull { it.type == t } }
            ?: outs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
    }

    private val PREFERRED = listOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_HDMI
    )

    internal fun kindOf(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "speaker"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER, AudioDeviceInfo.TYPE_BLE_BROADCAST,
        AudioDeviceInfo.TYPE_HEARING_AID -> "bt"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_LINE_ANALOG -> "wired"
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_ACCESSORY -> "usb"
        AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_HDMI_EARC -> "hdmi"
        else -> "other"
    }

    private fun nameOf(dev: AudioDeviceInfo?, kind: String): String {
        val product = dev?.productName?.toString()?.trim().orEmpty()
        return when (kind) {
            "speaker" -> "Phone speaker"
            // Bluetooth devices report their own name; wired ones often just the phone model.
            "bt" -> product.ifEmpty { "Bluetooth" }
            "wired" -> "Headphones"
            "usb" -> product.takeIf { it.isNotEmpty() && !it.equals(Build.MODEL, true) } ?: "USB audio"
            "hdmi" -> "HDMI"
            else -> product.ifEmpty { "Phone speaker" }
        }
    }
}
