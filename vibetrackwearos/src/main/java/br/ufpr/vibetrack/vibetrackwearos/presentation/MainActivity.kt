// Certifique-se que o pacote está correto
package br.ufpr.vibetrack.vibetrackwearos.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels // Importação necessária .
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
// Importe o seu tema
import br.ufpr.vibetrack.vibetrackwearos.presentation.theme.VibeTrackWearOSTheme

class MainActivity : ComponentActivity() {

    // 1. Inicializa o ViewModel usando a delegação
    private val viewModel: WearViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Pede permissões assim que o app abre
        viewModel.requestPermissions(this)

        setContent {
            // Usa o seu Tema
            VibeTrackWearOSTheme {
                // 3. Passa o ViewModel para a sua UI
                WearApp(viewModel)
            }
        }
    }
}

@Composable
fun WearApp(viewModel: WearViewModel) {
    // 4. Observa as variáveis do ViewModel
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val heartRate by viewModel.liveHeartRate.collectAsState()
    val steps by viewModel.liveSteps.collectAsState()
    val statusText by viewModel.statusText.collectAsState()

    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        timeText = { TimeText(modifier = Modifier.padding(top = 6.dp)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Título (Conforme protótipo [cite: 7362, 7364])
            Text(
                text = "VibeTrack",
                style = MaterialTheme.typography.title3,
                color = Color(0xFFF0B90A) // Um amarelo similar ao do seu TCC
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 5. Botão que chama o ViewModel
            Button(
                onClick = {
                    if (isMonitoring) {
                        viewModel.stopMonitoring()
                    } else {
                        viewModel.startMonitoring()
                    }
                },
                // Muda a cor do botão baseado no estado
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isMonitoring) Color.Red else Color(0xFF00C853) // Verde
                )
            ) {
                Text(if (isMonitoring) "Parar Coleta" else "Iniciar")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Mostradores de dados ao vivo
            Text("FC: $heartRate bpm")
            Text("Passos: $steps")

            Spacer(modifier = Modifier.height(8.dp))

            // 7. Status (ex: "Monitorando...", "Enviando dados...")
            Text(
                text = statusText,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}