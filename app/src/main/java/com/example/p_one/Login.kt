package com.example.p_one

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class Login : AppCompatActivity() {

    private lateinit var firebase: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var txtcorreo: EditText
    private lateinit var txtcontrasena: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvOlvidaste: TextView
    private lateinit var tvRegistrar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebase = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        txtcorreo = findViewById(R.id.txt_correo)
        txtcontrasena = findViewById(R.id.txt_contrasena)
        btnLogin = findViewById(R.id.btn_login)
        tvOlvidaste = findViewById(R.id.tv_olvidaste)
        tvRegistrar= findViewById(R.id.tv_registrate)

        btnLogin.setOnClickListener { validador() }
        tvOlvidaste.setOnClickListener { mostrarModalReset() }
        tvRegistrar.setOnClickListener {
            startActivity(Intent(this, crud_registro::class.java))
        }
    }

    private fun validador() {
        val correo = txtcorreo.text.toString().trim()
        val pass = txtcontrasena.text.toString()

        when {
            correo.isEmpty() -> {
                mostrarAlerta("Error", "Ingresa tu correo"); return
            }
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> {
                mostrarAlerta("Error", "Correo no válido"); return
            }
            pass.isEmpty() -> {
                mostrarAlerta("Error", "Ingresa tu contraseña"); return
            }
            pass.length < 6 -> {
                mostrarAlerta("Error", "La contraseña debe tener al menos 6 caracteres"); return
            }
        }

        btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(correo, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        mostrarAlerta("Inicio exitoso", "Usuario correcto.")
                        Handler(Looper.getMainLooper()).postDelayed({
                            startActivity(Intent(this, Crud::class.java))
                            finish()
                        }, 2000) // 2s
                    } else {
                        mostrarAlerta("Verifica tu cuenta", "Debes confirmar tu correo antes de ingresar.")
                        auth.signOut()
                        btnLogin.isEnabled = true
                    }
                } else {
                    val msg = task.exception?.localizedMessage ?: "Correo o contraseña incorrectos."
                    mostrarAlerta("Error", msg)
                    btnLogin.isEnabled = true  // re-habilita en fallo
                }
            }

    }

    private fun mostrarModalReset() {
        val correoActual = txtcorreo.text.toString().trim()
        AlertDialog.Builder(this)
            .setTitle("Recuperar contraseña")
            .setMessage("¿Quieres usar el correo escrito o ingresar otro?")
            .setNegativeButton("Ingresar otro") { _, _ -> pedirCorreoManual() }
            .setPositiveButton("Usar este") { _, _ ->
                if (!Patterns.EMAIL_ADDRESS.matcher(correoActual).matches()) {
                    mostrarAlerta("Correo inválido", "Escribe un correo válido en el campo o ingresa otro.")
                } else {
                    solicitarResetConAuth(correoActual)
                }
            }
            .show()
    }

    private fun pedirCorreoManual() {
        val input = EditText(this).apply {
            hint = "Correo"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine(true)
        }

        AlertDialog.Builder(this)
            .setTitle("Recuperar contraseña")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Enviar") { _, _ ->
                val correo = input.text.toString().trim()
                if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                    mostrarAlerta("Correo inválido", "Ingresa un correo válido.")
                } else {
                    solicitarResetConAuth(correo)
                }
            }
            .show()
    }

    private fun solicitarResetConAuth(correo: String) {
        val email = correo.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrarAlerta("Correo inválido", "Ingresa un correo válido.")
            return
        }

        firebase.collection("users")
            .whereEqualTo("correo", email)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                val puedeEnviar = if (snap.isEmpty) {
                    true
                } else {
                    val doc = snap.documents.first()
                    val last = doc.getTimestamp("lastRecovery")?.toDate()
                    if (last == null) true else (System.currentTimeMillis() - last.time) > 60_000
                }

                if (!puedeEnviar) {
                    mostrarAlerta("Espera un momento", "Intenta nuevamente en unos segundos.")
                } else {
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                if (!snap.isEmpty) {
                                    val id = snap.documents.first().id
                                    firebase.collection("users")
                                        .document(id)
                                        .update("lastRecovery", Timestamp.now())
                                }
                                mostrarAlerta("Listo", "Te envié un correo para restablecer tu contraseña.")
                            } else {
                                val msg = task.exception?.localizedMessage
                                    ?: "No se pudo enviar el correo de recuperación."
                                mostrarAlerta("Error", msg)
                            }
                        }
                }
            }
            .addOnFailureListener {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            mostrarAlerta("Listo", "Te envié un correo para restablecer tu contraseña.")
                        } else {
                            val msg = task.exception?.localizedMessage
                                ?: "No se pudo enviar el correo de recuperación."
                            mostrarAlerta("Error", msg)
                        }
                    }
            }
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        val builder = AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)

        if (titulo != "Inicio exitoso") {
            builder.setPositiveButton("Aceptar", null)
        }

        builder.show()
    }

}
