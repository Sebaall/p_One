package com.example.p_one.AdminMenu.ListCrudAdmin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.AdminMenu.CrudAdmin.crudAlumno
import com.example.p_one.AdminMenu.CrudAdmin.crudCursos
import com.example.p_one.AdminMenu.EditCrudAdmin.crudCursoEditar
import com.example.p_one.Models.Curso
import com.example.p_one.R
import com.google.firebase.firestore.FirebaseFirestore

class listcrudCurso : AppCompatActivity() {

    private lateinit var lvCursos: ListView
    private lateinit var db: FirebaseFirestore

    private val listaCursos = mutableListOf<Curso>()
    private lateinit var adapterCursos: CursosAdapter

    private val mapaProfes = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_listcrud_curso)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        lvCursos = findViewById(R.id.lvCursos)

        adapterCursos = CursosAdapter(this)
        lvCursos.adapter = adapterCursos

        cargarMapaProfes {
            cargarCursos()
            configurarEventosLista()
        }
    }
    fun ba(view: View){
        startActivity(Intent(this, crudCursos::class.java))
    }
    private fun cargarMapaProfes(onReady: () -> Unit) {
        db.collection("users")
            .whereEqualTo("rol", "Profesor")
            .get()
            .addOnSuccessListener { snap ->
                mapaProfes.clear()

                for (doc in snap.documents) {
                    val idProfe = doc.getString("idProfesor") ?: doc.id
                    val nombre = (doc.getString("nombre") ?: "") + " " +
                            (doc.getString("apellido") ?: "")

                    val label = nombre.trim().ifEmpty { "Profesor sin nombre" }
                    mapaProfes[idProfe] = label
                }

                onReady()
            }
            .addOnFailureListener {
                onReady()
            }
    }

    private fun cargarCursos() {
        listaCursos.clear()

        db.collection("Cursos")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    mostrarAlerta("Aviso", "No hay cursos registrados.")
                } else {
                    for (doc in snap.documents) {
                        val curso = doc.toObject(Curso::class.java) ?: continue

                        if (curso.idCurso.isNullOrEmpty()) {
                            curso.idCurso = doc.getString("idCurso") ?: doc.id
                        }

                        listaCursos.add(curso)
                    }
                }

                adapterCursos.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", "Error al cargar cursos: ${e.message}")
            }
    }

    private fun configurarEventosLista() {
        lvCursos.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { _, _, position, _ ->

                val curso = listaCursos[position]

                val opciones = arrayOf("Editar", "Eliminar", "Cancelar")

                AlertDialog.Builder(this)
                    .setTitle("Acciones del curso")
                    .setItems(opciones) { dialog, which ->
                        when (which) {
                            0 -> irAEditarCurso(curso)
                            1 -> confirmarEliminarCurso(curso, position)
                            else -> dialog.dismiss()
                        }
                    }.show()

                true
            }
    }

    private fun irAEditarCurso(curso: Curso) {
        val intent = Intent(this, crudCursoEditar::class.java)
        intent.putExtra("docId", curso.idCurso)
        intent.putExtra("nombreCurso", curso.nombreCurso)
        intent.putExtra("nivel", curso.nivel)
        intent.putExtra("profesorId", curso.profesorId)
        startActivity(intent)
    }

    private fun confirmarEliminarCurso(curso: Curso, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar curso")
            .setMessage("¿Seguro que deseas eliminar el curso seleccionado?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarCurso(curso, position)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarCurso(curso: Curso, position: Int) {
        val id = curso.idCurso ?: return

        db.collection("Cursos")
            .document(id)
            .delete()
            .addOnSuccessListener {
                mostrarAlerta("Éxito", "Curso eliminado.")

                listaCursos.removeAt(position)
                adapterCursos.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", "Error al eliminar: ${e.message}")
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

    private inner class CursosAdapter(context: Context) :
        ArrayAdapter<Curso>(context, 0, listaCursos) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val rowView = convertView
                ?: layoutInflater.inflate(R.layout.item_curso, parent, false)

            val tvCurso = rowView.findViewById<TextView>(R.id.tvFilaCurso)
            val tvProfesor = rowView.findViewById<TextView>(R.id.tvFilaProfesor)

            val curso = listaCursos[position]

            val nombreCurso = curso.nombreCurso ?: ""
            val nivel = curso.nivel ?: ""

            val textoCurso = when {
                nombreCurso.isNotEmpty() && nivel.isNotEmpty() -> "${nombreCurso}°$nivel"
                nombreCurso.isNotEmpty() -> nombreCurso
                else -> "Curso sin nombre"
            }

            val profesorId = curso.profesorId
            val textoProfesor = if (!profesorId.isNullOrEmpty()) {
                mapaProfes[profesorId] ?: "Profesor no encontrado"
            } else {
                "Sin profesor"
            }

            tvCurso.text = textoCurso
            tvProfesor.text = textoProfesor

            return rowView
        }
    }
}
