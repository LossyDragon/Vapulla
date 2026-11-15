package `in`.dragonbra.vapulla.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [TokenCipher] encrypts with a key in the Android Keystore, which only exists on a device,
 * so encrypting and decrypting cannot be tested here. What can be is how it tells the two
 * stored forms apart, and that a token saved before encryption is still read.
 */
class TokenCipherTest {

    @Test
    fun `the encrypted form is told by its prefix`() {
        assertTrue(TokenCipher.isEncrypted("v1:AAAA"))
        assertFalse(TokenCipher.isEncrypted("eyJhbGciOiJFZERTQSJ9.payload.signature"))
        assertFalse(TokenCipher.isEncrypted(""))
    }

    @Test
    fun `a token saved as plain text by an older build is returned as it is`() {
        val token = "eyJhbGciOiJFZERTQSJ9.payload.signature"

        assertEquals(token, TokenCipher.decrypt(token))
    }
}
