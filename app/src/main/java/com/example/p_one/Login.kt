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

class Login : AppCompatActivity() {

    private lateinit var firebase: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var txtcorreo: EditText
    private lateinit var txtcontrasena: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvOlvidaste: TextView

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

        btnLogin.setOnClickListener { validador() }
        tvOlvidaste.setOnClickListener { mostrarModalReset() }
    }

    private fun validador() {
        val correo = txtcorreo.text.toString().trim()
        val pass = txtcontrasena.text.toString()

        when {
            correo.isEmpty() -> { mostrarAlerta("Error", "Ingresa tu correo"); return }
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> { mostrarAlerta("Error", "Correo no válido"); return }
            pass.isEmpty() -> { mostrarAlerta("Error", "Ingresa tu contraseña"); return }
            pass.length < 6 -> { mostrarAlerta("Error", "La contraseña debe tener al menos 6 caracteres"); return }
        }

        auth.signInWithEmailAndPassword(correo, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    borrarContrasenaEnFirestore(correo)
                    mostrarAlerta("Inicio exitoso", "Usuario correcto. Redirigiendo…")
                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this, Crud::class.java)
                        startActivity(intent)
                        finish()
                    }, 3000)
                } else {
                    val msg = task.exception?.localizedMessage ?: "Correo o contraseña incorrectos."
                    mostrarAlerta("Error", msg)
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
        val input = EditText(this)
        input.hint = "Correo"
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.setSingleLine(true)

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
        firebase.collection("users")
            .whereEqualTo("correo", correo)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    mostrarAlerta("Error", "El correo no existe en la base de datos.")
                } else {
                    val doc = snap.documents.first()
                    val usado = doc.getBoolean("recoveryUsed") ?: false

                    if (usado) {
                        mostrarAlerta("Aviso", "Ya usaste la recuperación de contraseña una vez.")
                    } else {
                        auth.sendPasswordResetEmail(correo)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    mostrarAlerta("Listo", "Te envié un correo para restablecer tu contraseña.")
                                    firebase.collection("users")
                                        .document(doc.id)
                                        .update("recoveryUsed", true)
                                } else {
                                    val msg = task.exception?.localizedMessage
                                        ?: "No se pudo enviar el correo de recuperación."
                                    mostrarAlerta("Error", msg)
                                }
                            }
                    }
                }
            }
    }

    private fun crearCuentaDesdeFirestoreYReintentar(correo: String) {
        firebase.collection("users")
            .whereEqualTo("correo", correo)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    mostrarAlerta("Error", "El correo no existe en tu colección 'users'.")
                } else {
                    val doc = snap.documents.first()
                    val passDb = doc.getString("contrasena") ?: ""
                    val clave = if (passDb.length >= 6) passDb else "Tmp" + System.currentTimeMillis() + "!"
                    auth.createUserWithEmailAndPassword(correo, clave)
                        .addOnCompleteListener { createTask ->
                            if (createTask.isSuccessful) {
                                try { auth.signOut() } catch (_: Exception) {}
                                auth.sendPasswordResetEmail(correo)
                                    .addOnCompleteListener { retry ->
                                        if (retry.isSuccessful) {
                                            mostrarAlerta("Listo", "Te envié un correo para restablecer tu contraseña.")
                                        } else {
                                            val msg = retry.exception?.localizedMessage ?: "No se pudo enviar el correo de recuperación."
                                            mostrarAlerta("Error", msg)
                                        }
                                    }
                            } else {
                                val msg = createTask.exception?.localizedMessage ?: "No se pudo crear la cuenta en Auth."
                                mostrarAlerta("Error", msg)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", e.localizedMessage ?: "Error leyendo Firestore.")
            }
    }

    private fun borrarContrasenaEnFirestore(correo: String) {
        firebase.collection("users")
            .whereEqualTo("correo", correo)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    val id = snap.documents.first().id
                    val updates = hashMapOf<String, Any?>("contrasena" to null)
                    firebase.collection("users").document(id).update(updates)
                }
            }
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .show()
    }
}
