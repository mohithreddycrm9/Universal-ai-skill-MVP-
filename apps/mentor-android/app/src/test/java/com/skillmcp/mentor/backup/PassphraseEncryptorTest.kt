package com.skillmcp.mentor.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class PassphraseEncryptorTest {
    @Test
    fun roundTrip() {
        val plain = """{"hello":"world"}"""
        val phrase = "test-passphrase".toCharArray()
        val cipher = PassphraseEncryptor.encrypt(plain, phrase)
        val decoded = PassphraseEncryptor.decrypt(cipher, phrase)
        assertEquals(plain, decoded)
    }
}
