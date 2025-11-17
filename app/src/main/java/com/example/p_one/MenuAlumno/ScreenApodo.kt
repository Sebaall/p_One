package com.example.p_one.MenuAlumno
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.Main.Bienvenidas.bienvenidaScreen
import com.example.p_one.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ScreenApodo : AppCompatActivity() {

    private lateinit var etApodo: EditText
    private lateinit var btnComenzarQuiz: Button
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // 👉 FUNCION QUE ME PEDISTE (solo esto se agregó)
    private fun capitalizar(texto: String): String {
        return texto.trim().lowercase().replaceFirstChar { it.uppercase() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "No hay sesión activa", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uid = currentUser.uid

        // 🔎 Revisamos si ya tiene apodo guardado en Firestore
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val apodo = doc.getString("apodoAlumno")
                val nombre = doc.getString("nombre")

                if (!apodo.isNullOrBlank()) {
                    // 👉 YA TIENE APODO → saltar pantalla y mostrar Bienvenida
                    irABienvenida(nombre, apodo)
                    finish()
                } else {
                    // 👉 NO tiene apodo → mostrar pantalla para ingresarlo
                    mostrarPantallaApodo(uid)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                mostrarPantallaApodo(uid)
            }
    }

    private fun mostrarPantallaApodo(uid: String) {
        setContentView(R.layout.activity_screen_apodo)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etApodo = findViewById(R.id.etApodo)
        btnComenzarQuiz = findViewById(R.id.btnComenzarQuiz)

        btnComenzarQuiz.setOnClickListener {
            val apodoIngresado = capitalizar(etApodo.text.toString()) // 👉 SOLO ESTA LÍNEA USA LA FUNCIÓN

            if (apodoIngresado.isEmpty()) {
                Toast.makeText(this, "Ingresa un apodo", Toast.LENGTH_SHORT).show()
            } else {
                val updates = mapOf(
                    "apodoAlumno" to apodoIngresado,
                    "updatedAt" to System.currentTimeMillis()
                )

                db.collection("users").document(uid)
                    .update(updates)
                    .addOnSuccessListener {
                        irAlJuego(apodoIngresado)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al guardar el apodo", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun irABienvenida(nombre: String?, apodo: String) {
        val i = Intent(this, bienvenidaScreen::class.java)
        i.putExtra("nombreUsuario", nombre)
        i.putExtra("apodoAlumno", apodo)
        startActivity(i)
    }

    private fun irAlJuego(apodo: String) {
        val i = Intent(this, mathQuiz::class.java)
        i.putExtra("apodoAlumno", apodo)
        startActivity(i)
    }
}
