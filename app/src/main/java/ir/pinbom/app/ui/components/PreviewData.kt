package ir.pinbom.app.ui.components

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ir.pinbom.app.data.remote.Author
import ir.pinbom.app.data.remote.Board
import ir.pinbom.app.data.remote.Comment
import ir.pinbom.app.data.remote.Notification
import ir.pinbom.app.data.remote.Pin
import ir.pinbom.app.data.remote.SearchBoard
import ir.pinbom.app.data.remote.SearchUser
import ir.pinbom.app.data.remote.UserProfile

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · preview fixtures
 *  Only used by @Preview functions. Images use empty URLs on purpose: the
 *  previews then show exactly the placeholder state (deterministic pastels),
 *  which is what a designer needs to check in the IDE.
 * ──────────────────────────────────────────────────────────────────────────── */

internal object PreviewData {

    fun pin(
        id: Long,
        title: String,
        width: Int = 800,
        height: Int = 1200,
        likes: Int = 0,
        saved: Boolean = false,
        liked: Boolean = false,
        author: String = "نگار محمدی",
        username: String = "negar",
        savedAt: String = "2026-09-20T10:15:00Z",
        description: String = "",
        source: String = "",
    ) = Pin(
        id = id,
        title = title,
        description = description,
        source_url = source,
        width = width,
        height = height,
        created_at = savedAt,
        image_url = "",
        author = Author(username = username, display_name = author),
        likes = likes,
        saves = 0,
        comments = 0,
        liked = liked,
        saved = saved,
    )

    val pins: List<Pin> = listOf(
        pin(1, "ایده چیدمان اتاق نشیمن مینیمال", 900, 1350, likes = 1240, saved = true),
        pin(2, "دستور پخت کیک شکلاتی بدون فر", 1000, 1000, likes = 3200, author = "سارا کریمی", username = "sarak"),
        pin(3, "طرح ناخن پاییزی با رنگ‌های گرم", 800, 1600, likes = 870, author = "مریم رضایی", username = "maryam"),
        pin(4, "کافه‌های دنج تهران برای کار کردن", 1200, 800, likes = 540, author = "پویا احمدی", username = "pouya"),
        pin(5, "استایل پوشاک پاییز ۱۴۰۵", 900, 1200, likes = 9800, saved = true, author = "الهام نوری", username = "elham"),
        pin(6, "گیاهان آپارتمانی مقاوم برای نور کم", 1000, 1400, likes = 2100, author = "بهار سلطانی", username = "bahar"),
    )

    val boards: List<Board> = listOf(
        Board(1, 1, "دکوراسیون خانه", "ایده‌های چیدمان و رنگ"),
        Board(2, 1, "دستور پخت", "شیرینی و غذاهای سریع"),
        Board(3, 1, "سفر", "جاهای دیدنی ایران"),
    )

    val covers: List<Pin> = pins.take(3)

    val users: List<SearchUser> = listOf(
        SearchUser(1, "negar", "نگار محمدی", "طراح داخلی"),
        SearchUser(2, "sarak", "سارا کریمی", "آشپز خانگی"),
        SearchUser(3, "pouya", "پویا احمدی", "عکاس"),
    )

    val searchBoards: List<SearchBoard> = listOf(
        SearchBoard(1, "دکوراسیون خانه", "ایده‌های چیدمان"),
        SearchBoard(2, "دستور پخت", "شیرینی و غذاهای سریع"),
    )

    val profile = UserProfile(
        id = 1,
        username = "negar",
        display_name = "نگار محمدی",
        bio = "طراح داخلی · عاشق رنگ‌های گرم و گیاهان آپارتمانی",
        avatar_url = "",
        followers = 12450,
        following = 312,
        pins = 148,
        following_me = false,
        me = true,
    )

    val comments: List<Comment> = listOf(
        Comment(1, "عالی بود، حتماً امتحان می‌کنم 🌿", "2026-10-06T09:00:00Z", Author("sarak", "سارا کریمی")),
        Comment(2, "رنگ دیوار چی بود؟", "2026-10-05T18:30:00Z", Author("pouya", "پویا احمدی")),
    )

    val notifications: List<Notification> = listOf(
        Notification(1, "like", 2, 4, "پین شما را پسندید", false, "2026-10-07T08:00:00Z", Author("sarak", "سارا کریمی")),
        Notification(2, "comment", 3, 4, "نظر داد: «رنگ دیوار چی بود؟»", true, "2026-10-04T12:00:00Z", Author("pouya", "پویا احمدی")),
        Notification(3, "follow", 4, 0, "شما را دنبال کرد", true, "2026-09-28T12:00:00Z", Author("elham", "الهام نوری")),
    )
}

/** Lets a preview render a component in both themes from one function. */
internal class ThemePreviewParameter : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(false, true)
}
