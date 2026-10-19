package com.example.personalfitnessapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.personalfitnessapp.ui.theme.AppAvatarDefault
import com.example.personalfitnessapp.ui.theme.AppBackground
import com.example.personalfitnessapp.ui.theme.AppDanger
import com.example.personalfitnessapp.ui.theme.AppDivider
import com.example.personalfitnessapp.ui.theme.AppPrimary
import com.example.personalfitnessapp.ui.theme.AppProBadgeBg
import com.example.personalfitnessapp.ui.theme.AppSurface
import com.example.personalfitnessapp.ui.theme.AppSurfaceHigh
import com.example.personalfitnessapp.ui.theme.AppTextPrimary
import com.example.personalfitnessapp.ui.theme.AppTextSecondary
import com.example.personalfitnessapp.ui.theme.AppTopBar

/** Back arrow / Cancel · centered title · trailing action, on the `topBar` color (img 37, 03, 21). */
@Composable
fun HevyTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    titleColor: Color = AppTextPrimary,
    bottom: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().background(AppTopBar).statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(60.dp)) {
            Row(
                Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { navigation?.invoke() }
            Text(
                title,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 96.dp),
                fontSize = 20.sp,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { actions?.invoke(this) }
        }
        bottom?.invoke()
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AppTextPrimary)
    }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, enabled: Boolean = true, color: Color = AppPrimary) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(text, color = if (enabled) color else AppTextSecondary, fontSize = 17.sp)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingPlus: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false,
    compact: Boolean = false,
    containerColor: Color = AppPrimary,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = if (compact) 36.dp else 44.dp),
        shape = RoundedCornerShape(if (compact) 8.dp else 10.dp),
        contentPadding = if (compact) PaddingValues(horizontal = 14.dp) else ButtonDefaults.ContentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White,
            disabledContainerColor = containerColor.copy(alpha = 0.45f),
            disabledContentColor = Color.White.copy(alpha = 0.7f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        } else if (leadingPlus) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = if (compact) 16.sp else 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = AppTextPrimary,
    leadingIcon: ImageVector? = null,
    alignStart: Boolean = false,
    height: Dp = 44.dp,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = height),
        shape = RoundedCornerShape(10.dp),
        color = AppSurface,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (alignStart) Arrangement.Start else Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = textColor, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, color = textColor, fontSize = 17.sp, maxLines = 1)
        }
    }
}

/** Selected = primary fill, unselected = surface fill (Duration / Volume / Reps chips). */
@Composable
fun HevyFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) AppPrimary else AppSurface,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.White,
            fontSize = 15.sp,
        )
    }
}

/** Grey label over a value ("Duration" / "34s"). */
@Composable
fun StatColumn(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = AppTextPrimary) {
    Column(modifier) {
        Text(label, color = AppTextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = valueColor, fontSize = 18.sp, maxLines = 1)
    }
}

@Composable
fun HevyDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = 1.dp, color = AppDivider)
}

/** Full-bleed `surface` strip that separates feed cards and sections. */
@Composable
fun SectionStrip(text: String? = null, height: Dp = 16.dp) {
    Box(
        Modifier.fillMaxWidth().background(AppSurface).heightIn(min = height),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (text != null) {
            Text(text, Modifier.padding(16.dp, 12.dp), color = AppTextSecondary, fontSize = 17.sp)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = AppTextSecondary, fontSize = 17.sp)
}

/** Settings-style row: title left, grey value + chevron right (img 37). */
@Composable
fun SettingsNavRow(
    title: String,
    value: String? = null,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 64.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), color = AppTextPrimary, fontSize = 18.sp)
        if (value != null) Text(value, color = AppTextSecondary, fontSize = 18.sp)
        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = AppTextSecondary)
        }
    }
}

/** Title + switch, description below in grey (img 38). */
@Composable
fun SettingsToggleRow(title: String, description: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), color = AppTextPrimary, fontSize = 18.sp)
            HevySwitch(checked, onCheckedChange)
        }
        if (description != null) {
            Spacer(Modifier.height(6.dp))
            Text(description, color = AppTextSecondary, fontSize = 16.sp)
        }
    }
}

@Composable
fun HevySwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = AppPrimary,
            checkedBorderColor = AppPrimary,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = AppSurfaceHigh,
            uncheckedBorderColor = AppSurfaceHigh,
        ),
    )
}

/** Leading outline icon, title + subtitle, trailing chevron (Statistics list, Profile rows). */
@Composable
fun NavListRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = AppTextPrimary, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = AppTextPrimary, fontSize = 18.sp)
            if (subtitle != null) Text(subtitle, color = AppTextSecondary, fontSize = 15.sp)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = AppTextSecondary)
    }
}

@Composable
fun LetterAvatar(name: String, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).background(AppAvatarDefault),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Circular exercise image on white (the dataset's photos have white backgrounds), or the
 * exercise's initial when there is no image.
 */
@Composable
fun ExerciseThumbnail(imageUrl: String?, name: String, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "?",
            color = AppBackground,
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape),
            )
        }
    }
}

/** `surface` tile with icon above (or beside) a label (Workout tab routine tiles, Profile dashboard). */
@Composable
fun DashboardTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    height: Dp = 56.dp,
) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = height), shape = RoundedCornerShape(12.dp), color = AppSurface) {
        if (centered) {
            Column(
                Modifier.padding(vertical = 18.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(icon, contentDescription = null, tint = AppTextPrimary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text(label, color = AppTextPrimary, fontSize = 17.sp, textAlign = TextAlign.Center)
            }
        } else {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = AppTextPrimary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(label, color = AppTextPrimary, fontSize = 17.sp)
            }
        }
    }
}

/** `Last 3 months ▾` in primary. */
@Composable
fun PeriodDropdownLabel(text: String, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = AppPrimary, fontSize = 16.sp)
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = AppPrimary)
    }
}

@Composable
fun ProBadgeLike(text: String) {
    Box(Modifier.clip(CircleShape).background(AppProBadgeBg).padding(horizontal = 10.dp, vertical = 2.dp)) {
        Text(text, color = AppBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Grouped `surfaceHigh` card of rows with dividers, as used inside every bottom sheet (img 10, 24, 34). */
@Composable
fun SheetGroup(content: @Composable () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        color = AppSurfaceHigh,
    ) { Column { content() } }
}

@Composable
fun SheetRow(
    text: String,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    textColor: Color = AppTextPrimary,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(Modifier.width(36.dp), contentAlignment = Alignment.CenterStart) { leading() }
            }
            Text(text, Modifier.weight(1f), color = textColor, fontSize = 17.sp)
            trailing?.invoke()
        }
        if (showDivider) HorizontalDivider(Modifier.padding(start = 16.dp), color = AppDivider.copy(alpha = 0.9f))
    }
}

@Composable
fun SheetIcon(icon: ImageVector, tint: Color = AppTextPrimary) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
}

/** Dashed-border placeholder box ("Add a photo / video"). */
@Composable
fun OutlinedCell(modifier: Modifier = Modifier, focused: Boolean = false, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) AppPrimary else AppSurfaceHigh),
                RoundedCornerShape(8.dp),
            ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

val DangerColor = AppDanger
