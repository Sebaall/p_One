package com.example.p_one.EditCrud

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.Models.Users
import com.example.p_one.R
import com.google.firebase.firestore.FirebaseFirestore

class listcrudAlumno : AppCompatActivity() {

    private lateinit var lvAlumnos: ListView
    private lateinit var db: FirebaseFirestore

    private val listaAlumnos = mutableListOf<Users>()
    private lateinit var adapterAlumnos: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_listcrud_alumno)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        lvAlumnos = findViewById(R.id.lvAlumnos)

        adapterAlumnos = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )

        lvAlumnos.adapter = adapterAlumnos

        cargarAlumnos()
        configurarEventosLista()
    }

    private fun cargarAlumnos() {
        listaAlumnos.clear()
        adapterAlumnos.clear()

        db.collection("users")
            .whereEqualTo("rol", "Alumno")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    mostrarAlerta("Aviso", "No hay alumnos registrados.")
                } else {
                    for (doc in snap.documents) {
                        val alumno = doc.toObject(Users::class.java) ?: continue

                        alumno.uidAuth = doc.id
                        listaAlumnos.add(alumno)

                        val texto = buildString {
                            append(alumno.nombre ?: "")
                            append(" ")
                            append(alumno.apellido ?: "")
                            if (!alumno.apodoAlumno.isNullOrBlank()) {
                                append("\nApodo: ")
                                append(alumno.apodoAlumno)
                            }
                        }

                        adapterAlumnos.add(texto)
                    }
                }
                adapterAlumnos.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", "Error al cargar alumnos: ${e.message}")
            }
    }

    private fun configurarEventosLista() {
        lvAlumnos.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { _, _, position, _ ->

                val alumno = listaAlumnos[position]

                val opciones = arrayOf("Editar", "Eliminar", "Cancelar")

                AlertDialog.Builder(this)
                    .setTitle("Acciones del alumno")
                    .setItems(opciones) { dialog, which ->
                        when (which) {
                            0 -> irAEditarAlumno(alumno)
                            1 -> confirmarEliminarAlumno(alumno, position)
                            else -> dialog.dismiss()
                        }
                    }.show()

                true
            }
    }

    private fun irAEditarAlumno(alumno: Users) {
        val intent = Intent(this, crudAlumnoEditar::class.java)
        intent.putExtra("docId", alumno.uidAuth)
        intent.putExtra("nombre", alumno.nombre)
        intent.putExtra("apellido", alumno.apellido)
        intent.putExtra("apodo", alumno.apodoAlumno)
        intent.putExtra("correo", alumno.correo)
        intent.putExtra("edad", alumno.edadAlumno ?: 0)
        intent.putExtra("idCurso", alumno.idCurso)
        startActivity(intent)
    }

    private fun confirmarEliminarAlumno(alumno: Users, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar alumno")
            .setMessage("¿Seguro que deseas eliminar a ${alumno.nombre} ${alumno.apellido}?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarAlumno(alumno, position)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarAlumno(alumno: Users, position: Int) {
        val id = alumno.uidAuth ?: return

        db.collection("users")
            .document(id)
            .delete()
            .addOnSuccessListener {
                mostrarAlerta("Éxito", "Alumno eliminado.")
                listaAlumnos.removeAt(position)
                adapterAlumnos.remove(adapterAlumnos.getItem(position))
                adapterAlumnos.notifyDataSetChanged()
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
}
