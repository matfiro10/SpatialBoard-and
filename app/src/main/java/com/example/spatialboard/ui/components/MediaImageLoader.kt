package com.example.spatialboard.ui.components

import android.content.Context
import coil3.ImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.request.allowHardware

fun createSpatialImageLoader(context: Context): ImageLoader {
    val appContext = context.applicationContext
    return ImageLoader.Builder(appContext)
        .allowHardware(false)
        .components {
            add(AnimatedImageDecoder.Factory())
        }
        .build()
}