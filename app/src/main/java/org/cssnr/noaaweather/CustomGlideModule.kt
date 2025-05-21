package org.cssnr.noaaweather

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import okhttp3.OkHttpClient
import java.io.InputStream


@GlideModule
class CustomGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        Log.d("createRetrofit", "versionName: $versionName")
        val githubUrl = context.getString(R.string.github_url)
        val userAgent = "NOAAWeather Android/${versionName} - $githubUrl"

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().header("User-Agent", userAgent).build()
                chain.proceed(request)
            }
            .build()

        val factory = OkHttpUrlLoader.Factory(okHttpClient)
        registry.replace(GlideUrl::class.java, InputStream::class.java, factory)
    }
}
