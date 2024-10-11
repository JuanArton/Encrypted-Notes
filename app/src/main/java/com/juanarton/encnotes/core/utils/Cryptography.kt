package com.juanarton.encnotes.core.utils

import com.google.android.gms.common.util.Base64Utils
import com.google.crypto.tink.Aead
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.config.TinkConfig
import java.io.ByteArrayOutputStream
import java.security.GeneralSecurityException


class Cryptography {
    companion object {

        fun initTink() {
            TinkConfig.register()
            /*
            try {
                TinkConfig.register()
            } catch (e: GeneralSecurityException) {
                throw RuntimeException(e)
            }
             */
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
            return KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
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

        /*
        fun deserializeKeySet(keySet: String): KeysetHandle {
            return try {
                val deserializedKeySet = Base64Utils.decode(keySet)

                CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(deserializedKeySet))
            } catch (e: IOException) {
                throw GeneralSecurityException("DeSerialize keySet failed", e)
            }
        }
         */
    }
}
