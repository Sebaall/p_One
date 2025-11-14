package com.example.p_one.crudAdmin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.EditCrud.listcrudAdmin
import com.example.p_one.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class crudAdministradorEditar : AppCompatActivity() {

    private lateinit var firebase: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var txtNombreAdmin: TextInputEditText
    private lateinit var txtApellidoAdmin: TextInputEditText
    private lateinit var txtCorreoAdmin: TextInputEditText
    private lateinit var txtContrasenaAdmin: TextInputEditText

    private var documentoId: String? = null   // id del documento en "users"

    // Valores originales para saber qué cambió
    private var nombreOriginal: String = ""
    private var apellidoOriginal: String = ""
    private var correoOriginal: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crud_administrador_editar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebase = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        txtNombreAdmin = findViewById(R.id.txt_nombre_admin)
        txtApellidoAdmin = findViewById(R.id.txt_apellido_admin)
        txtCorreoAdmin = findViewById(R.id.txt_correo_admin)
        txtContrasenaAdmin = findViewById(R.id.txt_contrasena_admin)

        documentoId = intent.getStringExtra("docId")

        nombreOriginal = intent.getStringExtra("nombre") ?: ""
        apellidoOriginal = intent.getStringExtra("apellido") ?: ""
        correoOriginal = intent.getStringExtra("correo") ?: ""

        txtNombreAdmin.setText(nombreOriginal)
        txtApellidoAdmin.setText(apellidoOriginal)
        txtCorreoAdmin.setText(correoOriginal)

        if (documentoId.isNullOrEmpty()) {
            mostrarAlerta("Error", "No se encontró el administrador a editar.")
            finish()
        }
    }

    fun editarAdministrador(view: View) {
        val id = documentoId
        if (id.isNullOrEmpty()) {
            mostrarAlerta("Error", "No se encontró el administrador a editar.")
            return
        }

        val nombreNuevo = txtNombreAdmin.text?.toString()?.trim().orEmpty()
        val apellidoNuevo = txtApellidoAdmin.text?.toString()?.trim().orEmpty()
        val correoNuevo = txtCorreoAdmin.text?.toString()?.trim().orEmpty()
        val contrasenaNueva = txtContrasenaAdmin.text?.toString()?.trim().orEmpty()

        val datosActualizados = mutableMapOf<String, Any>()

        // Solo se agregan los campos que realmente cambiaste
        if (nombreNuevo != nombreOriginal) {
            datosActualizados["nombre"] = nombreNuevo
        }
        if (apellidoNuevo != apellidoOriginal) {
            datosActualizados["apellido"] = apellidoNuevo
        }
        if (correoNuevo != correoOriginal) {
            datosActualizados["correo"] = correoNuevo
        }

        // Si hay cualquier cambio o cambio de clave, actualizamos updatedAt
        if (datosActualizados.isNotEmpty() || contrasenaNueva.isNotEmpty()) {
            datosActualizados["updatedAt"] = System.currentTimeMillis()
        }

        // Si no cambiaste nada y tampoco pusiste clave nueva
        if (datosActualizados.isEmpty() && contrasenaNueva.isEmpty()) {
            mostrarAlerta("Aviso", "No hay cambios para guardar.")
            return
        }

        fun actualizarEnFirestore() {
            if (datosActualizados.isEmpty()) {
                // No hay campos a actualizar en Firestore (solo cambió la clave, caso extremo)
                finish()
                return
            }

            firebase.collection("users")
                .document(id)
                .update(datosActualizados as Map<String, Any>)
                .addOnSuccessListener {
                    mostrarAlerta("Éxito", "Datos del administrador actualizados correctamente.")

                    // actualiza originales por si te quedaras en pantalla
                    nombreOriginal = nombreNuevo
                    apellidoOriginal = apellidoNuevo
                    correoOriginal = correoNuevo

                    // ⏳ Espera 5 segundos para que alcances a leer la alerta
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this, listcrudAdmin::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()

                    }, 3000)
                }
                .addOnFailureListener { e ->
                    mostrarAlerta("Error", e.message ?: "No se pudo actualizar el administrador.")
                }
        }

        // Si la clave está vacía, solo se actualiza Firestore con lo que cambiaste
        if (contrasenaNueva.isEmpty()) {
            actualizarEnFirestore()
            return
        }

        // Si hay nueva contraseña → intentamos cambiarla en Auth del usuario actual
        val usuarioActual = auth.currentUser
        if (usuarioActual == null || usuarioActual.uid != id) {
            // No es el mismo usuario logueado, no se puede cambiar la clave desde el cliente
            // Pero sí actualizamos los otros datos
            actualizarEnFirestore()
            return
        }

        usuarioActual.updatePassword(contrasenaNueva)
            .addOnSuccessListener {
                // Después de actualizar la clave, también actualizamos Firestore (updatedAt, etc.)
                actualizarEnFirestore()
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", e.message ?: "No se pudo actualizar la contraseña.")
            }
    }

    fun cancelarEdicion(view: View) {
        finish()
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        val b = AlertDialog.Builder(this)
        b.setTitle(titulo)
        b.setMessage(mensaje)
        b.setPositiveButton("Aceptar", null)
        b.create().show()
    }
}
