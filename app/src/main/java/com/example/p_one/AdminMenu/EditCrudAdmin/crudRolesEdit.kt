package com.example.p_one.AdminMenu.EditCrudAdmin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.firestore.FirebaseFirestore

class crudRolesEdit : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    private lateinit var txtNombreRol: TextInputEditText
    private lateinit var txtDescripcionRol: TextInputEditText
    private lateinit var tvCreadorNombre: MaterialTextView
    private lateinit var spinnerPermisoMenu: Spinner
    private lateinit var btnCancelarRol: MaterialButton
    private lateinit var btnEditarRol: MaterialButton
    private lateinit var progressRol: ProgressBar

    private var idRol: String? = null
    private var permisosLista: ArrayList<String> = arrayListOf()

    // solo primera letra en mayúscula
    private fun capitalizar(texto: String): String {
        return texto.trim().lowercase().replaceFirstChar { it.uppercase() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crud_roles_edit)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()

        txtNombreRol = findViewById(R.id.txtNombreRol)
        txtDescripcionRol = findViewById(R.id.txtDescripcionRol)
        tvCreadorNombre = findViewById(R.id.tvCreadorNombre)
        spinnerPermisoMenu = findViewById(R.id.spinnerPermisoMenu)
        btnCancelarRol = findViewById(R.id.btnCancelarRol)
        btnEditarRol = findViewById(R.id.btnEditarRol)
        progressRol = findViewById(R.id.progressRol)

        idRol = intent.getStringExtra("idRol")
        val nombreRol = intent.getStringExtra("nombreRol") ?: ""
        val descripcionRol = intent.getStringExtra("descripcionRol") ?: ""
        val creadorId = intent.getStringExtra("creadoPor") ?: ""
        val creadorNombreExtra = intent.getStringExtra("creadorNombre")

        val textoCreador = when {
            !creadorNombreExtra.isNullOrEmpty() -> creadorNombreExtra
            creadorId.isNotEmpty() -> creadorId
            else -> "Desconocido"
        }

        permisosLista = intent.getStringArrayListExtra("permisos") ?: arrayListOf("Sin permisos")

        txtNombreRol.setText(nombreRol)
        txtDescripcionRol.setText(descripcionRol)
        tvCreadorNombre.text = textoCreador

        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            permisosLista
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPermisoMenu.adapter = spinnerAdapter

        if (permisosLista.isNotEmpty()) {
            spinnerPermisoMenu.setSelection(0)
        }

        btnCancelarRol.setOnClickListener {
            finish()
        }

        btnEditarRol.setOnClickListener {
            guardarCambios()
        }
    }

    private fun guardarCambios() {
        val id = idRol
        if (id.isNullOrEmpty()) {
            mostrarAlerta("Error", "No se encontró el id del rol.")
            return
        }

        val nombre = capitalizar(txtNombreRol.text?.toString().orEmpty())
        val descripcion = capitalizar(txtDescripcionRol.text?.toString().orEmpty())

        if (nombre.isEmpty()) {
            mostrarAlerta("Aviso", "El nombre del rol no puede estar vacío.")
            return
        }

        val permisoSeleccionado = spinnerPermisoMenu.selectedItem?.toString() ?: ""

        val datosActualizados = mutableMapOf<String, Any>(
            "nombreRol" to nombre,
            "descripcionRol" to descripcion
        )

        if (permisoSeleccionado.isNotEmpty() && permisoSeleccionado != "Sin permisos") {
            datosActualizados["permisos"] = listOf(permisoSeleccionado)
        }

        progressRol.visibility = View.VISIBLE

        db.collection("Roles")
            .document(id)
            .update(datosActualizados)
            .addOnSuccessListener {
                progressRol.visibility = View.GONE
                mostrarAlerta("Éxito", "Rol actualizado correctamente.")
                finish()
            }
            .addOnFailureListener { e ->
                progressRol.visibility = View.GONE
                mostrarAlerta("Error", "Error al actualizar: ${e.message}")
            }
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
