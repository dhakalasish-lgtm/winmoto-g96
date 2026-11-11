package com.vikas.win10launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class AppItem(val name: String, val packageName: String, val icon: Drawable)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Windows10Launcher()
        }
    }
}

@Composable
fun Windows10Launcher() {
    val context = LocalContext.current
    var isStartMenuOpen by remember { mutableStateOf(false) }
    var appList by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != context.packageName }
                .map {
                    AppItem(
                        name = it.loadLabel(pm).toString(),
                        packageName = it.activityInfo.packageName,
                        icon = it.loadIcon(pm)
                    )
                }
                .sortedBy { it.name.lowercase() }
            appList = apps
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
            currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(cal.time)
            kotlinx.coroutines.delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF004E8C)) // Classic Windows 10 Default Blue Wallpaper
    ) {
        // Desktop Area (tapping closes start menu)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 52.dp)
                .clickable { isStartMenuOpen = false }
        ) {
            // Windows 10 Logo Branding
            Icon(
                imageVector = Icons.Default.Window,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.Center)
            )
        }

        // Start Menu Pop-up
        if (isStartMenuOpen) {
            StartMenu(
                apps = appList,
                context = context,
                onClose = { isStartMenuOpen = false },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 52.dp)
            )
        }

        // Taskbar
        Taskbar(
            currentTime = currentTime,
            currentDate = currentDate,
            onStartToggle = { isStartMenuOpen = !isStartMenuOpen },
            onOpenSettings = { openSettings(context) },
            onOpenChrome = { openChrome(context) },
            onOpenPlayStore = { openPlayStore(context) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun Taskbar(
    currentTime: String,
    currentDate: String,
    onStartToggle: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenChrome: () -> Unit,
    onOpenPlayStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color(0xEE101010))
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Start Button
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(52.dp)
                .clickable { onStartToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Window,
                contentDescription = "Start",
                tint = Color(0xFF00ADEF),
                modifier = Modifier.size(24.dp)
            )
        }

        // Search Bar Replica
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .height(34.dp)
                .width(140.dp)
                .background(Color(0xFF2B2B2B)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.LightGray,
                modifier = Modifier.padding(horizontal = 6.dp).size(18.dp)
            )
            Text("Type to search", color = Color.Gray, fontSize = 11.sp)
        }

        // Pinned Quick App Icons
        TaskbarIconButton(Icons.Default.Settings, "Settings", onOpenSettings)
        TaskbarIconButton(Icons.Default.Public, "Chrome", onOpenChrome)
        TaskbarIconButton(Icons.Default.ShoppingBag, "Store", onOpenPlayStore)

        Spacer(modifier = Modifier.weight(1f))

        // System Tray (Time & Date)
        Column(
            modifier = Modifier
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(currentTime, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(currentDate, color = Color.LightGray, fontSize = 9.sp)
        }
    }
}

@Composable
fun TaskbarIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(44.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun StartMenu(
    apps: List<AppItem>,
    context: Context,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .fillMaxHeight(0.68f)
            .background(Color(0xF01A1A1A))
    ) {
        // Left Action Strip
        Column(
            modifier = Modifier
                .width(44.dp)
                .fillMaxHeight()
                .background(Color(0xFF111111)),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = { openSettings(context); onClose() }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
            IconButton(onClick = { openPlayStore(context); onClose() }) {
                Icon(Icons.Default.ShoppingBag, contentDescription = "Store", tint = Color.White)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Middle App List
        LazyColumn(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .padding(horizontal = 6.dp)
        ) {
            item {
                Text(
                    "All Applications",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            items(apps) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            launchApp(context, app.packageName)
                            onClose()
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bmp = drawableToBitmap(app.icon)
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = app.name,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        app.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Right Windows 10 Live Tiles
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Explore", color = Color.LightGray, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                LiveTile(
                    title = "Chrome",
                    icon = Icons.Default.Public,
                    color = Color(0xFFD83B01),
                    modifier = Modifier.weight(1f).height(65.dp)
                ) {
                    openChrome(context)
                    onClose()
                }
                LiveTile(
                    title = "Play Store",
                    icon = Icons.Default.ShoppingBag,
                    color = Color(0xFF0078D7),
                    modifier = Modifier.weight(1f).height(65.dp)
                ) {
                    openPlayStore(context)
                    onClose()
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                LiveTile(
                    title = "Settings",
                    icon = Icons.Default.Settings,
                    color = Color(0xFF107C41),
                    modifier = Modifier.weight(1f).height(65.dp)
                ) {
                    openSettings(context)
                    onClose()
                }
                LiveTile(
                    title = "Camera",
                    icon = Icons.Default.PhotoCamera,
                    color = Color(0xFF5C2D91),
                    modifier = Modifier.weight(1f).height(65.dp)
                ) {
                    val cameraIntent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(cameraIntent)
                    onClose()
                }
            }
        }
    }
}

@Composable
fun LiveTile(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(color)
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(24.dp).align(Alignment.Center)
        )
        Text(
            text = title,
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

// Helpers
fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.let {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(it)
    }
}

fun openSettings(context: Context) {
    val intent = Intent(Settings.ACTION_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

fun openChrome(context: Context) {
    val pm = context.packageManager
    val intent = pm.getLaunchIntentForPackage("com.android.chrome") ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

fun openPlayStore(context: Context) {
    val pm = context.packageManager
    val intent = pm.getLaunchIntentForPackage("com.android.vending") ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=")).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 48
    val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 48
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bmp
}
