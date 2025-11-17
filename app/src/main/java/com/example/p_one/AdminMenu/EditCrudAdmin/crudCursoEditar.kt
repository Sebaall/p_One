package com.example.p_one.AdminMenu.EditCrudAdmin

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.AdminMenu.ListCrudAdmin.listcrudCurso
import com.example.p_one.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class crudCursoEditar : AppCompatActivity() {

    private lateinit var firebase: FirebaseFirestore

    private lateinit var txtNombreCurso: TextInputEditText
    private lateinit var txtNivelCurso: TextInputEditText
    private lateinit var spinnerProfe: Spinner
    private lateinit var btnCancelarCurso: MaterialButton
    private lateinit var btnEditarCurso: MaterialButton

    private var documentoId: String? = null

    private var nombreOriginal = ""
    private var nivelOriginal = ""
    private var profesorOriginalId: String? = null

    private val listaProfesores = mutableListOf<String>()
    private val listaIdsProfes = mutableListOf<String>()
    private var profesorSeleccionadoId: String? = null

    // solo primera letra en mayúscula
    private fun capitalizar(texto: String): String {
        return texto.trim().lowercase().replaceFirstChar { it.uppercase() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crud_curso_editar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, ins ->
            val s = ins.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(s.left, s.top, s.right, s.bottom)
            ins
        }

        firebase = FirebaseFirestore.getInstance()

        txtNombreCurso = findViewById(R.id.txt_nombre_curso)
        txtNivelCurso = findViewById(R.id.txt_nivel_curso)
        spinnerProfe = findViewById(R.id.spinner_profe)
        btnCancelarCurso = findViewById(R.id.btnCancelarCurso)
        btnEditarCurso = findViewById(R.id.btnEditarCurso)

        documentoId = intent.getStringExtra("docId")
        nombreOriginal = intent.getStringExtra("nombreCurso") ?: ""
        nivelOriginal = intent.getStringExtra("nivel") ?: ""
        profesorOriginalId = intent.getStringExtra("profesorId")

        txtNombreCurso.setText(nombreOriginal)
        txtNivelCurso.setText(nivelOriginal)
        profesorSeleccionadoId = profesorOriginalId

        if (documentoId.isNullOrEmpty()) {
            mostrarAlerta("Error", "No se encontró el curso.")
            finish()
        }

        btnCancelarCurso.setOnClickListener { cancelarEdicion() }
        btnEditarCurso.setOnClickListener { editarCurso(it) }

        cargarProfesores()
    }

    private fun cargarProfesores() {
        firebase.collection("users")
            .whereEqualTo("rol", "Profesor")
            .get()
            .addOnSuccessListener { result ->
                listaProfesores.clear()
                listaIdsProfes.clear()

                for (doc in result) {
                    val nombreRaw = doc.getString("nombre") ?: ""
                    val apellidoRaw = doc.getString("apellido") ?: ""

                    val nombre = capitalizar(nombreRaw)
                    val apellido = capitalizar(apellidoRaw)

                    val idProfe = doc.getString("idProfesor") ?: doc.id

                    listaProfesores.add("$nombre $apellido".trim())
                    listaIdsProfes.add(idProfe)
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    listaProfesores
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerProfe.adapter = adapter

                if (!profesorSeleccionadoId.isNullOrEmpty()) {
                    val index = listaIdsProfes.indexOf(profesorSeleccionadoId)
                    if (index != -1) spinnerProfe.setSelection(index)
                }

                spinnerProfe.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            pos: Int,
                            id: Long
                        ) {
                            profesorSeleccionadoId = listaIdsProfes[pos]
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }
            }
            .addOnFailureListener {
                mostrarAlerta("Error", "No se pudieron cargar los profesores.")
            }
    }

    fun editarCurso(view: View) {
        val id = documentoId ?: return

        val nombreNuevo = capitalizar(txtNombreCurso.text.toString())
        val nivelNuevo = capitalizar(txtNivelCurso.text.toString())
        val profesorNuevo = profesorSeleccionadoId

        val datos = mutableMapOf<String, Any>()

        if (nombreNuevo != nombreOriginal) datos["nombreCurso"] = nombreNuevo
        if (nivelNuevo != nivelOriginal) datos["nivel"] = nivelNuevo
        if (!profesorNuevo.isNullOrEmpty()) datos["profesorId"] = profesorNuevo

        datos["updatedAt"] = System.currentTimeMillis()

        firebase.collection("Cursos")
            .document(id)
            .update(datos)
            .addOnSuccessListener {
                mostrarAlerta("Éxito", "Curso actualizado correctamente.")

                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, listcrudCurso::class.java))
                    finish()
                }, 2500)
            }
            .addOnFailureListener {
                mostrarAlerta("Error", "No se pudo actualizar el curso.")
            }
    }

    fun cancelarEdicion() {
        finish()
    }

    private fun mostrarAlerta(t: String, m: String) {
        AlertDialog.Builder(this)
            .setTitle(t)
            .setMessage(m)
            .setPositiveButton("Aceptar", null)
            .show()
    }
}
