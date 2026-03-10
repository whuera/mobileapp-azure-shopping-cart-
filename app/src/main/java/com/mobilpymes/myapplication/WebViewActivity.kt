package com.mobilpymes.myapplication

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.mobilpymes.myapplication.databinding.ActivityWebviewBinding

class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebviewBinding

    companion object {
        private const val TARGET_URL = "https://webapp-azure-shopping-cart.vercel.app/login"
        private const val KEY_URL = "current_url"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        setupErrorView()
        setupBackNavigation()

        if (!isNetworkAvailable()) {
            showError("Sin conexión a internet")
            return
        }

        val urlToLoad = savedInstanceState?.getString(KEY_URL) ?: TARGET_URL
        binding.webView.loadUrl(urlToLoad)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView

        webView.settings.apply {
            // JavaScript habilitado — necesario para SPAs (React/Next.js)
            javaScriptEnabled = true

            // Almacenamiento para tokens / sesiones
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled   = true

            // Soporte multimedia
            mediaPlaybackRequiresUserGesture = false

            // Zoom
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false

            // Mejora renderizado
            loadWithOverviewMode = true
            useWideViewPort      = true

            // Cache
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }

        // Cookies habilitadas (login persistente)
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                binding.progressBar.visibility = View.VISIBLE
                binding.errorLayout.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView, url: String) {
                binding.progressBar.visibility = View.GONE
                binding.urlText.text = url
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                // Solo mostrar error si falla la URL principal (no recursos secundarios)
                if (request.isForMainFrame) {
                    binding.progressBar.visibility = View.GONE
                    showError("No se pudo cargar la página\n(${error.description})")
                }
            }

            // Mantener toda la navegación dentro del WebView
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                view.loadUrl(request.url.toString())
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                binding.progressBar.progress = newProgress
                if (newProgress == 100) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun setupErrorView() {
        binding.retryButton.setOnClickListener {
            binding.errorLayout.visibility = View.GONE
            binding.webView.reload()
        }
    }

    private fun showError(message: String) {
        binding.errorMessage.text = message
        binding.errorLayout.visibility = View.VISIBLE
        binding.webView.visibility    = View.GONE
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps    = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Navegar atrás dentro del WebView antes de salir de la Activity
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // Guardar la URL actual al rotar pantalla
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_URL, binding.webView.url ?: TARGET_URL)
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onDestroy() {
        binding.webView.apply {
            stopLoading()
            destroy()
        }
        super.onDestroy()
    }
}
