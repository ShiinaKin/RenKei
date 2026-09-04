package io.sakurasou.renkei.util

import java.security.MessageDigest

fun logReference(value: String): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .take(6)
        .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

fun logText(
    value: String,
    maxLength: Int = 200,
): String = value.replace(CONTROL_CHARACTERS, " ").take(maxLength)

private val CONTROL_CHARACTERS = Regex("[\\p{Cc}\\p{Cf}]")
