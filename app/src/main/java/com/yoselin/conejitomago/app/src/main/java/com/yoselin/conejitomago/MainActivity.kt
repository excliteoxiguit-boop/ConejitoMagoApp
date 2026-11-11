package com.yoselin.conejitomago

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.*
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Permiso de cámara para reconocimiento facial
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF1E1B2E)
            ) {
                MascotaMagicaPantallaCompleta()
            }
        }
    }
}

@Composable
fun MascotaMagicaPantallaCompleta() {
    val context = LocalContext.current
    var videoActualResId by remember { mutableStateOf(R.raw.conejito_idle) }
    var textoMensaje by remember { mutableStateOf("¡Dime 'Magic' seguido de tu pregunta o usa los botones!") }

    val faceDetectorOptions = remember {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    }
    val faceDetector = remember { FaceDetection.getClient(faceDetectorOptions) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val textoHablado = results?.get(0)?.lowercase(Locale.getDefault()) ?: ""
            
            if (textoHablado.isNotEmpty()) {
                if (textoHablado.contains("magic")) {
                    val preguntaReal = textoHablado.substringAfter("magic").trim()
                    videoActualResId = R.raw.conejito_hablando
                    
                    if (preguntaReal.isNotEmpty()) {
                        textoMensaje = "✨ Magic responde a '$preguntaReal': Las cartas del tarot auguran éxito."
                    } else {
                        textoMensaje = "✨ ¡Dime, Yutziri! ¿Qué deseas consultarme?"
                    }
                } else {
                    videoActualResId = R.raw.conejito_idle
                    textoMensaje = "💤 (No escuché tu llamado)... Di 'Magic' seguido de tu pregunta."
                }
            }
        }
    }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    val ejeX = event.values[0]
                    val ejeY = event.values[1]
                    val ejeZ = event.values[2]

                    val fuerzaShake = sqrt((ejeX * ejeX + ejeY * ejeY + ejeZ * ejeZ).toDouble()) - SensorManager.GRAVITY_EARTH

                    if (fuerzaShake > 4.5) {
                        videoActualResId = R.raw.conejito_feliz
                        textoMensaje = "✨ ¡Whoa! ¡Me agitas con alegría!"
                    } else if (ejeX > 6.0f) {
                        videoActualResId = R.raw.conejito_triste
                        textoMensaje = "💧 ¡Ay, me inclino mucho y me pongo triste!"
                    } else if (ejeX < -6.0f) {
                        videoActualResId = R.raw.conejito_enojado
                        textoMensaje = "💢 ¡Hey! ¡No me inclines tan brusco!"
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    // Usamos un Box para superponer los elementos encima del video de fondo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                videoActualResId = R.raw.conejito_hablando
                textoMensaje = "💬 ¡Hola Yutziri! Llámame 'Magic' con el micrófono."
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. VIDEO DE FONDO A PANTALLA COMPLETA
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    val uri = Uri.parse("android.resource://${ctx.packageName}/$videoActualResId")
                    setVideoURI(uri)
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = true
                        // Escalar para que cubra toda la pantalla de forma fluida
                        mediaPlayer.setVideoScalingSize(width, height)
                        start()
                    }
                }
            },
            update = { videoView ->
                val nuevaUri = Uri.parse("android.resource://${context.packageName}/$videoActualResId")
                videoView.setVideoURI(nuevaUri)
                videoView.start()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Capa semitransparente opcional para que los botones resalten bien sobre el video
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        // 2. INTERFAZ Y BOTONES FLOTANDO ENCIMA DEL VIDEO
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Título superior
            Text(
                text = "🐰 Conejito Mago AI 🐰",
                style = MaterialTheme.typography.h6,
                color = Color(0xFFE2D9F3)
            )

            // Espacio central libre para lucir al conejito en pantalla completa
            Spacer(modifier = Modifier.weight(1f))

            // Controles flotantes en la parte inferior
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botones principales (Voz y Spotify)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Di 'Magic' seguido de tu pregunta...")
                            }
                            speechRecognizerLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF5B3B8C))
                    ) {
                        Text("🎤 Preguntar / Tarot", color = Color.White)
                    }

                    Button(
                        onClick = {
                            val spotifyIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                            if (spotifyIntent != null) {
                                context.startActivity(spotifyIntent)
                                textoMensaje = "🎵 Abriendo Spotify..."
                            } else {
                                textoMensaje = "⚠️ Spotify no está instalado."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1DB954))
                    ) {
                        Text("🎧 Spotify", color = Color.White)
                    }
                }

                // Botones de control manual de emociones
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            videoActualResId = R.raw.conejito_feliz
                            textoMensaje = "🍎 ¡Feliz!"
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF7B52AB))
                    ) {
                        Text("🍎 Feliz", color = Color.White)
                    }

                    Button(
                        onClick = {
                            videoActualResId = R.raw.conejito_idle
                            textoMensaje = "💤 Descansando..."
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4A3B69))
                    ) {
                        Text("💤 Descansar", color = Color.White)
                    }

                    Button(
                        onClick = {
                            videoActualResId = R.raw.conejito_triste
                            textoMensaje = "💧 Triste..."
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3B5686))
                    ) {
                        Text("💧 Triste", color = Color.White)
                    }

                    Button(
                        onClick = {
                            videoActualResId = R.raw.conejito_enojado
                            textoMensaje = "💢 ¡Enojado!"
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF8B3A3A))
                    ) {
                        Text("💢 Enojado", color = Color.White)
                    }
                }

                // Tarjeta de mensajes flotante en la parte más baja
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xEE2D2A45) // Ligeramente transparente
                ) {
                    Text(
                        text = textoMensaje,
                        modifier = Modifier.padding(12.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }
}