package br.gov.caninde.transparencia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.gov.caninde.transparencia.data.TransparenciaViewModel
import br.gov.caninde.transparencia.data.WebSocketEndpoint
import br.gov.caninde.transparencia.data.createAppModule
import br.gov.caninde.transparencia.platform.initExternalLinks
import br.gov.caninde.transparencia.presentation.TransparenciaApp
import org.koin.android.ext.android.inject
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initExternalLinks(this)

        if (!Koin.isStarted()) {
            val endpoint = WebSocketEndpoint(
                scheme = BuildConfig.WS_SCHEME,
                host = BuildConfig.WS_HOST,
                port = BuildConfig.WS_PORT,
                authToken = BuildConfig.WS_AUTH_TOKEN,
            )
            startKoin { modules(createAppModule(endpoint)) }
            Koin.setStarted()
        }

        setContent {
            val viewModel: TransparenciaViewModel by inject()
            TransparenciaApp(viewModel)
        }
    }
}

object Koin {
    private var started = false

    fun isStarted(): Boolean = started

    fun setStarted() {
        started = true
    }
}
