package com.example.p_one.EditCrud

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.firestore.FirebaseFirestore

class crudAlumnoEditar : AppCompatActivity() {

    private lateinit var firebase: FirebaseFirestore

    private lateinit var txtNombreAlumno: TextInputEditText
    private lateinit var txtApellidoAlumno: TextInputEditText
    private lateinit var txtApodoAlumno: TextInputEditText
    private lateinit var txtEdadAlumno: TextInputEditText
    private lateinit var tvCorreoAlumno: MaterialTextView

    private var documentoId: String? = null

    private var nombreOriginal: String = ""
    private var apellidoOriginal: String = ""
    private var apodoOriginal: String = ""
    private var edadOriginal: Int = 0
    private var correoOriginal: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crud_alumno_editar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebase = FirebaseFirestore.getInstance()

        txtNombreAlumno = findViewById(R.id.txt_nombre)
        txtApellidoAlumno = findViewById(R.id.txt_apellido)
        txtApodoAlumno = findViewById(R.id.txt_apodo)
        txtEdadAlumno = findViewById(R.id.txt_edad)
        tvCorreoAlumno = findViewById(R.id.tvCorreoAlumno)

        documentoId = intent.getStringExtra("docId")

        nombreOriginal = intent.getStringExtra("nombre") ?: ""
        apellidoOriginal = intent.getStringExtra("apellido") ?: ""
        apodoOriginal = intent.getStringExtra("apodo") ?: ""
        correoOriginal = intent.getStringExtra("correo") ?: ""
        edadOriginal = intent.getIntExtra("edad", 0)

        txtNombreAlumno.setText(nombreOriginal)
        txtApellidoAlumno.setText(apellidoOriginal)
        txtApodoAlumno.setText(apodoOriginal)
        if (edadOriginal != 0) txtEdadAlumno.setText(edadOriginal.toString())

        // Mostrar correo como label
        tvCorreoAlumno.text = "Correo: $correoOriginal"

        if (documentoId.isNullOrEmpty()) {
            mostrarAlerta("Error", "No se encontró el alumno a editar.")
            finish()
        }
    }

    fun editarAlumno(view: View) {
        val id = documentoId ?: return

        val nombreNuevo = txtNombreAlumno.text.toString().trim()
        val apellidoNuevo = txtApellidoAlumno.text.toString().trim()
        val apodoNuevo = txtApodoAlumno.text.toString().trim()
        val edadNueva = txtEdadAlumno.text.toString().toIntOrNull() ?: 0

        val datos = mutableMapOf<String, Any>()

        if (nombreNuevo != nombreOriginal) datos["nombre"] = nombreNuevo
        if (apellidoNuevo != apellidoOriginal) datos["apellido"] = apellidoNuevo
        if (apodoNuevo != apodoOriginal) datos["apodoAlumno"] = apodoNuevo
        if (edadNueva != edadOriginal && edadNueva > 0) datos["edadAlumno"] = edadNueva

        if (datos.isEmpty()) {
            mostrarAlerta("Aviso", "No hay cambios para guardar.")
            return
        }

        datos["updatedAt"] = System.currentTimeMillis()

        firebase.collection("users")
            .document(id)
            .update(datos)
            .addOnSuccessListener {
                mostrarAlerta("Éxito", "Datos del alumno actualizados.")

                android.os.Handler(mainLooper).postDelayed({
                    val intent = Intent(this, listcrudAlumno::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }, 3000)
            }
            .addOnFailureListener {
                mostrarAlerta("Error", it.message ?: "No se pudo actualizar el alumno.")
            }
    }

    fun cancelarEdicion(view: View) {
        finish()
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .create()
            .show()
    }
}
