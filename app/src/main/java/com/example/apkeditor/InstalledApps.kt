package com.example.apkeditor

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

object InstalledApps {

    data class App(
        val label: String,
        val packageName: String,
        val isSystem: Boolean,
        val icon: ImageBitmap,
    )

    fun load(pm: PackageManager): List<App> =
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { info ->
                App(
                    label = pm.getApplicationLabel(info).toString(),
                    packageName = info.packageName,
                    isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    icon = pm.getApplicationIcon(info)
                        .toBitmap(96, 96, Bitmap.Config.ARGB_8888)
                        .asImageBitmap(),
                )
            }
            .sortedBy { it.label.lowercase() }
}
