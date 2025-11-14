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
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class crudAdministradorEditar : AppCompatActivity() {

    private lateinit var firebase: FirebaseFirestore

    private lateinit var txtNombreAdmin: TextInputEditText
    private lateinit var txtApellidoAdmin: TextInputEditText
    private lateinit var tvCorreoAdmin: MaterialTextView
    private lateinit var txtContrasenaAdmin: TextInputEditText

    private var documentoId: String? = null   // id del documento en "users"

    private var nombreOriginal: String = ""
    private var apellidoOriginal: String = ""
    private var correoOriginal: String = ""

    private val client = OkHttpClient()
    private val BASE_URL = "https://pone-backend-kz8c.onrender.com"

    private val URL_CAMBIAR_CLAVE =
        "$BASE_URL/cambiarPasswordUsuario"

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

        txtNombreAdmin = findViewById(R.id.txt_nombre_admin)
        txtApellidoAdmin = findViewById(R.id.txt_apellido_admin)
        tvCorreoAdmin = findViewById(R.id.tvCorreoAdmin)
        txtContrasenaAdmin = findViewById(R.id.txt_contrasena_admin)

        documentoId = intent.getStringExtra("docId")

        nombreOriginal = intent.getStringExtra("nombre") ?: ""
        apellidoOriginal = intent.getStringExtra("apellido") ?: ""
        correoOriginal = intent.getStringExtra("correo") ?: ""

        txtNombreAdmin.setText(nombreOriginal)
        txtApellidoAdmin.setText(apellidoOriginal)
        tvCorreoAdmin.text = "Correo: $correoOriginal"

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
        val contrasenaNueva = txtContrasenaAdmin.text?.toString()?.trim().orEmpty()

        val datosActualizados = mutableMapOf<String, Any>()

        if (nombreNuevo != nombreOriginal) {
            datosActualizados["nombre"] = nombreNuevo
        }
        if (apellidoNuevo != apellidoOriginal) {
            datosActualizados["apellido"] = apellidoNuevo
        }

        if (datosActualizados.isNotEmpty() || contrasenaNueva.isNotEmpty()) {
            datosActualizados["updatedAt"] = System.currentTimeMillis()
        }

        if (datosActualizados.isEmpty() && contrasenaNueva.isEmpty()) {
            mostrarAlerta("Aviso", "No hay cambios para guardar.")
            return
        }

        firebase.collection("users")
            .document(id)
            .update(datosActualizados as Map<String, Any>)
            .addOnSuccessListener {
                if (contrasenaNueva.isNotEmpty()) {
                    cambiarClaveBackend(id, contrasenaNueva) { ok, mensaje ->
                        runOnUiThread {
                            if (ok) {
                                mostrarAlerta(
                                    "Éxito",
                                    "Administrador actualizado y contraseña cambiada."
                                )
                            } else {
                                mostrarAlerta(
                                    "Aviso",
                                    "Datos actualizados, pero la contraseña no se pudo cambiar.\n$mensaje"
                                )
                            }

                            android.os.Handler(mainLooper).postDelayed({
                                val intent = Intent(this, listcrudAdmin::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                            }, 3000)
                        }
                    }
                } else {
                    mostrarAlerta("Éxito", "Datos del administrador actualizados correctamente.")
                    android.os.Handler(mainLooper).postDelayed({
                        val intent = Intent(this, listcrudAdmin::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    }, 3000)
                }
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", e.message ?: "No se pudo actualizar el administrador.")
            }
    }

    private fun cambiarClaveBackend(
        idUsuario: String,
        nuevaClave: String,
        callback: (Boolean, String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("idUsuario", idUsuario)
            put("nuevaPassword", nuevaClave)
        }

        val body = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(URL_CAMBIAR_CLAVE)
            .post(body)
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val result = response.body?.string() ?: ""
                callback(response.isSuccessful, result)
            } catch (e: Exception) {
                callback(false, e.message ?: "Error desconocido")
            }
        }.start()
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
