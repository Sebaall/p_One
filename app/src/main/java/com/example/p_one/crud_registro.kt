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
    private lateinit var txtUsuario: TextInputEditText
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
        txtUsuario = findViewById(R.id.txtNombre)
        txtPassword = findViewById(R.id.txtPassword)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        progress = findViewById(R.id.progress)

        btnRegistrar.setOnClickListener { registrar() }
    }
    //bloquea boton de volver
    override fun onBackPressed() {
    }
    private fun registrar() {
        val email = txtEmail.text?.toString()?.trim()?.lowercase().orEmpty()
        val usuario = txtUsuario.text?.toString()?.trim().orEmpty()
        val pass = txtPassword.text?.toString().orEmpty()

        when {
            email.isEmpty() -> { alerta("Ingresa tu correo"); return }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { alerta("Correo no válido"); return }
            usuario.isEmpty() -> { alerta("Ingresa tu nombre de usuario"); return }
            pass.isEmpty() -> { alerta("Ingresa tu contraseña"); return }
            pass.length < 6 -> { alerta("La contraseña debe tener al menos 6 caracteres"); return }
        }

        bloquear(true)

        limpiarStalePorUsuario(usuario) {
            limpiarStalePorCorreo(email) {
                crearCuenta(email, usuario, pass)
            }
        }
    }

    private fun crearCuenta(email: String, usuario: String, pass: String) {
        db.collection("users").whereEqualTo("nombreusuario", usuario).limit(1).get()
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
                                                nombreusuario = usuario,
                                                rol = null,
                                                idPerfil = null,
                                                activo = false,
                                                emailVerificado = false,
                                                createdAt = System.currentTimeMillis()
                                            )

                                            db.collection("users").document(u.uid).set(perfil)
                                                .addOnSuccessListener {
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
                    alerta("Ese nombre de usuario ya está en uso.")
                }
            }
            .addOnFailureListener { e ->
                bloquear(false)
                alerta(e.message ?: "Error al validar nombre de usuario.")
            }
    }

    private fun enviarCorreoVerificacion(user: FirebaseUser, cb: (Boolean, String?) -> Unit) {
        auth.setLanguageCode("es")
        user.sendEmailVerification()
            .addOnCompleteListener { t ->
                if (t.isSuccessful) {
                    cb(true, null)
                } else {
                    val again = FirebaseAuth.getInstance().currentUser
                    if (again != null) {
                        again.sendEmailVerification()
                            .addOnCompleteListener { tt ->
                                if (tt.isSuccessful) cb(true, null)
                                else cb(false, tt.exception?.localizedMessage ?: "Fallo al enviar verificación")
                            }
                    } else {
                        cb(false, t.exception?.localizedMessage ?: "Usuario no disponible para verificación")
                    }
                }
            }
    }

    private fun limpiarStalePorUsuario(usuario: String, continuar: () -> Unit) {
        db.collection("users").whereEqualTo("nombreusuario", usuario).limit(1).get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    continuar()
                } else {
                    val doc = snap.documents.first()
                    val idU = doc.getString("idUsuario").orEmpty()
                    if (idU.isBlank()) {
                        doc.reference.delete()
                            .addOnSuccessListener { continuar() }
                            .addOnFailureListener { continuar() }
                    } else {
                        continuar()
                    }
                }
            }
            .addOnFailureListener { continuar() }
    }

    private fun limpiarStalePorCorreo(email: String, continuar: () -> Unit) {
        db.collection("users").whereEqualTo("correo", email).limit(1).get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    continuar()
                } else {
                    val doc = snap.documents.first()
                    val idU = doc.getString("idUsuario").orEmpty()
                    if (idU.isBlank()) {
                        doc.reference.delete()
                            .addOnSuccessListener { continuar() }
                            .addOnFailureListener { continuar() }
                    } else {
                        continuar()
                    }
                }
            }
            .addOnFailureListener { continuar() }
    }

    private fun bloquear(b: Boolean) {
        progress.visibility = if (b) View.VISIBLE else View.GONE
        btnRegistrar.isEnabled = !b
        txtEmail.isEnabled = !b
        txtUsuario.isEnabled = !b
        txtPassword.isEnabled = !b
    }

    private fun alerta(msg: String) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }
   fun volver(view: View){
        startActivity(Intent(this, Login::class.java))
    }
}
