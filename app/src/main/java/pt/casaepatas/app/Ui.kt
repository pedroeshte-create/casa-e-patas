package pt.casaepatas.app

import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

val Ink = Color(0xFF14302A)
val Bg = Color(0xFFF2F5F1)
val Muted = Color(0xFF566B65)
val Line = Color(0xFFDDE5DF)

val Primaries = listOf(
    "Verde" to 0xFF1E6B58, "Azul" to 0xFF2F5FA8, "Roxo" to 0xFF6B4BA8,
    "Laranja" to 0xFFB4531A, "Rosa" to 0xFFB03A6F,
)
val Accents = listOf(
    "Âmbar" to 0xFFF2A33A, "Coral" to 0xFFF28C6B, "Lilás" to 0xFFB9A3F0, "Azul-céu" to 0xFF7DB8F0,
)

@Composable
fun AppTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val primary = Color(Primaries[settings.primary.coerceIn(Primaries.indices)].second)
    val accent = Color(Accents[settings.accent.coerceIn(Accents.indices)].second)
    val scheme = lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = lerp(Color.White, primary, 0.14f),
        onPrimaryContainer = Ink,
        secondary = accent,
        onSecondary = Ink,
        secondaryContainer = lerp(Color.White, accent, 0.35f),
        onSecondaryContainer = Ink,
        background = Bg,
        onBackground = Ink,
        surface = Color.White,
        onSurface = Ink,
        surfaceVariant = Color(0xFFE6ECE8),
        onSurfaceVariant = Muted,
        outline = Color(0xFFA9BBB2),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val m = if (onClick != null) modifier.clip(shape).clickable(onClick = onClick) else modifier
    Surface(modifier = m, shape = shape, color = Color.White, border = BorderStroke(1.dp, Line)) {
        Column(content = content)
    }
}

@Composable
fun Avatar(label: String, color: Color, size: Dp, textColor: Color = Color.White) {
    Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(
            label.trim().take(1).uppercase(), color = textColor,
            fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp,
        )
    }
}

@Composable
fun PersonAvatar(p: Person?, size: Dp = 32.dp) {
    Avatar(p?.name ?: "?", Color(p?.color ?: 0xFF566B65), size)
}

@Composable
fun PetAvatar(pet: Pet, size: Dp) {
    val bmp = remember(pet.photoPath) { pet.photoPath?.let { loadBitmap(it, 384) } }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(), contentDescription = pet.name,
            modifier = Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop,
        )
    } else {
        val (bg, fg) = when (pet.type) {
            PetType.CAT -> Color(0xFFDDE8F7) to Color(0xFF234A7E)
            PetType.DOG -> Color(0xFFFDEBCB) to Color(0xFF7A4A00)
            PetType.OTHER -> Color(0xFFE8E0F5) to Color(0xFF4A2F80)
        }
        Avatar(pet.name, bg, size, fg)
    }
}

@Composable
fun Pill(text: String, bg: Color, fg: Color) {
    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = modifier.padding(top = 16.dp, bottom = 8.dp))
}

// ---------- rótulos ----------

fun Kind.label() = if (this == Kind.HOME) "Casa" else "Animal"

fun Repeat.label() = when (this) {
    Repeat.NONE -> "Uma vez"
    Repeat.DAILY -> "Todos os dias"
    Repeat.WEEKLY -> "Todas as semanas"
}

/** Dia da semana (Calendar.DAY_OF_WEEK) -> rótulo, por ordem Seg..Dom. */
val WeekdayLabels = listOf(2 to "Seg", 3 to "Ter", 4 to "Qua", 5 to "Qui", 6 to "Sex", 7 to "Sáb", 1 to "Dom")

fun weekdayName(d: Int): String = WeekdayLabels.firstOrNull { it.first == d }?.second ?: ""

// ---------- fotos ----------

fun loadBitmap(path: String, maxSize: Int): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / sample > maxSize * 2 || bounds.outHeight / sample > maxSize * 2) sample *= 2
    val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    val deg = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
        6 -> 90f
        3 -> 180f
        8 -> 270f
        else -> 0f
    }
    if (bmp != null && deg != 0f) {
        Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true)
    } else {
        bmp
    }
} catch (e: Exception) {
    null
}

/** Copia a foto escolhida para a pasta privada da app (o acesso ao URI original é temporário). */
fun copyPhoto(ctx: Context, uri: Uri): String? = try {
    val dir = File(ctx.filesDir, "pets").apply { mkdirs() }
    val f = File(dir, "pet_${System.currentTimeMillis()}.jpg")
    ctx.contentResolver.openInputStream(uri)?.use { input -> f.outputStream().use { out -> input.copyTo(out) } }
    f.absolutePath
} catch (e: Exception) {
    null
}

fun pickTime(ctx: Context, initial: String, onPicked: (String) -> Unit) {
    val parts = initial.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 19
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    TimePickerDialog(ctx, { _, hh, mm -> onPicked("%02d:%02d".format(hh, mm)) }, h, m, true).show()
}
