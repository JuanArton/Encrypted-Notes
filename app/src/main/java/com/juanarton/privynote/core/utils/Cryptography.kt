package com.juanarton.privynote.core.utils

import com.google.android.gms.common.util.Base64Utils
import com.google.crypto.tink.Aead
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.config.TinkConfig
import java.io.ByteArrayOutputStream


class Cryptography {
    companion object {

        fun initTink() {
            TinkConfig.register()
        }

        fun encrypt(data: String, keysetHandle: KeysetHandle): String {
            val aead = keysetHandle.getPrimitive(Aead::class.java)
            val cipherText = aead.encrypt(data.toByteArray(Charsets.UTF_8), null)
            return Base64Utils.encode(cipherText)
        }

        fun decrypt(ciphertext: String, keysetHandle: KeysetHandle): String {
            val aead = keysetHandle.getPrimitive(Aead::class.java)
            val byteArray = Base64Utils.decode(ciphertext)
            return String(aead.decrypt(byteArray, null))
        }

        fun generateKeySet(): KeysetHandle {
            return KeysetHandle.generateNew(PredefinedAeadParameters.XCHACHA20_POLY1305)
        }

        fun serializeKeySet(keysetHandle: KeysetHandle): String {
            return ByteArrayOutputStream().use { outputStream ->
                CleartextKeysetHandle.write(keysetHandle, BinaryKeysetWriter.withOutputStream(outputStream))
                Base64Utils.encode(outputStream.toByteArray())
            }
        }

        fun deserializeKeySet(keySet: String): KeysetHandle {
            val deserializedKeySet = Base64Utils.decode(keySet)

            return CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(deserializedKeySet))
        }

        fun encrypt(data: ByteArray, keysetHandle: KeysetHandle): ByteArray {
            val aead = keysetHandle.getPrimitive(Aead::class.java)
            return aead.encrypt(data, null)
        }

        fun decrypt(data: ByteArray, keysetHandle: KeysetHandle): ByteArray {
            val aead = keysetHandle.getPrimitive(Aead::class.java)
            return aead.decrypt(data, null)
        }
    }
}
