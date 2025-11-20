// Certifique-se que o pacote está correto
package br.ufpr.vibetrack.vibetrackwearos.presentation

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Importe os modelos do pacote que você ACABOU DE CRIAR
import br.ufpr.vibetrack.vibetrackwearos.data.model.HealthData
import br.ufpr.vibetrack.vibetrackwearos.data.model.HeartRate
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Mude para AndroidViewModel para ter acesso ao Contexto
class WearViewModel(private val app: Application) : AndroidViewModel(app), SensorEventListener {

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring = _isMonitoring.asStateFlow()

    private val _liveHeartRate = MutableStateFlow(0)
    val liveHeartRate = _liveHeartRate.asStateFlow()

    private val _liveSteps = MutableStateFlow(0)
    val liveSteps = _liveSteps.asStateFlow()

    private val _statusText = MutableStateFlow("Pronto para iniciar")
    val statusText = _statusText.asStateFlow()

    private lateinit var sensorManager: SensorManager
    private var hrSensor: Sensor? = null
    private var stepSensor: Sensor? = null

    // Listas para calcular média, min, max
    private val heartRateReadings = mutableListOf<Int>()
    private var initialSteps = 0

    // O path que o seu DataLayerListenerService no celular está ouvindo
    private val EXPERIMENT_DATA_PATH = "/experiment-data"

    private val PERMISSIONS = arrayOf(
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION
    )

    fun requestPermissions(activity: Activity) {
        if (!hasPermissions(activity)) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, 0)
        }
    }

    private fun hasPermissions(context: Context): Boolean {
        return PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun startMonitoring() {
        val context = app
        if (!hasPermissions(context)) {
            _statusText.value = "Permissões negadas."
            return
        }

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        hrSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (hrSensor == null) {
            _statusText.value = "Sensor de FC não disponível."
            Log.w("WearViewModel", "Sensor de Frequência Cardíaca não encontrado.")
            // Mesmo sem sensor de FC, tentamos o de passos
        }
        if (stepSensor == null) {
            _statusText.value = "Sensor de Passos não disponível."
            Log.w("WearViewModel", "Sensor de Passos não encontrado.")
        }
        if (hrSensor == null && stepSensor == null) {
            _statusText.value = "Nenhum sensor disponível."
            return
        }


        // Reseta os dados da sessão
        heartRateReadings.clear()
        initialSteps = 0 // Será pego no primeiro evento
        _liveSteps.value = 0
        _liveHeartRate.value = 0

        // Registra os listeners
        hrSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        _isMonitoring.value = true
        _statusText.value = "Monitorando..."
    }

    fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        _isMonitoring.value = false
        _statusText.value = "Coleta parada. Gerando dados..."

        // Processa e envia os dados
        processAndSendData()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!_isMonitoring.value || event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                val hr = event.values[0].toInt()
                if (hr > 0) { // Ignora leituras 0
                    _liveHeartRate.value = hr
                    heartRateReadings.add(hr)
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val currentSteps = event.values[0].toInt()
                if (initialSteps == 0) {
                    initialSteps = currentSteps // Pega a contagem inicial
                }
                // Garante que os passos nunca sejam negativos se o sensor reiniciar
                _liveSteps.value = (currentSteps - initialSteps).coerceAtLeast(0)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Não é necessário para este TCC
    }

    private fun processAndSendData() {
        val stepsCount = _liveSteps.value
        val hrAvg: Int
        val hrMax: Int
        val hrResting: Int // "Resting" no seu modelo

        if (heartRateReadings.isEmpty()) {
            // Se não houver dados de FC, envie 0
            hrAvg = 0
            hrMax = 0
            hrResting = 0
            _statusText.value = "Nenhum dado de FC coletado. Enviando passos..."
        } else {
            hrAvg = heartRateReadings.average().toInt()
            hrMax = heartRateReadings.maxOrNull() ?: 0
            // Usando o Mínimo como "Repouso" para simplificar
            hrResting = heartRateReadings.minOrNull() ?: 0
            _statusText.value = "Dados de FC processados."
        }

        // 2. Monta o objeto HealthData
        // Note o construtor do HeartRate (resting, average, max)
        val heartRateData = HeartRate(hrResting, hrAvg, hrMax)
        val healthData = HealthData(stepsCount, heartRateData)

        // 3. Serializa para JSON
        val gson = Gson()
        val jsonPayload = gson.toJson(healthData)

        // 4. Envia para o Mobile
        viewModelScope.launch {
            sendDataToMobile(jsonPayload)
        }
    }

    private suspend fun sendDataToMobile(json: String) {
        _statusText.value = "Enviando para o celular..."
        Log.d("WearViewModel", "Enviando JSON: $json")
        val context = app

        try {
            // Acessa a API de Nós (Nodes) para encontrar o celular
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            val phoneNode = nodes.firstOrNull() // Pega o primeiro celular conectado

            Log.d("WearViewModel", "Nós conectados: ${nodes.map{it.id + ':' + it.displayName}}")
            Log.d("WearViewModel", "Celular conectado: ${phoneNode?.id + ':' + phoneNode?.displayName}")

//            nodes.forEach { node ->
//                Wearable.getMessageClient(context).sendMessage(node.id, "/experiment-data", "hello".toByteArray()).addOnSuccessListener {
//                    Log.d("WearTest", "mensagem enviada para ${node.displayName}")
//                }.addOnFailureListener { e ->
//                    Log.e("WearTest", "falha enviar", e)
//                }
//            }


            if (phoneNode == null) {
                _statusText.value = "Falha: Celular não conectado."
                Log.w("WearViewModel", "Nenhum nó de celular conectado.")
                return
            }

            // Envia a mensagem!
            Wearable.getMessageClient(context).sendMessage(
                phoneNode.id,
                EXPERIMENT_DATA_PATH, // Este é o PATH que o DataLayerListenerService espera!
                json.toByteArray(Charsets.UTF_8)
            ).await() // Espera a chamada ser concluída

            _statusText.value = "Dados enviados com sucesso!"
            Log.d("WearViewModel", "Mensagem enviada para ${phoneNode.displayName}")

        } catch (e: Exception) {
            _statusText.value = "Falha ao enviar dados."
            Log.e("WearViewModel", "Erro ao enviar mensagem", e)
        }
    }


}