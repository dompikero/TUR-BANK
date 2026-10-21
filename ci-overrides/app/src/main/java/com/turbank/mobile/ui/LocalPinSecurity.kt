package com.turbank.mobile.ui

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object LocalPinSecurity {
    fun createSalt(): String {
        val value = ByteArray(16)
        SecureRandom().nextBytes(value)
        return Base64.encodeToString(value, Base64.NO_WRAP)
    }

    fun hashPin(pin: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, 120_000, 256)
        return try {
            val derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            Base64.encodeToString(derived, Base64.NO_WRAP)
        } finally {
            spec.clearPassword()
        }
    }

    fun matches(pin: String, salt: String, expectedHash: String): Boolean {
        if (pin.length !in 4..6 || salt.isBlank() || expectedHash.isBlank()) return false
        return runCatching {
            val actual = Base64.decode(hashPin(pin, salt), Base64.NO_WRAP)
            val expected = Base64.decode(expectedHash, Base64.NO_WRAP)
            MessageDigest.isEqual(actual, expected)
        }.getOrDefault(false)
    }
}
