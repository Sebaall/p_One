package com.example.p_one

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.Models.Curso
import com.example.p_one.Models.Alumno
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions // AGREGADO

class Crud : AppCompatActivity() {

    private lateinit var firebase: FirebaseFirestore
    private lateinit var txt_nombre: EditText
    private lateinit var txt_apellido: EditText
    private lateinit var txt_apodo: EditText
    private lateinit var txt_edad: EditText
    private val listaCursos = mutableListOf<Curso>()
    private val listaIds = mutableListOf<String>()
    private val listaRegistro = mutableListOf<String>()
    private lateinit var adaptador: ArrayAdapter<String>
    private var documentoId: String? = null
    private lateinit var spCursos: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crud)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        firebase = FirebaseFirestore.getInstance()
        txt_nombre = findViewById(R.id.txt_nombre)
        txt_apellido = findViewById(R.id.txt_apellido)
        txt_apodo = findViewById(R.id.txt_apodo)
        txt_edad = findViewById(R.id.txt_edad)
        spCursos = findViewById(R.id.spinner_curso)

        adaptador = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaRegistro)
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCursos.adapter = adaptador

        cargarcomboCursos()
    }

    fun crearAlumno(view: View) {
        val name = txt_nombre.text.toString().trim()
        val apellido = txt_apellido.text.toString().trim()
        val apodo = txt_apodo.text.toString().trim()
        val edad = txt_edad.text.toString().trim()
        val categoriaSeleccionada = spCursos.selectedItem?.toString()?.trim() ?: ""

        if (name.isEmpty() || apellido.isEmpty() || apodo.isEmpty() || edad.isEmpty() || categoriaSeleccionada.isEmpty()) {
            mostrarAlerta("Error", "Completa todos los campos y selecciona un curso.")
            return
        }

        val categoriaObj = listaCursos.find { it.idCurso == categoriaSeleccionada }
        val idcurso = categoriaObj?.idCurso ?: ""

        val data = hashMapOf(
            "nombre" to name,
            "apellido" to apellido,
            "apodo" to apodo,
            "edad" to edad,
            "idCurso" to idcurso
        )

        if (documentoId == null) {
            firebase.collection("Alumnos")
                .document(apodo)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        mostrarAlerta("Error", "Ya existe un alumno con ese apodo.")
                        txt_apodo.text.clear()
                    } else {
                        firebase.collection("Alumnos")
                            .document(apodo)
                            .set(data)
                            .addOnSuccessListener {
                                documentoId = apodo
                                mostrarAlerta("Éxito", "Alumno $name registrado con apodo '$apodo'.")
                                limpiarForm()

                                // ===== AGREGADO: Compat con campos *Alumno e int =====
                                val dataCompat = hashMapOf(
                                    "nombreAlumno" to name,
                                    "apellidoAlumno" to apellido,
                                    "apodoAlumno" to apodo,
                                    "edadAlumno" to (edad.toIntOrNull() ?: 0),
                                    "idCurso" to idcurso
                                )
                                firebase.collection("Alumnos")
                                    .document(apodo)
                                    .set(dataCompat, SetOptions.merge())
                                // ===== FIN AGREGADO =====
                            }
                            .addOnFailureListener { e ->
                                mostrarAlerta("Error", e.message ?: "No se pudo registrar.")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    mostrarAlerta("Error", e.message ?: "Error al verificar duplicados.")
                }
        } else {
            mostrarAlerta("Aviso", "Estás en modo edición. Usa Editar.")
        }
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        val b = AlertDialog.Builder(this)
        b.setTitle(titulo)
        b.setMessage(mensaje)
        b.setPositiveButton("Aceptar", null)
        b.create().show()
    }

    fun limpiarForm() {
        txt_nombre.text.clear()
        txt_apellido.text.clear()
        txt_apodo.text.clear()
        txt_edad.text.clear()
        spCursos.setSelection(0)
    }

    private fun cargarcomboCursos() {
        firebase.collection("Cursos").get().addOnSuccessListener { result ->
            listaCursos.clear()
            listaRegistro.clear()
            for (document in result) {
                val curso = Curso(
                    document.getString("gradoCurso"),
                    document.getString("escuelaCurso"),
                    document.getString("idCurso")
                )

                // ===== AGREGADO: asegurar idCurso y llenar listaIds =====
                if (curso.idCurso.isNullOrBlank()) {
                    curso.idCurso = curso.nivel
                }
                listaIds.add(curso.idCurso ?: "")
                // ===== FIN AGREGADO =====

                listaCursos.add(curso)
                listaRegistro.add(curso.nivel ?: "")
            }
            adaptador.notifyDataSetChanged()
        }
    }
}
