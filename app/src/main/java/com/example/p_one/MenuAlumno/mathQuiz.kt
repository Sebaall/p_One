package com.example.p_one.MenuAlumno

import MathQuizResultado
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class mathQuiz : AppCompatActivity() {

    private enum class OperationType {
        SUMA, RESTA, MULTIPLICACION, DIVISION
    }

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var tvApodoHeader: TextView
    private lateinit var tvProgreso: TextView
    private lateinit var tvOperacion: TextView
    private lateinit var tvResultadoInstantaneo: TextView
    private lateinit var tvResumenParcial: TextView

    private lateinit var btnOpcion1: MaterialButton
    private lateinit var btnOpcion2: MaterialButton
    private lateinit var btnOpcion3: MaterialButton
    private lateinit var btnOpcion4: MaterialButton
    private lateinit var btnSiguiente: MaterialButton

    private val random = Random(System.currentTimeMillis())

    private var num1: Int = 0
    private var num2: Int = 0
    private var respuestaCorrecta: Int = 0
    private var operacionActual: OperationType = OperationType.SUMA
    private var simboloOperacion: String = "+"

    private val totalPreguntas = 10
    private var numeroPregunta = 1
    private var correctas = 0
    private var incorrectas = 0

    private var respondido = false

    private var apodoAlumno: String = "Invitado"
    private var uidAuth: String? = null

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
        setContentView(R.layout.activity_math_quiz)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        uidAuth = auth.currentUser?.uid

        apodoAlumno = intent.getStringExtra("apodoAlumno") ?: "Invitado"

        tvApodoHeader = findViewById(R.id.tvApodoHeader)
        tvProgreso = findViewById(R.id.tvProgreso)
        tvOperacion = findViewById(R.id.tvOperacion)
        tvResultadoInstantaneo = findViewById(R.id.tvResultadoInstantaneo)
        tvResumenParcial = findViewById(R.id.tvResumenParcial)

        btnOpcion1 = findViewById(R.id.btnOpcion1)
        btnOpcion2 = findViewById(R.id.btnOpcion2)
        btnOpcion3 = findViewById(R.id.btnOpcion3)
        btnOpcion4 = findViewById(R.id.btnOpcion4)
        btnSiguiente = findViewById(R.id.btnSiguiente)

        tvApodoHeader.text = "Alumno: $apodoAlumno"
        actualizarProgreso()
        actualizarResumenParcial()

        btnOpcion1.setOnClickListener {
            if (!respondido) {
                marcarOpcionSeleccionada(btnOpcion1)
                verificarRespuesta(btnOpcion1.text.toString())
            }
        }

        btnOpcion2.setOnClickListener {
            if (!respondido) {
                marcarOpcionSeleccionada(btnOpcion2)
                verificarRespuesta(btnOpcion2.text.toString())
            }
        }

        btnOpcion3.setOnClickListener {
            if (!respondido) {
                marcarOpcionSeleccionada(btnOpcion3)
                verificarRespuesta(btnOpcion3.text.toString())
            }
        }

        btnOpcion4.setOnClickListener {
            if (!respondido) {
                marcarOpcionSeleccionada(btnOpcion4)
                verificarRespuesta(btnOpcion4.text.toString())
            }
        }

        btnSiguiente.setOnClickListener {
            if (!respondido) {
                mostrarAlerta("Aviso", "Primero responde la pregunta")
            } else {
                if (numeroPregunta >= totalPreguntas) {
                    guardarResultadoEnFirestore()
                } else {
                    numeroPregunta++
                    generarNuevaPregunta()
                }
            }
        }

        generarNuevaPregunta()
    }

    private fun actualizarProgreso() {
        tvProgreso.text = "Pregunta $numeroPregunta / $totalPreguntas"
    }

    private fun actualizarResumenParcial() {
        tvResumenParcial.text = "Correctas: $correctas | Incorrectas: $incorrectas"
    }

    private fun generarNuevaPregunta() {
        respondido = false
        tvResultadoInstantaneo.text = ""
        prepararNuevaPreguntaVisual()

        operacionActual = OperationType.values()[random.nextInt(OperationType.values().size)]

        when (operacionActual) {

            OperationType.SUMA -> {
                simboloOperacion = "+"
                num1 = random.nextInt(1, 10)
                num2 = random.nextInt(1, 10)
                respuestaCorrecta = num1 + num2
            }

            OperationType.RESTA -> {
                simboloOperacion = "-"
                val a = random.nextInt(1, 10)
                val b = random.nextInt(1, 10)
                num1 = max(a, b)
                num2 = min(a, b)
                respuestaCorrecta = num1 - num2
            }

            OperationType.MULTIPLICACION -> {
                simboloOperacion = "×"
                num1 = random.nextInt(1, 10)
                num2 = random.nextInt(1, 10)
                respuestaCorrecta = num1 * num2
            }

            OperationType.DIVISION -> {
                simboloOperacion = "÷"
                val resultado = random.nextInt(1, 10)
                val divisor = random.nextInt(1, 10)

                num1 = resultado * divisor
                num2 = divisor
                respuestaCorrecta = resultado
            }
        }

        tvOperacion.text = "$num1 $simboloOperacion $num2"
        actualizarProgreso()

        val opcionesSet = mutableSetOf<Int>()
        opcionesSet.add(respuestaCorrecta)

        while (opcionesSet.size < 4) {
            opcionesSet.add(random.nextInt(1, 20))
        }

        val opcionesList = opcionesSet.shuffled()
        btnOpcion1.text = opcionesList[0].toString()
        btnOpcion2.text = opcionesList[1].toString()
        btnOpcion3.text = opcionesList[2].toString()
        btnOpcion4.text = opcionesList[3].toString()
    }

    private fun verificarRespuesta(textoBoton: String) {
        if (respondido) return

        val respuestaElegida = textoBoton.toIntOrNull() ?: return
        respondido = true

        if (respuestaElegida == respuestaCorrecta) {
            correctas++
            tvResultadoInstantaneo.text = "¡Correcto!"
            tvResultadoInstantaneo.setTextColor(Color.GREEN)
        } else {
            incorrectas++
            tvResultadoInstantaneo.text = "Incorrecto"
            tvResultadoInstantaneo.setTextColor(Color.RED)
        }

        actualizarResumenParcial()
    }

    private fun resetVisualOpciones() {
        val blanco = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
        val naranjaTexto = Color.parseColor("#FFA726")

        listOf(btnOpcion1, btnOpcion2, btnOpcion3, btnOpcion4).forEach {
            it.backgroundTintList = blanco
            it.setTextColor(naranjaTexto)
        }
    }

    private fun marcarOpcionSeleccionada(btn: MaterialButton) {
        val naranjaSeleccion = ColorStateList.valueOf(Color.parseColor("#FFCC80"))
        val naranjaSuaveSiguiente = ColorStateList.valueOf(Color.parseColor("#FFB74D"))

        resetVisualOpciones()

        btn.backgroundTintList = naranjaSeleccion
        btn.setTextColor(Color.WHITE)

        btnSiguiente.isEnabled = true
        btnSiguiente.backgroundTintList = naranjaSuaveSiguiente
        btnSiguiente.setTextColor(Color.WHITE)
    }

    private fun prepararNuevaPreguntaVisual() {
        resetVisualOpciones()

        btnSiguiente.isEnabled = false
        btnSiguiente.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#BDBDBD"))
        btnSiguiente.setTextColor(Color.WHITE)
    }

    private fun guardarResultadoEnFirestore() {
        val porcentaje = (correctas * 100.0) / totalPreguntas.toDouble()

        val resultado = MathQuizResultado(
            uidAuth = uidAuth,
            apodo = apodoAlumno,
            correctas = correctas,
            incorrectas = incorrectas,
            totalPreguntas = totalPreguntas,
            porcentaje = porcentaje,
            fechaUltimoJuego = Timestamp.now()
        )

        db.collection("mathQuizResultados")
            .document(apodoAlumno)
            .set(resultado, SetOptions.merge())
            .addOnSuccessListener {
                val intent = Intent(this, Results::class.java)
                intent.putExtra("apodoAlumno", apodoAlumno)
                intent.putExtra("correctas", correctas)
                intent.putExtra("incorrectas", incorrectas)
                intent.putExtra("totalPreguntas", totalPreguntas)
                intent.putExtra("porcentaje", porcentaje)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", "Error al guardar resultados: ${e.message}")
            }
    }

}
