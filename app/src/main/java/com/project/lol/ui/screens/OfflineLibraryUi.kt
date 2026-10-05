package com.project.lol.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.lol.R
import com.project.lol.offline.OfflineSong
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronDown
import compose.icons.tablericons.ChevronLeft
import compose.icons.tablericons.CloudOff
import compose.icons.tablericons.Disc
import compose.icons.tablericons.Dots
import compose.icons.tablericons.Music
import compose.icons.tablericons.PlayerStop
import compose.icons.tablericons.Search
import compose.icons.tablericons.Settings
import compose.icons.tablericons.Wifi
import compose.icons.tablericons.X
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

// ---------------------------------------------------------------------------------------------
// Palette, icons, glass
// ---------------------------------------------------------------------------------------------

internal object OfflinePalette {
    val Green = Color(0xFF1ED760)
    val Background = Color(0xFF121212)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFB3B3B3)
    val TextMuted = Color(0xFF7C7C7C)
    val Placeholder = Color(0xFF282828)
    val Sheet = Color(0xFF232323)
    val DownloadsTile = Brush.linearGradient(listOf(Color(0xFF138A5E), Color(0xFF07402C)))
    val LikedTile = Brush.linearGradient(listOf(Color(0xFF450AF5), Color(0xFF8E8EE5), Color(0xFFC4EFD9)))
    val SinglesTile = Brush.linearGradient(listOf(Color(0xFF4B5563), Color(0xFF1F2937)))
    val DownloadsTint = Color(0xFF15583F)
    val LikedTint = Color(0xFF4A3AA8)
    val NeutralTint = Color(0xFF3A3A3A)
    val SinglesTint = Color(0xFF374151)
}

/** Filled glyphs Tabler does not have (it only has outline play and pause, and no shuffle). */
internal object OfflineIcons {
    val Play: ImageVector by lazy {
        filledIcon("Play", "M8,5.14V18.86C8,19.65 8.87,20.13 9.54,19.7L20.33,12.84C20.95,12.45 20.95,11.55 20.33,11.16L9.54,4.3C8.87,3.87 8,4.35 8,5.14Z")
    }
    val Pause: ImageVector by lazy {
        filledIcon("Pause", "M7,4H10A1,1 0,0 1,11 5V19A1,1 0,0 1,10 20H7A1,1 0,0 1,6 19V5A1,1 0,0 1,7 4ZM14,4H17A1,1 0,0 1,18 5V19A1,1 0,0 1,17 20H14A1,1 0,0 1,13 19V5A1,1 0,0 1,14 4Z")
    }
    val Next: ImageVector by lazy {
        filledIcon("Next", "M5,6.4V17.6C5,18.4 5.9,18.9 6.6,18.4L14.5,12.8C15.1,12.4 15.1,11.6 14.5,11.2L6.6,5.6C5.9,5.1 5,5.6 5,6.4ZM16.5,5H19V19H16.5Z")
    }
    val Previous: ImageVector by lazy {
        filledIcon("Previous", "M19,6.4V17.6C19,18.4 18.1,18.9 17.4,18.4L9.5,12.8C8.9,12.4 8.9,11.6 9.5,11.2L17.4,5.6C18.1,5.1 19,5.6 19,6.4ZM5,5H7.5V19H5Z")
    }
    val Shuffle: ImageVector by lazy {
        filledIcon("Shuffle", "M10.59,9.17L5.41,4 4,5.41l5.17,5.17 1.42,-1.41zM14.5,4l2.04,2.04L4,18.59 5.41,20 17.96,7.46 20,9.5L20,4h-5.5zM14.83,13.41l-1.41,1.41 3.13,3.13L14.5,20L20,20v-5.5l-2.04,2.04 -3.13,-3.13z")
    }
    val Heart: ImageVector by lazy {
        filledIcon("Heart", "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09C13.09,3.81 14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78 -3.4,6.86 -8.55,11.54L12,21.35z")
    }
    val Download: ImageVector by lazy {
        filledIcon("Download", "M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z")
    }

    private fun filledIcon(name: String, path: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black)).build()
}

/** iOS style frosted surface: translucent fill, a soft top sheen and a light rim. */
internal fun Modifier.glass(
    shape: Shape,
    fill: Color = Color.White.copy(alpha = 0.07f),
    rim: Float = 0.12f,
): Modifier = this
    .clip(shape)
    .background(fill)
    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)))
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = rim), Color.White.copy(alpha = rim * 0.3f))),
        shape = shape,
    )

/** Click with a small spring "press" instead of a ripple, for round buttons. */
internal fun Modifier.bouncyClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "press",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

// ---------------------------------------------------------------------------------------------
// Artwork: decoding off the main thread, an in-memory cache, and a tint taken from the cover
// ---------------------------------------------------------------------------------------------

