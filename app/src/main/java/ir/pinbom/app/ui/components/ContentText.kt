package ir.pinbom.app.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import ir.pinbom.app.ui.theme.PinBomText
import ir.pinbom.app.ui.theme.PinBomTheme

/* ────────────────────────────────────────────────────────────────────────────
 *  PinBom · content-direction text
 *
 *  UI labels follow the layout direction; *user and server content* must not.
 *  A Persian pin title belongs on the right even in the English UI, and an
 *  English board name belongs on the left even in the Persian UI.
 *
 *  `TextDirection.Content` + `TextAlign.Start` does exactly that: the paragraph
 *  direction is taken from the first strong character of the text itself, and
 *  Start resolves against that direction. A mixed line like
 *  «ایده‌ی تصویری شماره 29» keeps its numbers and punctuation where they belong.
 * ──────────────────────────────────────────────────────────────────────────── */

/** The style every piece of user content is rendered with. */
fun TextStyle.byContentDirection(): TextStyle =
    copy(textAlign = TextAlign.Start, textDirection = TextDirection.Content)

/**
 * Text that aligns by its own language. Give it `Modifier.fillMaxWidth()` when
 * the alignment has to be visible (under a pin, a bio, a comment).
 */
@Composable
fun ContentText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = PinBomText.body,
    color: Color = PinBomTheme.colors.textPrimary,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.byContentDirection(),
        color = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * Field for user content (pin titles, descriptions, tags, comments, bios):
 * the same look as [PinBomTextField], with the typed characters laid out by their
 * own language.
 */
@Composable
fun ContentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    error: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    PinBomTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        error = error,
        supportingText = supportingText,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        fieldDirection = FieldDirection.Content,
    )
}
