package com.grimm.kin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.IntentCompat
import com.grimm.kin.data.Library
import com.grimm.kin.data.Security
import com.grimm.kin.ui.KinApp
import com.grimm.kin.ui.KinTheme

class MainActivity : ComponentActivity() {
    private val incoming = mutableStateOf<List<Uri>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Library.init(this)
        Security.init(this)
        applySecureFlag()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            KinTheme {
                KinApp(incoming = incoming, onSecureChanged = { applySecureFlag() })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        Security.onForeground()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) Security.onBackground()
    }

    private fun applySecureFlag() {
        if (Security.secureScreen) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val uris: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND ->
                listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.toList()
                    ?: emptyList()
            else -> emptyList()
        }
        if (uris.isNotEmpty()) incoming.value = uris
    }
}
