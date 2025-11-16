package com.example.p_one.Main.Bienvenidas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.Main.menuProfesor
import com.example.p_one.R

class bienvenidaScreenProfe : AppCompatActivity() {

    private lateinit var tvBienvenidaProfe: TextView
    private lateinit var btnIrMenuProfe: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bienvenida_screen_profe)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvBienvenidaProfe = findViewById(R.id.tvBienvenidaProfe)
        btnIrMenuProfe = findViewById(R.id.btnIrMenuProfe)

        val nombre = intent.getStringExtra("nombre") ?: "Profesor"
        tvBienvenidaProfe.text = "Bienvenido, Prof. $nombre"

        btnIrMenuProfe.setOnClickListener {
            val intentMenuProfe = Intent(this, menuProfesor::class.java)
            intentMenuProfe.putExtra("nombre", nombre)
            startActivity(intentMenuProfe)
        }
    }
}