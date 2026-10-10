package ir.pinbom.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.pinbom.app.data.usernameFromToken
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Base64

/**
 * A blank username leaves the profile tab on its skeleton forever, so the claim
 * the server really sends (`usr`) must be read — padded or not.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class UsernameFromTokenTest {

    private fun token(payloadJson: String): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        return enc.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray()) + "." +
            enc.encodeToString(payloadJson.toByteArray()) + ".sig"
    }

    @Test
    fun `reads the usr claim the backend issues`() {
        assertEquals("fatality", usernameFromToken(token("""{"sub":1,"sid":2,"usr":"fatality","iss":"pinbom"}""")))
    }

    @Test
    fun `still reads a username claim`() {
        assertEquals("ali", usernameFromToken(token("""{"username":"ali"}""")))
    }

    @Test
    fun `works whatever the padding of the payload turns out to be`() {
        for (name in listOf("a", "ab", "abc", "abcd", "abcde", "فاطمه")) {
            assertEquals(name, usernameFromToken(token("""{"usr":"$name"}""")))
        }
    }

    @Test
    fun `garbage gives a blank name instead of crashing`() {
        assertEquals("", usernameFromToken("not-a-token"))
        assertEquals("", usernameFromToken(token("""{"sub":1}""")))
    }
}
