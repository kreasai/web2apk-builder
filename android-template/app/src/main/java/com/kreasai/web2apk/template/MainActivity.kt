package com.kreasai.web2apk.template

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var webView: WebView
    private lateinit var offlineView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        webView = findViewById(R.id.webView)
        offlineView = findViewById(R.id.offlineView)

        setupWebView()
        setupSwipeRefresh()
        setupOfflineRetry()
        setupBackNavigation()
        requestConfiguredPermissions()

        if (BuildConfig.ENABLE_SPLASH) {
            window.decorView.setBackgroundColor(parseColorOrDefault(BuildConfig.SPLASH_BACKGROUND_COLOR))
            Handler(Looper.getMainLooper()).postDelayed({
                loadInitialUrl()
            }, 1400)
        } else {
            loadInitialUrl()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            mediaPlaybackRequiresUserGesture = false
            loadsImagesAutomatically = true
            setSupportZoom(false)
            databaseEnabled = true
            setGeolocationEnabled(true)
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString().orEmpty()
                if (!BuildConfig.ENABLE_EXTERNAL_APPS) {
                    return false
                }

                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://")) {
                    return false
                }

                return openExternalApp(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefreshLayout.isRefreshing = false
                offlineView.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true && BuildConfig.ENABLE_OFFLINE_PAGE) {
                    swipeRefreshLayout.isRefreshing = false
                    webView.visibility = View.GONE
                    offlineView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.isEnabled = BuildConfig.ENABLE_SWIPE_REFRESH
        swipeRefreshLayout.setOnRefreshListener {
            if (BuildConfig.ENABLE_SWIPE_REFRESH) {
                webView.reload()
            } else {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupOfflineRetry() {
        offlineView.setOnClickListener {
            offlineView.visibility = View.GONE
            webView.visibility = View.VISIBLE
            loadInitialUrl()
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (BuildConfig.ENABLE_BACK_NAVIGATION && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun loadInitialUrl() {
        val initialUrl = BuildConfig.WEB_URL.ifBlank { "https://example.com" }
        webView.loadUrl(initialUrl)
    }

    private fun requestConfiguredPermissions() {
        val requested = BuildConfig.PERMISSIONS_CSV
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }

        if (requested.isEmpty()) {
            return
        }

        val permissions = mutableListOf<String>()
        requested.forEach { key ->
            when (key) {
                "camera" -> permissions.add(Manifest.permission.CAMERA)
                "location" -> {
                    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                "microphone" -> permissions.add(Manifest.permission.RECORD_AUDIO)
                "storage" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                        permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                    } else {
                        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                "notifications" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }

        val missing = permissions.distinct().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1010)
        }
    }

    private fun openExternalApp(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun parseColorOrDefault(raw: String): Int {
        return try {
            Color.parseColor(raw)
        } catch (_: IllegalArgumentException) {
            Color.parseColor("#111827")
        }
    }
}
