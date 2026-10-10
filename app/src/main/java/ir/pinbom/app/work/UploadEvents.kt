package ir.pinbom.app.work

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · upload signals
 *  A finished upload is worth reloading two walls that live in other parts of
 *  the graph — the home feed and the user's own pins — and each of them owns a
 *  ViewModel of its own. Rather than threading a shared ViewModel through every
 *  route, the completion travels through this five-line holder: the upload
 *  ViewModel advances it, the walls apply it once.
 *
 *  The value is a nonce, never a flag: it only moves forward, so two uploads in
 *  a row look like two events and a wall that was off screen can tell the event
 *  it already applied from a fresh one.
 * ──────────────────────────────────────────────────────────────────────────── */

object UploadEvents {

    private val _completed = MutableStateFlow(0L)

    /** Last completed upload, as a nonce. `0` means “nothing yet”. */
    val completed: StateFlow<Long> = _completed.asStateFlow()

    /** Called once, when the server has accepted the pin. */
    fun publishCompleted() {
        _completed.value = System.nanoTime()
    }
}
