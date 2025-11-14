package com.example.p_one.crudAdmin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.EditCrud.listcrudAlumno
import com.example.p_one.EditCrud.listcrudCurso
import com.example.p_one.EditCrud.listcrudProfesor
import com.example.p_one.Models.Curso
import com.example.p_one.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class crudCursos : AppCompatActivity() {

    private lateinit var firebase: FirebaseFirestore

    private lateinit var txtNombreCurso: TextInputEditText
    private lateinit var txtNivelCurso: TextInputEditText

    private lateinit var spProfes: Spinner

    private val listaProfes = mutableListOf<String>()
    private val listaProfesIds = mutableListOf<String>()
    private lateinit var adaptadorProfes: ArrayAdapter<String>

    private var documentoId: String? = null   // modo edición

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crud_cursos)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        firebase = FirebaseFirestore.getInstance()

        txtNombreCurso = findViewById(R.id.txt_nombre_curso)
        txtNivelCurso = findViewById(R.id.txt_nivel_curso)
        spProfes = findViewById(R.id.spinner_profe)

        adaptadorProfes = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaProfes)
        adaptadorProfes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spProfes.adapter = adaptadorProfes

        cargarProfes()
    }

    fun listcurso(view: View) {
        startActivity(Intent(this, listcrudCurso::class.java))
    }

    private fun cargarProfes() {
        firebase.collection("users")
            .whereEqualTo("rol", "Profesor")
            .get()
            .addOnSuccessListener { snap ->

                listaProfes.clear()
                listaProfesIds.clear()

                for (doc in snap) {
                    val nombre = doc.getString("nombre") ?: ""
                    val apellido = doc.getString("apellido") ?: ""
                    val id = doc.id

                    listaProfes.add("$nombre $apellido")
                    listaProfesIds.add(id)
                }

                adaptadorProfes.notifyDataSetChanged()
                if (listaProfes.isNotEmpty()) spProfes.setSelection(0)
            }
    }

    fun crearCurso(view: View) {
        val nombre = txtNombreCurso.text?.toString()?.trim().orEmpty()
        val nivel = txtNivelCurso.text?.toString()?.trim().orEmpty()

        if (nombre.isEmpty() || nivel.isEmpty()) {
            mostrarAlerta("Error", "Completa nombre y nivel del curso.")
            return
        }

        // profesorId OPCIONAL
        val profesorId: String? = if (listaProfesIds.isNotEmpty()
            && spProfes.selectedItemPosition in listaProfesIds.indices
        ) {
            listaProfesIds[spProfes.selectedItemPosition]
        } else {
            null
        }

        val cursosRef = firebase.collection("Cursos")
        val nuevoDoc = cursosRef.document()
        val idGenerado = nuevoDoc.id

        val curso = Curso(
            idCurso = idGenerado,
            nombreCurso = nombre,
            nivel = nivel,
            profesorId = profesorId
        )

        nuevoDoc.set(curso)
            .addOnSuccessListener {
                val msg = if (profesorId == null) {
                    "Curso '$nombre' creado SIN profesor asignado."
                } else {
                    "Curso '$nombre' creado con profesor asignado."
                }
                mostrarAlerta("Éxito", msg)
                limpiarFormCurso()
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", e.message ?: "No se pudo guardar el curso.")
            }
    }

    private fun limpiarFormCurso() {
        txtNombreCurso.setText("")
        txtNivelCurso.setText("")
        if (listaProfes.isNotEmpty()) spProfes.setSelection(0)
        txtNombreCurso.requestFocus()
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        val b = AlertDialog.Builder(this)
        b.setTitle(titulo)
        b.setMessage(mensaje)
        b.setPositiveButton("Aceptar", null)
        b.create().show()
    }
}
