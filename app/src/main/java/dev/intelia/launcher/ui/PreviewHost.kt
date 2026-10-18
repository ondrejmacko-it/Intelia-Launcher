package dev.intelia.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription

/**
 * M1: what a Studio page shows above its sheet. Each kind is drawn by the renderer the real surface uses, fed with the
 * owner's settings, inside a [PreviewCover], so a preview looks exactly like the phone and never launches, plays or
 * runs anything.
 */
enum class PreviewKind {
    /** The sheet takes the whole room. */
    None,
    /** The live Home miniature (clock, wallpaper, dock with LENS, favourites). */
    Home,
}

/** Which preview each Studio page gets. Pure, so the mapping is tested on the JVM. */
object PreviewPlan {
    /** [section] null = Studio's main page. */
    fun kind(section: StudioSection?): PreviewKind = when (section) {
        null, StudioSection.Look, StudioSection.Home, StudioSection.Favorites -> PreviewKind.Home
        // PULSE draws its own state preview inside its list (S3.1); the rest has nothing on Home to show yet.
        else -> PreviewKind.None
    }
}

/** True inside a preview: a renderer must not start effects with side effects (focus, keyboard, haptics, actions). */
val LocalPreviewMode = compositionLocalOf { false }

/**
 * Wraps a renderer as a preview: every touch is consumed before the renderer sees it, TalkBack hears one [label]
 * instead of the renderer's content, and [LocalPreviewMode] is set.
 */
@Composable
fun PreviewCover(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.clearAndSetSemantics { contentDescription = label }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }) {
        CompositionLocalProvider(LocalPreviewMode provides true) { content() }
    }
}
