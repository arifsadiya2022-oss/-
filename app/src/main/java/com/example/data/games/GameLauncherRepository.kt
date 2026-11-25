package com.example.data.games

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.model.InstalledGame
import com.example.data.model.LaunchableAppEntry

class GameLauncherRepository(private val context: Context) {

    companion object {
        const val FREE_FIRE_PACKAGE = "com.dts.freefireth"
        const val FREE_FIRE_MAX_PACKAGE = "com.dts.freefiremax"

        private val KNOWN_GAME_PACKAGES = setOf(
            FREE_FIRE_PACKAGE,
            FREE_FIRE_MAX_PACKAGE,
            "com.tencent.ig",
            "com.pubg.imobile",
            "com.activision.callofduty.shooter",
            "com.mobile.legends",
            "com.miHoYo.GenshinImpact",
            "com.supercell.brawlstars",
            "com.supercell.clashroyale",
            "com.supercell.clashofclans",
            "com.roblox.client",
            "com.mojang.minecraftpe",
            "com.innersloth.spacemafia",
            "com.ea.gp.fifamobile"
        )
    }

    fun loadGamesAndApps(customPinnedPackages: Set<String>): Pair<List<InstalledGame>, List<LaunchableAppEntry>> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }
        } catch (_: Exception) {
            emptyList()
        }

        val seenPackages = HashSet<String>()
        val detectedGames = mutableListOf<InstalledGame>()
        val allLaunchableApps = mutableListOf<LaunchableAppEntry>()

        for (resolveInfo in resolveInfos) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val pkg = activityInfo.packageName ?: continue
            if (pkg == context.packageName || !seenPackages.add(pkg)) continue

            val appInfo = activityInfo.applicationInfo
            val appName = resolveInfo.loadLabel(pm).toString().takeIf { it.isNotBlank() } ?: pkg
            val versionName = readVersionName(pm, pkg)
            val isSystemGame = isAndroidGame(appInfo) || pkg in KNOWN_GAME_PACKAGES
            val isFreeFire = pkg == FREE_FIRE_PACKAGE || pkg == FREE_FIRE_MAX_PACKAGE
            val isCustomPinned = pkg in customPinnedPackages

            val iconBitmap = try {
                resolveInfo.loadIcon(pm)?.toBoundedImageBitmap(96)
            } catch (_: Exception) {
                null
            }

            allLaunchableApps += LaunchableAppEntry(
                packageName = pkg,
                appName = appName,
                versionName = versionName,
                isSystemGame = isSystemGame,
                isPinned = isCustomPinned,
                iconBitmap = iconBitmap
            )

            if (isSystemGame || isCustomPinned) {
                detectedGames += InstalledGame(
                    packageName = pkg,
                    appName = appName,
                    versionName = versionName,
                    isGameCategory = isSystemGame,
                    isFreeFire = isFreeFire,
                    isCustomPinned = isCustomPinned,
                    iconBitmap = iconBitmap
                )
            }
        }

        val sortedGames = detectedGames.sortedWith(
            compareByDescending<InstalledGame> { it.isFreeFire }
                .thenByDescending { it.isCustomPinned }
                .thenBy { it.appName.lowercase() }
        )

        val sortedApps = allLaunchableApps.sortedWith(
            compareByDescending<LaunchableAppEntry> { it.isPinned }
                .thenByDescending { it.isSystemGame }
                .thenBy { it.appName.lowercase() }
        )

        return sortedGames to sortedApps
    }

    fun launchPackage(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isAndroidGame(appInfo: ApplicationInfo): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo.category == ApplicationInfo.CATEGORY_GAME) {
            return true
        }
        @Suppress("DEPRECATION")
        return (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
    }

    private fun readVersionName(pm: PackageManager, packageName: String): String {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            info.versionName?.takeIf { it.isNotBlank() } ?: "1.0"
        } catch (_: Exception) {
            "—"
        }
    }

    private fun Drawable.toBoundedImageBitmap(sizePx: Int): ImageBitmap {
        if (this is BitmapDrawable && bitmap != null && bitmap.width <= sizePx && bitmap.height <= sizePx) {
            return bitmap.asImageBitmap()
        }
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        setBounds(0, 0, sizePx, sizePx)
        draw(canvas)
        return bmp.asImageBitmap()
    }
}
