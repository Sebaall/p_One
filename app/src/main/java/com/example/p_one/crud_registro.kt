package com.example.p_one

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.Models.Users
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class crud_registro : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var txtEmail: TextInputEditText
    private lateinit var txtNombre: TextInputEditText
    private lateinit var txtApellido: TextInputEditText
    private lateinit var txtPassword: TextInputEditText
    private lateinit var btnRegistrar: Button
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crud_registro)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        txtEmail = findViewById(R.id.txtEmail)
        txtNombre = findViewById(R.id.txtNombre)
        txtApellido = findViewById(R.id.txt_Apellido)
        txtPassword = findViewById(R.id.txtPassword)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        progress = findViewById(R.id.progress)

        btnRegistrar.setOnClickListener { registrar() }
    }

    override fun onBackPressed() { }

    private fun registrar() {
        val email = txtEmail.text?.toString()?.trim()?.lowercase().orEmpty()
        val nombre = txtNombre.text?.toString()?.trim().orEmpty()
        val apellido = txtApellido.text?.toString()?.trim().orEmpty()
        val pass = txtPassword.text?.toString().orEmpty()

        when {
            email.isEmpty() -> { alerta("Ingresa tu correo"); return }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { alerta("Correo no válido"); return }
            nombre.isEmpty() -> { alerta("Ingresa tu nombre"); return }
            apellido.isEmpty() -> { alerta("Ingresa tu apellido"); return }
            pass.isEmpty() -> { alerta("Ingresa tu contraseña"); return }
            pass.length < 6 -> { alerta("La contraseña debe tener al menos 6 caracteres"); return }
        }

        bloquear(true)
        limpiarStalePorUsuario(nombre) {
            limpiarStalePorCorreo(email) {
                crearCuenta(email, nombre, apellido, pass)
            }
        }
    }

    private fun crearCuenta(email: String, nombre: String, apellido: String, pass: String) {
        db.collection("users").whereEqualTo("nombre", nombre).limit(1).get()
            .addOnSuccessListener { snapUser ->
                if (snapUser.isEmpty) {
                    db.collection("users").whereEqualTo("correo", email).limit(1).get()
                        .addOnSuccessListener { snapMail ->
                            if (snapMail.isEmpty) {
                                auth.createUserWithEmailAndPassword(email, pass)
                                    .addOnSuccessListener { res ->
                                        val u = res.user
                                        if (u != null) {
                                            val perfil = Users(
                                                idUsuario = u.uid,
                                                correo = email,
                                                nombre = nombre,
                                                apellido = apellido,
                                                roles = null,
                                                idPerfil = null,
                                                activo = false,
                                                emailVerificado = false,
                                                createdAt = System.currentTimeMillis()
                                            )

                                            db.collection("users").document(u.uid).set(perfil)
                                                .addOnSuccessListener {
                                                    asignarRolPorDefecto(u)
                                                    enviarCorreoVerificacion(u) { ok, info ->
                                                        if (ok) {
                                                            db.collection("users").document(u.uid)
                                                                .update(mapOf("welcomeSent" to true))
                                                        }
                                                        auth.signOut()
                                                        bloquear(false)
                                                        if (ok) {
                                                            alerta("Cuenta creada. Revisa tu correo y verifica para poder ingresar.")
                                                            finish()
                                                        } else {
                                                            alerta("Cuenta creada, pero no pude enviar el correo: ${info ?: "intenta más tarde"}")
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    bloquear(false)
                                                    alerta(e.message ?: "Error guardando el perfil.")
                                                }
                                        } else {
                                            bloquear(false)
                                            alerta("No se obtuvo el usuario.")
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        bloquear(false)
                                        if (e is FirebaseAuthUserCollisionException) {
                                            alerta("Ese correo ya está registrado.")
                                        } else {
                                            alerta(e.message ?: "No se pudo crear la cuenta.")
                                        }
                                    }
                            } else {
                                bloquear(false)
                                alerta("Ese correo ya está en uso.")
                            }
                        }
                        .addOnFailureListener { e ->
                            bloquear(false)
                            alerta(e.message ?: "Error al validar correo.")
                        }
                } else {
                    bloquear(false)
                    alerta("Ese nombre ya está en uso.")
                }
            }
            .addOnFailureListener { e ->
                bloquear(false)
                alerta(e.message ?: "Error al validar nombre.")
            }
    }

    private fun enviarCorreoVerificacion(user: FirebaseUser, cb: (Boolean, String?) -> Unit) {
        auth.setLanguageCode("es")
        user.sendEmailVerification()
            .addOnCompleteListener { t ->
                if (t.isSuccessful) cb(true, null)
                else cb(false, t.exception?.localizedMessage ?: "Fallo al enviar verificación")
            }
    }

    private fun limpiarStalePorUsuario(nombre: String, continuar: () -> Unit) {
        db.collection("users").whereEqualTo("nombre", nombre).limit(1).get()
            .addOnSuccessListener { continuar() }
            .addOnFailureListener { continuar() }
    }

    private fun limpiarStalePorCorreo(email: String, continuar: () -> Unit) {
        db.collection("users").whereEqualTo("correo", email).limit(1).get()
            .addOnSuccessListener { continuar() }
            .addOnFailureListener { continuar() }
    }

    private fun bloquear(b: Boolean) {
        progress.visibility = if (b) View.VISIBLE else View.GONE
        btnRegistrar.isEnabled = !b
        txtEmail.isEnabled = !b
        txtNombre.isEnabled = !b
        txtApellido.isEnabled = !b
        txtPassword.isEnabled = !b
    }

    private fun alerta(msg: String) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    fun volver(view: View) {
        startActivity(Intent(this, Login::class.java))
    }

    private fun asignarRolPorDefecto(user: FirebaseUser) {
        val correoSan = user.email.orEmpty().replace(Char(64).toString(), "(at)")
        val dataUser = hashMapOf(
            "correoSan" to correoSan,
            "roles" to listOf("MENU_ALUMNOS"),
            "nivelAcceso" to 1,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("users")
            .document(user.uid)
            .set(dataUser, com.google.firebase.firestore.SetOptions.merge())
    }
}
