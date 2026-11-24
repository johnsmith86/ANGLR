package com.anglr.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

private val Navy = Color(0xFF071A2D)
private val Sea = Color(0xFF00AFC0)
private val Aqua = Color(0xFF65D7C5)
private val Sand = Color(0xFFF3F6F7)
private val Ink = Color(0xFF10283B)

data class Spot(val name: String, val type: String, val lat: Double, val lon: Double, val distance: Double)

private const val WORLD_TIDES_ALIAS = "anglr_worldtides_aes"
private fun worldTidesKey(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(WORLD_TIDES_ALIAS, null) as? SecretKey)?.let { return it }
    val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
    generator.init(android.security.keystore.KeyGenParameterSpec.Builder(WORLD_TIDES_ALIAS,
        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true).build())
    return generator.generateKey()
}
private fun saveWorldTidesKey(context: Context, raw: String) {
    val prefs = context.getSharedPreferences("anglr", Context.MODE_PRIVATE)
    if (raw.isBlank()) { prefs.edit().remove("worldtides_key_enc").apply(); return }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, worldTidesKey()) }
    val encrypted = cipher.doFinal(raw.toByteArray(Charsets.UTF_8))
    val packed = cipher.iv + encrypted
    prefs.edit().putString("worldtides_key_enc", Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
}
private fun loadWorldTidesKey(context: Context): String {
    return try {
        val packed = Base64.decode(context.getSharedPreferences("anglr", Context.MODE_PRIVATE).getString("worldtides_key_enc", "") ?: "", Base64.NO_WRAP)
        if (packed.size < 13) return ""
        val iv = packed.copyOfRange(0, 12)
        val ciphertext = packed.copyOfRange(12, packed.size)
        Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, worldTidesKey(), GCMParameterSpec(128, iv)) }
            .doFinal(ciphertext).toString(Charsets.UTF_8)
    } catch (_: Exception) { "" }
}

