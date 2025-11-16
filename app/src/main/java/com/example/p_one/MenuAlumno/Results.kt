package com.example.p_one.MenuAlumno

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.R

class Results : AppCompatActivity() {

    private lateinit var tvApodoResultado: TextView
    private lateinit var tvResumenResultado: TextView
    private lateinit var tvDetalleResultado: TextView
    private lateinit var btnVerRanking: Button

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        val b = AlertDialog.Builder(this)
        b.setTitle(titulo)
        b.setMessage(mensaje)
        b.setPositiveButton("Aceptar", null)
        b.create().show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_results)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvApodoResultado = findViewById(R.id.tvApodoResultado)
        tvResumenResultado = findViewById(R.id.tvResumenResultado)
        tvDetalleResultado = findViewById(R.id.tvDetalleResultado)
        btnVerRanking = findViewById(R.id.btnVerRanking)

        val apodo = intent.getStringExtra("apodoAlumno") ?: "-"
        val correctas = intent.getIntExtra("correctas", 0)
        val incorrectas = intent.getIntExtra("incorrectas", 0)
        val totalPreguntas = intent.getIntExtra("totalPreguntas", 0)
        val porcentaje = intent.getDoubleExtra("porcentaje", 0.0)

        tvApodoResultado.text = "Alumno: $apodo"
        tvResumenResultado.text = "Resultados del juego matemático"
        tvDetalleResultado.text =
            "Total de preguntas: $totalPreguntas\n" +
                    "Correctas: $correctas\n" +
                    "Incorrectas: $incorrectas\n" +
                    "Porcentaje de acierto: ${porcentaje.toInt()}%"

        btnVerRanking.setOnClickListener {
            val intentRanking = Intent(this, Ranking::class.java)
            intentRanking.putExtra("apodoAlumno", apodo)
            startActivity(intentRanking)
        }
    }
}
