package com.pgclone.app

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var service: Intent
    private var isRunning = false

    private lateinit var btnToggle: Button
    private lateinit var etLat: EditText
    private lateinit var etLng: EditText
    private lateinit var tvStatus: TextView
    private lateinit var joystickView: JoystickView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        service = Intent(this, MockLocationService::class.java)

        btnToggle = findViewById(R.id.btnToggle)
        etLat = findViewById(R.id.etLat)
        etLng = findViewById(R.id.etLng)
        tvStatus = findViewById(R.id.tvStatus)
        joystickView = findViewById(R.id.joystick)

        // botão ligar/desligar spoof
        btnToggle.setOnClickListener {
            if (!isRunning) {
                startSpoof()
            } else {
                stopSpoof()
            }
        }

        // teleport por coordenadas
        findViewById<Button>(R.id.btnTeleport).setOnClickListener {
            val lat = etLat.text.toString().toDoubleOrNull()
            val lng = etLng.text.toString().toDoubleOrNull()
            if (lat != null && lng != null) {
                teleport(lat, lng)
            } else {
                Toast.makeText(this, "Coordenadas inválidas", Toast.LENGTH_SHORT).show()
            }
        }

        // joystick move a posição gradualmente
        joystickView.setOnMoveListener { angle, strength ->
            if (isRunning) moveByJoystick(angle, strength)
        }
    }

    private fun startSpoof() {
        startForegroundService(service)
        isRunning = true
        btnToggle.text = "Parar GPS"
        tvStatus.text = "Status: ativo"
    }

    private fun stopSpoof() {
        stopService(service)
        isRunning = false
        btnToggle.text = "Iniciar GPS"
        tvStatus.text = "Status: parado"
    }

    private fun teleport(lat: Double, lng: Double) {
        val intent = Intent(this, MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_UPDATE
            putExtra(MockLocationService.EXTRA_LAT, lat)
            putExtra(MockLocationService.EXTRA_LNG, lng)
            putExtra(MockLocationService.EXTRA_SPEED, 0f)
        }
        startService(intent)
        tvStatus.text = "Teleportado: $lat, $lng"
    }

    private fun moveByJoystick(angle: Int, strength: Int) {
        val step = 0.00005 * (strength / 100.0)
        val rad = Math.toRadians(angle.toDouble())
        val dLat = step * Math.cos(rad)
        val dLng = step * Math.sin(rad)

        val intent = Intent(this, MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_UPDATE
            putExtra(MockLocationService.EXTRA_LAT,
                (MockLocationService::class.java.getDeclaredField("currentLat")
                    .also { it.isAccessible = true }
                    .getDouble(null) + dLat))
            putExtra(MockLocationService.EXTRA_LNG,
                (MockLocationService::class.java.getDeclaredField("currentLng")
                    .also { it.isAccessible = true }
                    .getDouble(null) + dLng))
            putExtra(MockLocationService.EXTRA_SPEED, (strength * 0.05f))
        }
        startService(intent)
    }
}