internal object ArtCache {
    private val bitmaps = object : LruCache<String, ImageBitmap>(cacheKb()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.width * value.height * 4 / 1024).coerceAtLeast(1)
    }
    private val tints = LruCache<String, Int>(256)
    private val missing = LruCache<String, Boolean>(512)

    private fun cacheKb(): Int =
        (Runtime.getRuntime().maxMemory() / 1024L / 8L).toInt().coerceIn(8 * 1024, 64 * 1024)

    fun bucketFor(px: Int): Int = when {
        px <= 180 -> 180
        px <= 360 -> 360
        px <= 640 -> 640
        else -> 1024
    }

    private fun key(art: CoverArt, bucket: Int) = "${art.cacheKey}@$bucket"

    fun cached(art: CoverArt, bucket: Int): ImageBitmap? = bitmaps.get(key(art, bucket))

    fun load(context: Context, art: CoverArt, bucket: Int): ImageBitmap? {
        val key = key(art, bucket)
        bitmaps.get(key)?.let { return it }
        if (missing.get(key) == true) return null
        val bmp = decodeArt(context, art, bucket)
        if (bmp == null) {
            missing.put(key, true)
            return null
        }
        return bmp.asImageBitmap().also { bitmaps.put(key, it) }
    }

    /** A dark, slightly saturated color from the cover, for gradients behind it. */
    fun tint(context: Context, art: CoverArt): Int? {
        tints.get(art.cacheKey)?.let { return it }
        val img = load(context, art, 180) ?: return null
        val color = runCatching { dominantColor(img.asAndroidBitmap()) }.getOrNull() ?: return null
        tints.put(art.cacheKey, color)
        return color
    }

    private fun sampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        val shortest = min(width, height)
        if (shortest <= 0) return 1
        while (shortest / (sample * 2) >= target) sample *= 2
        return sample
    }

    private fun decodeArt(context: Context, art: CoverArt, bucket: Int): Bitmap? {
        art.file?.takeIf { it.exists() && it.length() > 0 }?.let { file ->
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, bucket)
                }
                BitmapFactory.decodeFile(file.absolutePath, opts)
            }.getOrNull()?.let { return it }
        }
        val uri: Uri = art.audio ?: return null
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.embeddedPicture?.let { data ->
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, bucket)
                    }
                    BitmapFactory.decodeByteArray(data, 0, data.size, opts)
                }
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrNull()
    }

    private fun dominantColor(source: Bitmap): Int {
        val small = Bitmap.createScaledBitmap(source, 24, 24, true)
        var r = 0.0
        var g = 0.0
        var b = 0.0
        var total = 0.0
        val hsv = FloatArray(3)
        for (y in 0 until small.height) {
            for (x in 0 until small.width) {
                val p = small.getPixel(x, y)
                android.graphics.Color.colorToHSV(p, hsv)
                // Colorful, mid-bright pixels say more about a cover than black or white ones.
                val weight = 0.15 + hsv[1] * (1.0 - kotlin.math.abs(hsv[2] - 0.6))
                r += android.graphics.Color.red(p) * weight
                g += android.graphics.Color.green(p) * weight
                b += android.graphics.Color.blue(p) * weight
                total += weight
            }
        }
        if (small != source) small.recycle()
        if (total <= 0.0) return android.graphics.Color.rgb(58, 58, 58)
        val avg = android.graphics.Color.rgb((r / total).toInt(), (g / total).toInt(), (b / total).toInt())
        android.graphics.Color.colorToHSV(avg, hsv)
        if (hsv[1] > 0.12f) hsv[1] = hsv[1].coerceIn(0.35f, 0.75f)
        hsv[2] = hsv[2].coerceIn(0.30f, 0.50f)
        return android.graphics.Color.HSVToColor(hsv)
    }
}

@Composable
internal fun rememberCoverBitmap(art: CoverArt?, size: Dp): ImageBitmap? {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val bucket = ArtCache.bucketFor(px)
    val key = art?.cacheKey
    var bitmap by remember(key, bucket) { mutableStateOf(art?.let { ArtCache.cached(it, bucket) }) }
    LaunchedEffect(key, bucket) {
        if (art != null && bitmap == null) {
            bitmap = withContext(Dispatchers.IO) { ArtCache.load(context.applicationContext, art, bucket) }
        }
    }
    return bitmap
}

@Composable
internal fun rememberCoverTint(art: CoverArt?, fallback: Color): Color {
    val context = LocalContext.current
    val key = art?.cacheKey
    var tint by remember(key) { mutableStateOf<Color?>(null) }
    LaunchedEffect(key) {
        if (art != null) {
            tint = withContext(Dispatchers.IO) { ArtCache.tint(context.applicationContext, art) }?.let { Color(it) }
        }
    }
    val animated by animateColorAsState(tint ?: fallback, animationSpec = tween(450), label = "tint")
    return animated
}

@Composable
internal fun CoverImage(
    art: CoverArt?,
    size: Dp,
    corner: Dp,
    modifier: Modifier = Modifier,
    placeholder: ImageVector = TablerIcons.Music,
) {
    val bitmap = rememberCoverBitmap(art, size)
    val alpha by animateFloatAsState(if (bitmap != null) 1f else 0f, tween(200), label = "coverFade")
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(OfflinePalette.Placeholder),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap == null || alpha < 1f) {
            Icon(
                imageVector = placeholder,
                contentDescription = null,
                tint = OfflinePalette.TextMuted,
                modifier = Modifier.size(size * 0.4f)
            )
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha }
            )
        }
    }
}

@Composable
private fun GradientTile(size: Dp, shape: Shape, brush: Brush, icon: ImageVector, iconTint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(size * 0.42f))
    }
}

