package dev.intelia.launcher.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.InputStream

enum class BackupError { TooLarge, NotABackup, Newer, Damaged }

sealed interface BackupRead {
    data class Ok(val settings: LauncherSettings) : BackupRead
    data class Failed(val error: BackupError) : BackupRead
}

/**
 * The settings backup file (D17): `{"format":"intelia-settings","version":1,"settings":{…}}`. A file is untrusted:
 * it is bounded, checked and sent through [LauncherSettings.sanitized]; a bad file never replaces anything.
 */
object BackupCodec {
    const val FORMAT = "intelia-settings"
    const val VERSION = 1
    const val MAX_BYTES = 256 * 1024

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun encode(settings: LauncherSettings): String = buildJsonObject {
        put("format", FORMAT)
        put("version", VERSION)
        put("settings", json.encodeToJsonElement(LauncherSettings.serializer(), settings))
    }.toString()

    /** At most [MAX_BYTES] of UTF-8; null when the stream is longer. */
    fun readBounded(input: InputStream): String? {
        // InputStream.readNBytes is API 33; minSdk is 26.
        val buffer = ByteArray(MAX_BYTES + 1)
        var size = 0
        while (size < buffer.size) {
            val n = input.read(buffer, size, buffer.size - size)
            if (n < 0) break
            size += n
        }
        return if (size > MAX_BYTES) null else String(buffer, 0, size, Charsets.UTF_8)
    }

    fun decode(text: String?): BackupRead {
        if (text == null || text.length > MAX_BYTES) return BackupRead.Failed(BackupError.TooLarge)
        return try {
            val root = json.parseToJsonElement(text) as? JsonObject ?: return BackupRead.Failed(BackupError.NotABackup)
            if (root["format"]?.jsonPrimitive?.content != FORMAT) return BackupRead.Failed(BackupError.NotABackup)
            val version = root["version"]?.jsonPrimitive?.intOrNull ?: return BackupRead.Failed(BackupError.NotABackup)
            val settings = root["settings"]?.jsonObject ?: return BackupRead.Failed(BackupError.NotABackup)
            val schema = settings["schema"]?.jsonPrimitive?.intOrNull ?: 0
            if (version > VERSION || schema > LauncherSettings.SCHEMA) return BackupRead.Failed(BackupError.Newer)
            BackupRead.Ok(json.decodeFromJsonElement(LauncherSettings.serializer(), settings).sanitized())
        } catch (_: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException, as are wrong JSON element types.
            BackupRead.Failed(BackupError.Damaged)
        }
    }

    /** The imported document on this phone: its camera cutout and editor position stay, onboarding is done. */
    fun applyTo(current: LauncherSettings, imported: LauncherSettings): LauncherSettings =
        imported.copy(pulseLens = current.pulseLens, editorCard = current.editorCard, onboarded = true).sanitized()
}
