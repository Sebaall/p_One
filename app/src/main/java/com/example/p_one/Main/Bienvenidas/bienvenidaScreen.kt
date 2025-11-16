package com.example.p_one.Main.Bienvenidas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.R
import com.example.p_one.MenuAlumno.mathQuiz

class bienvenidaScreen : AppCompatActivity() {

    private lateinit var tvBienvenida: TextView
    private lateinit var btnJugar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bienvenida_screen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvBienvenida = findViewById(R.id.tvBienvenida)
        btnJugar = findViewById(R.id.btnJugar)

        // 🔹 Recibe SIEMPRE el apodo desde ScreenApodo
        val apodoAlumno = intent.getStringExtra("apodoAlumno") ?: "Alumno"

        // 🔹 Mostrar SOLO el apodo
        tvBienvenida.text = "Bienvenido, $apodoAlumno"

        // 🔹 Ir al juego enviando el apodo
        btnJugar.setOnClickListener {
            val intentJuego = Intent(this, mathQuiz::class.java)
            intentJuego.putExtra("apodoAlumno", apodoAlumno)
            startActivity(intentJuego)
        }
    }
}