@Composable
internal fun CollectionArtwork(item: LibraryItem, size: Dp, corner: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(corner)
    when {
        item.kind == LibraryKind.Downloads ->
            GradientTile(size, shape, OfflinePalette.DownloadsTile, OfflineIcons.Download, OfflinePalette.Green, modifier)
        item.kind == LibraryKind.Liked ->
            GradientTile(size, shape, OfflinePalette.LikedTile, OfflineIcons.Heart, Color.White, modifier)
        item.kind == LibraryKind.Singles ->
            GradientTile(size, shape, OfflinePalette.SinglesTile, TablerIcons.Music, Color.White, modifier)
        item.mosaic.size == 4 -> Column(
            modifier = modifier
                .size(size)
                .clip(shape)
        ) {
            for (row in 0 until 2) {
                Row {
                    for (col in 0 until 2) {
                        CoverImage(art = item.mosaic[row * 2 + col], size = size / 2, corner = 0.dp)
                    }
                }
            }
        }
        else -> CoverImage(
            art = item.cover,
            size = size,
            corner = corner,
            modifier = modifier,
            placeholder = if (item.kind == LibraryKind.Album) TablerIcons.Disc else TablerIcons.Music
        )
    }
}

internal fun LibraryItem.tintArt(): CoverArt? = cover ?: mosaic.firstOrNull()

@Composable
internal fun LibraryItem.headerTint(): Color = when (kind) {
    LibraryKind.Downloads -> OfflinePalette.DownloadsTint
    LibraryKind.Liked -> OfflinePalette.LikedTint
    LibraryKind.Singles -> OfflinePalette.SinglesTint
    else -> rememberCoverTint(tintArt(), OfflinePalette.NeutralTint)
}

/** Spotify's small green "downloaded" badge. */
@Composable
internal fun DownloadBadge(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        drawCircle(OfflinePalette.Green)
        val w = this.size.width
        val path = Path().apply {
            moveTo(w * 0.5f, w * 0.27f)
            lineTo(w * 0.5f, w * 0.71f)
            moveTo(w * 0.31f, w * 0.53f)
            lineTo(w * 0.5f, w * 0.72f)
            lineTo(w * 0.69f, w * 0.53f)
        }
        drawPath(
            path = path,
            color = Color.Black,
            style = Stroke(width = w * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/** Three bouncing bars next to the song that is playing. */
@Composable
private fun PlayingBars(playing: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bars")
    val a by transition.animateFloat(0.25f, 1f, infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by transition.animateFloat(1f, 0.3f, infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    val c by transition.animateFloat(0.4f, 0.9f, infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse), label = "c")
    Canvas(modifier = modifier.size(14.dp)) {
        val bar = size.width / 5f
        val heights = if (playing) floatArrayOf(a, b, c) else floatArrayOf(0.35f, 0.35f, 0.35f)
        heights.forEachIndexed { i, h ->
            val barHeight = size.height * h
            drawRoundRect(
                color = OfflinePalette.Green,
                topLeft = Offset(i * bar * 2f, size.height - barHeight),
                size = Size(bar, barHeight),
                cornerRadius = CornerRadius(bar / 2f, bar / 2f)
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Labels
// ---------------------------------------------------------------------------------------------

@Composable
internal fun LibraryItem.displayName(): String = when (kind) {
    LibraryKind.Downloads -> stringResource(R.string.offline_lib_downloaded_songs)
    LibraryKind.Singles -> stringResource(R.string.offline_lib_single_songs)
    else -> name
}

@Composable
internal fun LibraryItem.kindLabel(): String = when (kind) {
    LibraryKind.Album -> stringResource(R.string.offline_lib_kind_album)
    LibraryKind.Singles -> stringResource(R.string.offline_lib_kind_songs)
    else -> stringResource(R.string.offline_lib_kind_playlist)
}

@Composable
private fun LibraryItem.librarySubtitle(): String {
    val detail = when {
        downloaded == 0 -> stringResource(R.string.offline_lib_none_downloaded)
        downloaded < total -> stringResource(R.string.offline_lib_x_of_y_downloaded, downloaded, total)
        kind == LibraryKind.Album && artist.isNotBlank() -> artist
        else -> pluralStringResource(R.plurals.offline_lib_songs, total, total)
    }
    return "${kindLabel()} · $detail"
}

@Composable
private fun LibraryItem.detailMeta(): String {
    val songs = pluralStringResource(R.plurals.offline_lib_songs, total, total)
    val got = if (downloaded == 0) {
        stringResource(R.string.offline_lib_none_downloaded)
    } else {
        stringResource(R.string.offline_lib_n_downloaded, downloaded)
    }
    return "${kindLabel()} · $songs · $got"
}

// ---------------------------------------------------------------------------------------------
// Rows
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryRow(
    item: LibraryItem,
    playingHere: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = item.downloaded == 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .alpha(if (dim) 0.45f else 1f)
    ) {
        CollectionArtwork(item = item, size = 56.dp, corner = 6.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName(),
                color = if (playingHere) OfflinePalette.Green else OfflinePalette.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.downloaded > 0) {
                    DownloadBadge(size = 13.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = item.librarySubtitle(),
                    color = OfflinePalette.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun TrackRow(
    title: String,
    artist: String,
    art: CoverArt?,
    downloaded: Boolean,
    current: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onMore: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
            .alpha(if (downloaded) 1f else 0.35f)
    ) {
        CoverImage(art = art, size = 48.dp, corner = 4.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (current) {
                    PlayingBars(playing = playing)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    color = if (current) OfflinePalette.Green else OfflinePalette.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (downloaded) {
                    DownloadBadge(size = 12.dp)
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    text = artist.ifBlank { stringResource(R.string.offline_unknown_artist) },
                    color = OfflinePalette.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onMore != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onMore),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TablerIcons.Dots,
                    contentDescription = stringResource(R.string.offline_lib_desc_more),
                    tint = OfflinePalette.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = OfflinePalette.TextPrimary,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

// ---------------------------------------------------------------------------------------------
// Library page
// ---------------------------------------------------------------------------------------------

@Composable
private fun LibraryHeader(onSettings: () -> Unit, onSearch: () -> Unit, searchOpen: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 2.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_spotios_logo),
            contentDescription = stringResource(R.string.offline_desc_settings),
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onSettings)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.offline_lib_title),
            color = OfflinePalette.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        RoundIconButton(
            icon = if (searchOpen) TablerIcons.X else TablerIcons.Search,
            contentDescription = if (searchOpen) {
                stringResource(R.string.offline_desc_clear_search)
            } else {
                stringResource(R.string.offline_lib_desc_search)
            },
            onClick = onSearch,
            size = 44.dp
        )
        RoundIconButton(
            icon = TablerIcons.Settings,
            contentDescription = stringResource(R.string.offline_desc_settings),
            onClick = onSettings,
            size = 44.dp
        )
    }
}

@Composable
private fun OfflineStrip(onGoOnline: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .glass(RoundedCornerShape(16.dp))
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Icon(
            imageVector = TablerIcons.CloudOff,
            contentDescription = null,
            tint = OfflinePalette.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.offline_lib_youre_offline),
                color = OfflinePalette.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.offline_lib_offline_detail),
                color = OfflinePalette.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onGoOnline)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = TablerIcons.Wifi,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.offline_lib_go_online),
                color = Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) OfflinePalette.Green else Color.White.copy(alpha = 0.08f),
        tween(180),
        label = "chipBg"
    )
    val fg by animateColorAsState(if (selected) Color.Black else OfflinePalette.TextPrimary, tween(180), label = "chipFg")
    val rim by animateFloatAsState(if (selected) 0f else 0.14f, tween(180), label = "chipRim")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .bouncyClickable(onClick = onClick)
            .height(34.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = rim), CircleShape)
            .padding(horizontal = 16.dp)
    ) {
        Text(text = label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun FilterChips(filter: LibraryFilter?, onFilterChange: (LibraryFilter?) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        AnimatedVisibility(
            visible = filter != null,
            enter = fadeIn(tween(160)) + expandHorizontally(tween(200)),
            exit = fadeOut(tween(120)) + shrinkHorizontally(tween(180))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .bouncyClickable { onFilterChange(null) }
                    .glass(CircleShape, rim = 0.14f)
            ) {
                Icon(
                    imageVector = TablerIcons.X,
                    contentDescription = stringResource(R.string.offline_lib_desc_clear_filter),
                    tint = OfflinePalette.TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        val labels = mapOf(
            LibraryFilter.Playlists to stringResource(R.string.offline_lib_filter_playlists),
            LibraryFilter.Albums to stringResource(R.string.offline_lib_filter_albums),
            LibraryFilter.Songs to stringResource(R.string.offline_lib_filter_songs),
        )
        LibraryFilter.entries.forEach { f ->
            FilterChip(
                label = labels.getValue(f),
                selected = filter == f,
                onClick = { onFilterChange(if (filter == f) null else f) }
            )
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(40.dp)
            .glass(RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp)
    ) {
        Icon(
            imageVector = TablerIcons.Search,
            contentDescription = null,
            tint = OfflinePalette.TextSecondary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.offline_search_placeholder),
                    color = OfflinePalette.TextMuted,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                    color = OfflinePalette.TextPrimary,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(OfflinePalette.Green),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChange(it.isFocused) }
            )
        }
        if (value.isNotEmpty()) {
            RoundIconButton(
                icon = TablerIcons.X,
                contentDescription = stringResource(R.string.offline_desc_clear_search),
                onClick = { onValueChange("") },
                tint = OfflinePalette.TextSecondary,
                size = 30.dp,
                iconSize = 15.dp
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = OfflinePalette.TextPrimary,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun CenterMessage(title: String, body: String, modifier: Modifier = Modifier, icon: ImageVector? = null, action: (@Composable () -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = OfflinePalette.TextMuted, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(14.dp))
        }
        Text(
            text = title,
            color = OfflinePalette.TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            color = OfflinePalette.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

@Composable
internal fun LibraryPage(
    library: OfflineLibrary?,
    listState: LazyListState,
    filter: LibraryFilter?,
    onFilterChange: (LibraryFilter?) -> Unit,
    searchOpen: Boolean,
    onSearchOpenChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    songResults: List<OfflineSong>?,
    currentSongId: String?,
    isPlaying: Boolean,
    playingSourceKey: String?,
    bottomPadding: Dp,
    versionLabel: String,
    onOpenItem: (LibraryItem) -> Unit,
    onItemMenu: (LibraryItem) -> Unit,
    onPlaySong: (OfflineSong) -> Unit,
    onSongMenu: (OfflineSong) -> Unit,
    onSettings: () -> Unit,
    onGoOnline: () -> Unit,
) {
    val query = searchQuery.trim()
    val visibleItems = remember(library, filter) {
        library?.items?.filter { it.matchesFilter(filter) }.orEmpty()
    }
    val matchingItems = remember(library, query) {
        if (query.isEmpty()) {
            emptyList()
        } else {
            library?.items?.filter { it.isCollection && it.name.contains(query, ignoreCase = true) }.orEmpty()
        }
    }
    val scrolled by remember { derivedStateOf { listState.canScrollBackward } }
    val edgeAlpha by animateFloatAsState(if (scrolled) 1f else 0f, tween(150), label = "edge")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        LibraryHeader(
            onSettings = onSettings,
            onSearch = { onSearchOpenChange(!searchOpen) },
            searchOpen = searchOpen
        )
        OfflineStrip(onGoOnline = onGoOnline)
        AnimatedContent(
            targetState = searchOpen,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "chipsOrSearch"
        ) { open ->
            if (open) {
                SearchField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    onFocusChange = onSearchFocusChange
                )
            } else {
                FilterChips(filter = filter, onFilterChange = onFilterChange)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                library == null -> CircularProgressIndicator(
                    color = OfflinePalette.Green,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                )

                library.isEmpty -> CenterMessage(
                    title = stringResource(R.string.offline_empty_nothing_here_yet),
                    body = stringResource(R.string.offline_empty_download_hint),
                    icon = TablerIcons.CloudOff,
                    modifier = Modifier.align(Alignment.Center),
                    action = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .bouncyClickable(onClick = onGoOnline)
                                .height(44.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.offline_lib_go_online),
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )

                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 4.dp, bottom = bottomPadding),
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        query.isNotEmpty() -> {
                            val songs = songResults
                            if (songs != null && songs.isEmpty() && matchingItems.isEmpty()) {
                                item(key = "noResults") {
                                    CenterMessage(
                                        title = stringResource(R.string.offline_empty_no_results),
                                        body = stringResource(R.string.offline_empty_nothing_matches, query),
                                        icon = TablerIcons.Search
                                    )
                                }
                            }
                            if (matchingItems.isNotEmpty()) {
                                item(key = "hdrCollections") {
                                    SectionLabel(stringResource(R.string.offline_lib_section_collections))
                                }
                                items(matchingItems, key = { "r:${it.key}" }) { item ->
                                    LibraryRow(
                                        item = item,
                                        playingHere = item.key == playingSourceKey,
                                        onClick = { onOpenItem(item) },
                                        onLongClick = { onItemMenu(item) }
                                    )
                                }
                            }
                            if (!songs.isNullOrEmpty()) {
                                item(key = "hdrSongs") {
                                    SectionLabel(stringResource(R.string.offline_lib_section_songs))
                                }
                                items(songs, key = { "s:${it.id}" }) { song ->
                                    TrackRow(
                                        title = song.title,
                                        artist = song.artist,
                                        art = song.coverArt(),
                                        downloaded = true,
                                        current = song.id == currentSongId,
                                        playing = isPlaying,
                                        onClick = { onPlaySong(song) },
                                        onMore = { onSongMenu(song) }
                                    )
                                }
                            }
                        }

                        filter == LibraryFilter.Songs -> {
                            if (library.songs.isEmpty()) {
                                item(key = "emptySongs") {
                                    CenterMessage(
                                        title = stringResource(R.string.offline_empty_nothing_here_yet),
                                        body = stringResource(R.string.offline_empty_download_hint)
                                    )
                                }
                            }
                            items(library.songs, key = { "s:${it.id}" }) { song ->
                                TrackRow(
                                    title = song.title,
                                    artist = song.artist,
                                    art = song.coverArt(),
                                    downloaded = true,
                                    current = song.id == currentSongId,
                                    playing = isPlaying,
                                    onClick = { onPlaySong(song) },
                                    onMore = { onSongMenu(song) }
                                )
                            }
                        }

                        else -> {
                            if (visibleItems.isEmpty()) {
                                item(key = "emptyFilter") {
                                    CenterMessage(
                                        title = stringResource(R.string.offline_empty_nothing_here_yet),
                                        body = stringResource(R.string.offline_empty_download_hint)
                                    )
                                }
                            }
                            items(visibleItems, key = { it.key }) { item ->
                                LibraryRow(
                                    item = item,
                                    playingHere = item.key == playingSourceKey,
                                    onClick = { onOpenItem(item) },
                                    onLongClick = { onItemMenu(item) }
                                )
                            }
                        }
                    }
                    item(key = "footer") {
                        Text(
                            text = versionLabel,
                            color = OfflinePalette.TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp, bottom = 8.dp)
                        )
                    }
                }
            }

            // Soft edge under the fixed header once the list moves.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(10.dp)
                    .graphicsLayer { alpha = edgeAlpha }
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)))
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Playlist / album page
// ---------------------------------------------------------------------------------------------

@Composable
internal fun CollectionPage(
    item: LibraryItem,
    background: Color,
    currentSongId: String?,
    isPlaying: Boolean,
    isCurrentSource: Boolean,
    shuffleOn: Boolean,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onMore: () -> Unit,
    onTrackClick: (LibraryTrack) -> Unit,
    onTrackMenu: (OfflineSong) -> Unit,
) {
    val listState = remember(item.key) { LazyListState() }
    val tint = item.headerTint()
    val barColor = lerp(tint, Color.Black, 0.45f)
    val title = item.displayName()
    var headerHeight by remember { mutableIntStateOf(1) }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    fun headerOffset(): Float =
        if (listState.firstVisibleItemIndex > 0) Float.MAX_VALUE else listState.firstVisibleItemScrollOffset.toFloat()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = bottomPadding),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { headerHeight = it.height.coerceAtLeast(1) }
                        .background(Brush.verticalGradient(0f to tint, 0.92f to background))
                        .padding(top = statusTop + 56.dp)
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val coverSize = (maxWidth * 0.64f).coerceAtMost(280.dp)
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    val offset = headerOffset()
                                    // Spotify-style: the cover shrinks, fades and lags behind the scroll.
                                    val p = (offset / coverSize.toPx()).coerceIn(0f, 1f)
                                    val scale = 1f - 0.2f * p
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - p
                                    translationY = min(offset, coverSize.toPx()) * 0.35f
                                }
                                .shadow(28.dp, RoundedCornerShape(8.dp), ambientColor = Color.Black, spotColor = Color.Black)
                        ) {
                            CollectionArtwork(item = item, size = coverSize, corner = 8.dp)
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = title,
                            color = OfflinePalette.TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.kind == LibraryKind.Album && item.artist.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = item.artist,
                                color = OfflinePalette.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.detailMeta(),
                            color = OfflinePalette.TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                                if (item.downloaded > 0) {
                                    DownloadBadge(size = 24.dp)
                                } else {
                                    Icon(
                                        imageVector = OfflineIcons.Download,
                                        contentDescription = null,
                                        tint = OfflinePalette.TextMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            RoundIconButton(
                                icon = TablerIcons.Dots,
                                contentDescription = stringResource(R.string.offline_lib_desc_more),
                                onClick = onMore,
                                tint = OfflinePalette.TextSecondary,
                                size = 44.dp,
                                iconSize = 24.dp
                            )
                            Spacer(Modifier.weight(1f))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .bouncyClickable(onClick = onShuffle)
                            ) {
                                Icon(
                                    imageVector = OfflineIcons.Shuffle,
                                    contentDescription = stringResource(R.string.offline_lib_desc_shuffle),
                                    tint = if (shuffleOn) OfflinePalette.Green else OfflinePalette.TextSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                                if (shuffleOn) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 3.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(OfflinePalette.Green)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            val showPause = isCurrentSource && isPlaying
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .bouncyClickable(onClick = onPlay)
                                    .alpha(if (item.downloaded > 0) 1f else 0.4f)
                                    .shadow(10.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                                    .clip(CircleShape)
                                    .background(OfflinePalette.Green)
                            ) {
                                Icon(
                                    imageVector = if (showPause) OfflineIcons.Pause else OfflineIcons.Play,
                                    contentDescription = if (showPause) {
                                        stringResource(R.string.offline_desc_pause)
                                    } else {
                                        stringResource(R.string.offline_desc_play)
                                    },
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            itemsIndexed(item.tracks, key = { _, t -> "t:${t.key}" }) { _, track ->
                val song = track.song
                TrackRow(
                    title = track.title,
                    artist = track.artist,
                    art = track.art,
                    downloaded = song != null,
                    current = song != null && song.id == currentSongId,
                    playing = isPlaying,
                    onClick = { onTrackClick(track) },
                    onMore = song?.let { s -> { onTrackMenu(s) } }
                )
            }
        }

        // Top bar: fades in as the cover scrolls away.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val start = headerHeight * 0.30f
                    val span = headerHeight * 0.22f
                    val a = ((headerOffset() - start) / span).coerceIn(0f, 1f)
                    drawRect(barColor.copy(alpha = a))
                }
                .statusBarsPadding()
                .height(56.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .size(38.dp)
                    .bouncyClickable(onClick = onBack)
                    .glass(CircleShape, fill = Color.Black.copy(alpha = 0.28f), rim = 0.18f)
            ) {
                Icon(
                    imageVector = TablerIcons.ChevronLeft,
                    contentDescription = stringResource(R.string.offline_lib_desc_back),
                    tint = OfflinePalette.TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                color = OfflinePalette.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 64.dp)
                    .graphicsLayer {
                        val start = headerHeight * 0.52f
                        val span = headerHeight * 0.12f
                        alpha = ((headerOffset() - start) / span).coerceIn(0f, 1f)
                    }
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Mini player and Now Playing
// ---------------------------------------------------------------------------------------------

@Composable
internal fun MiniPlayer(
    song: OfflineSong,
    isPlaying: Boolean,
    progress: () -> Float,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val art = remember(song.id, song.uri) { song.coverArt() }
    val tint = rememberCoverTint(art, OfflinePalette.NeutralTint)
    val shape = RoundedCornerShape(16.dp)
    val openLabel = stringResource(R.string.offline_lib_desc_open_now_playing)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(62.dp)
            .shadow(18.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .glass(shape, fill = lerp(Color(0xFF1A1A1A), tint, 0.5f).copy(alpha = 0.93f), rim = 0.2f)
            .clickable(onClickLabel = openLabel, onClick = onOpen)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 9.dp, end = 4.dp)
        ) {
            CoverImage(art = art, size = 44.dp, corner = 8.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = OfflinePalette.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DownloadBadge(size = 11.dp)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = song.artist.ifBlank { stringResource(R.string.offline_unknown_artist) },
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            RoundIconButton(
                icon = if (isPlaying) OfflineIcons.Pause else OfflineIcons.Play,
                contentDescription = if (isPlaying) {
                    stringResource(R.string.offline_desc_pause)
                } else {
                    stringResource(R.string.offline_desc_play)
                },
                onClick = onTogglePlay,
                size = 44.dp,
                iconSize = 26.dp
            )
            RoundIconButton(
                icon = OfflineIcons.Next,
                contentDescription = stringResource(R.string.offline_desc_next),
                onClick = onNext,
                size = 44.dp,
                iconSize = 24.dp
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .height(2.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .drawBehind {
                    drawRect(Color.White, size = Size(size.width * progress().coerceIn(0f, 1f), size.height))
                }
        )
    }
}

@Composable
internal fun SeekBar(
    positionMs: Int,
    durationMs: Int,
    scrubbing: Boolean,
    onScrub: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
    progressColor: Color = Color.White,
    trackColor: Color = Color.White.copy(alpha = 0.22f),
) {
    val total = durationMs.coerceAtLeast(1)
    val fraction = positionMs.coerceIn(0, total).toFloat() / total.toFloat()
    var widthPx by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }

    fun msAt(x: Float): Int =
        if (widthPx <= 0) 0 else ((x / widthPx) * total).toInt().coerceIn(0, total)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(total, widthPx) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        onScrub(msAt(offset.x))
                    },
                    onDragEnd = {
                        dragging = false
                        onScrubFinished()
                    },
                    onDragCancel = {
                        dragging = false
                        onScrubFinished()
                    }
                ) { change, _ ->
                    change.consume()
                    onScrub(msAt(change.position.x))
                }
            }
            .pointerInput(total, widthPx) {
                detectTapGestures { offset ->
                    onScrub(msAt(offset.x))
                    onScrubFinished()
                }
            }
            .drawBehind {
                val active = dragging || scrubbing
                val centerY = size.height / 2f
                val trackHeight = (if (active) 6.dp else 4.dp).toPx()
                val radius = trackHeight / 2f
                val progressWidth = size.width * fraction.coerceIn(0f, 1f)
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(0f, centerY - radius),
                    size = Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(radius, radius)
                )
                drawRoundRect(
                    color = if (active) OfflinePalette.Green else progressColor,
                    topLeft = Offset(0f, centerY - radius),
                    size = Size(progressWidth, trackHeight),
                    cornerRadius = CornerRadius(radius, radius)
                )
                val thumbRadius = (if (active) 8.dp else 6.dp).toPx()
                drawCircle(
                    color = progressColor,
                    radius = thumbRadius,
                    center = Offset(
                        x = progressWidth.coerceIn(thumbRadius, size.width - thumbRadius),
                        y = centerY
                    )
                )
            }
    )
}

@Composable
internal fun NowPlayingView(
    song: OfflineSong,
    sourceLabel: String,
    sourceName: String,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    scrubMs: Int,
    shuffleOn: Boolean,
    background: Color,
    onScrub: (Int) -> Unit,
    onScrubFinished: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onClose: () -> Unit,
    onStop: () -> Unit,
) {
    val art = remember(song.id, song.uri) { song.coverArt() }
    val tint = rememberCoverTint(art, OfflinePalette.NeutralTint)
    val scope = rememberCoroutineScope()
    val dragY = remember { Animatable(0f) }
    val dismissPx = with(LocalDensity.current) { 140.dp.toPx() }
    val scrubbing = scrubMs >= 0
    val shownPosition = if (scrubbing) scrubMs else positionMs
    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "coverScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = dragY.value }
            .background(background)
            .background(Brush.verticalGradient(0f to tint, 0.55f to lerp(tint, background, 0.6f), 1f to background))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (dragY.value > dismissPx) onClose() else dragY.animateTo(0f, spring(dampingRatio = 0.8f))
                        }
                    },
                    onDragCancel = { scope.launch { dragY.animateTo(0f) } }
                ) { change, dy ->
                    change.consume()
                    scope.launch { dragY.snapTo(max(0f, dragY.value + dy)) }
                }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                RoundIconButton(
                    icon = TablerIcons.ChevronDown,
                    contentDescription = stringResource(R.string.offline_lib_desc_close_now_playing),
                    onClick = onClose,
                    size = 44.dp,
                    iconSize = 26.dp
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = sourceLabel.uppercase(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sourceName,
                        color = OfflinePalette.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                RoundIconButton(
                    icon = TablerIcons.PlayerStop,
                    contentDescription = stringResource(R.string.offline_lib_desc_stop),
                    onClick = onStop,
                    tint = Color.White.copy(alpha = 0.8f),
                    size = 44.dp,
                    iconSize = 22.dp
                )
            }

            // The cover takes whatever height is left, so short screens never push the controls off.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val coverSize = minOf(maxWidth.value, maxHeight.value - 32f, 400f).coerceAtLeast(96f).dp
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = coverScale
                            scaleY = coverScale
                        }
                        .shadow(32.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black, spotColor = Color.Black)
                ) {
                    CoverImage(art = art, size = coverSize, corner = 12.dp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = OfflinePalette.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = song.artist.ifBlank { stringResource(R.string.offline_unknown_artist) },
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(12.dp))
                DownloadBadge(size = 22.dp)
            }
            Spacer(Modifier.height(14.dp))
            SeekBar(
                positionMs = shownPosition,
                durationMs = durationMs,
                scrubbing = scrubbing,
                onScrub = onScrub,
                onScrubFinished = onScrubFinished
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatPlaybackTime(shownPosition),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "-" + formatPlaybackTime((durationMs - shownPosition).coerceAtLeast(0)),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .bouncyClickable(onClick = onShuffle)
                ) {
                    Icon(
                        imageVector = OfflineIcons.Shuffle,
                        contentDescription = stringResource(R.string.offline_lib_desc_shuffle),
                        tint = if (shuffleOn) OfflinePalette.Green else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(26.dp)
                    )
                    if (shuffleOn) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 2.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(OfflinePalette.Green)
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .bouncyClickable(onClick = onPrev)
                ) {
                    Icon(
                        imageVector = OfflineIcons.Previous,
                        contentDescription = stringResource(R.string.offline_desc_previous),
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .bouncyClickable(onClick = onTogglePlay)
                        .shadow(12.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = if (isPlaying) OfflineIcons.Pause else OfflineIcons.Play,
                        contentDescription = if (isPlaying) {
                            stringResource(R.string.offline_desc_pause)
                        } else {
                            stringResource(R.string.offline_desc_play)
                        },
                        tint = Color.Black,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .bouncyClickable(onClick = onNext)
                ) {
                    Icon(
                        imageVector = OfflineIcons.Next,
                        contentDescription = stringResource(R.string.offline_desc_next),
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                // Keeps the transport controls centered (Spotify has repeat here).
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

internal fun formatPlaybackTime(ms: Int): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

// ---------------------------------------------------------------------------------------------
// Options sheet
// ---------------------------------------------------------------------------------------------

internal class SheetAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OptionsSheet(
    title: String,
    subtitle: String,
    artwork: @Composable () -> Unit,
    actions: List<SheetAction>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OfflinePalette.Sheet,
        contentColor = OfflinePalette.TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.28f)) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            artwork()
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = OfflinePalette.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = OfflinePalette.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        actions.forEach { action ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismiss()
                            action.onClick()
                        }
                    }
                    .padding(horizontal = 22.dp, vertical = 15.dp)
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = OfflinePalette.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(18.dp))
                Text(text = action.label, color = OfflinePalette.TextPrimary, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Previews (fake data, nothing on disk)
// ---------------------------------------------------------------------------------------------

private fun previewLibrary(): OfflineLibrary {
    fun song(id: String, title: String, artist: String, album: String, collection: String) =
        OfflineSong(id = id, title = title, artist = artist, uri = Uri.EMPTY, album = album, collection = collection)

    val songs = listOf(
        song("1", "Midnight City", "M83", "Hurry Up, We're Dreaming", "Night Drive"),
        song("2", "Nightcall", "Kavinsky", "OutRun", "Night Drive"),
        song("3", "Blinding Lights", "The Weeknd", "After Hours", "After Hours"),
    )
    val manifests = listOf(
        com.project.lol.offline.CollectionManifest(
            name = "Night Drive",
            kind = com.project.lol.offline.CollectionManifest.KIND_PLAYLIST,
            tracks = listOf(
                com.project.lol.offline.CollectionTrack("1", "Midnight City", "M83"),
                com.project.lol.offline.CollectionTrack("9", "Resonance", "HOME"),
                com.project.lol.offline.CollectionTrack("2", "Nightcall", "Kavinsky"),
            ),
        )
    )
    return buildOfflineLibrary(songs, manifests, emptyMap())
}

@Preview(showBackground = true, backgroundColor = 0xFF121212, widthDp = 380, heightDp = 760)
@Composable
private fun LibraryPagePreview() {
    LibraryPage(
        library = previewLibrary(),
        listState = remember { LazyListState() },
        filter = null,
        onFilterChange = {},
        searchOpen = false,
        onSearchOpenChange = {},
        searchQuery = "",
        onSearchQueryChange = {},
        onSearchFocusChange = {},
        songResults = null,
        currentSongId = "2",
        isPlaying = true,
        playingSourceKey = "c:Night Drive",
        bottomPadding = 96.dp,
        versionLabel = "SpotiOS v2.8.0",
        onOpenItem = {},
        onItemMenu = {},
        onPlaySong = {},
        onSongMenu = {},
        onSettings = {},
        onGoOnline = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF121212, widthDp = 380, heightDp = 760)
@Composable
private fun CollectionPagePreview() {
    val item = previewLibrary().item("c:Night Drive") ?: return
    CollectionPage(
        item = item,
        background = OfflinePalette.Background,
        currentSongId = "1",
        isPlaying = true,
        isCurrentSource = true,
        shuffleOn = false,
        bottomPadding = 96.dp,
        onBack = {},
        onPlay = {},
        onShuffle = {},
        onMore = {},
        onTrackClick = {},
        onTrackMenu = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF121212, widthDp = 380)
@Composable
private fun MiniPlayerPreview() {
    MiniPlayer(
        song = OfflineSong(id = "1", title = "Midnight City", artist = "M83", uri = Uri.EMPTY),
        isPlaying = true,
        progress = { 0.4f },
        onOpen = {},
        onTogglePlay = {},
        onNext = {},
    )
}
