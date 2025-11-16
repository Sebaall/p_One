package com.example.p_one.AdminMenu.ListCrudAdmin

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
import com.example.p_one.AdminMenu.EditCrudAdmin.crudProfesorEdit
import com.example.p_one.Models.Users
import com.example.p_one.R
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class listcrudProfesor : AppCompatActivity() {

    private lateinit var lvProfesores: ListView
    private lateinit var db: FirebaseFirestore

    private val listaProfesores = mutableListOf<Users>()
    private lateinit var adapterProfesores: ArrayAdapter<String>

    private val mapaCursos = mutableMapOf<String, String>()

    private val client = OkHttpClient()
    private val BASE_URL = "https://pone-backend-kz8c.onrender.com"

    private val URL_ELIMINAR_USUARIO =
        "$BASE_URL/eliminarUsuarioCompleto"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_listcrud_profesor)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        lvProfesores = findViewById(R.id.lvProfesores)

        adapterProfesores = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )

        lvProfesores.adapter = adapterProfesores

        cargarMapaCursos {
            cargarProfesores()
            configurarEventosLista()
        }
    }

    private fun cargarMapaCursos(onReady: () -> Unit) {
        db.collection("Cursos")
            .get()
            .addOnSuccessListener { snap ->
                mapaCursos.clear()

                for (doc in snap.documents) {
                    val idCurso = doc.getString("idCurso") ?: doc.id
                    val nombreCurso = doc.getString("nombreCurso") ?: ""
                    val nivel = doc.getString("nivel") ?: ""

                    val label = if (nombreCurso.isNotEmpty() && nivel.isNotEmpty()) {
                        "$nombreCurso°$nivel"
                    } else {
                        nombreCurso.ifEmpty { "Curso sin nombre" }
                    }

                    mapaCursos[idCurso] = label
                }

                onReady()
            }
            .addOnFailureListener {
                onReady()
            }
    }

    private fun cargarProfesores() {
        listaProfesores.clear()
        adapterProfesores.clear()

        db.collection("users")
            .whereEqualTo("rol", "Profesor")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    mostrarAlerta("Aviso", "No hay profesores registrados.")
                } else {
                    for (doc in snap.documents) {
                        val profesor = doc.toObject(Users::class.java) ?: continue

                        profesor.uidAuth = doc.id
                        listaProfesores.add(profesor)

                        val texto = buildString {
                            append(profesor.nombre ?: "")
                            append(" ")
                            append(profesor.apellido ?: "")

                            val cursosIds = profesor.cursosAsignados
                            if (!cursosIds.isNullOrEmpty()) {
                                append("\nCursos: ")

                                val nombres = cursosIds.map { id ->
                                    mapaCursos[id] ?: id
                                }

                                append(nombres.joinToString(", "))
                            }
                        }

                        adapterProfesores.add(texto)
                    }
                }

                adapterProfesores.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", "Error al cargar profesores: ${e.message}")
            }
    }

    private fun configurarEventosLista() {
        lvProfesores.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { _, _, position, _ ->

                val profesor = listaProfesores[position]

                val opciones = arrayOf("Editar", "Eliminar", "Cancelar")

                AlertDialog.Builder(this)
                    .setTitle("Acciones del profesor")
                    .setItems(opciones) { dialog, which ->
                        when (which) {
                            0 -> irAEditarProfesor(profesor)
                            1 -> confirmarEliminarProfesor(profesor, position)
                            else -> dialog.dismiss()
                        }
                    }.show()

                true
            }
    }

    private fun irAEditarProfesor(profesor: Users) {
        val intent = Intent(this, crudProfesorEdit::class.java)
        intent.putExtra("docId", profesor.uidAuth)
        intent.putExtra("nombre", profesor.nombre)
        intent.putExtra("apellido", profesor.apellido)

        val cursosIds = profesor.cursosAsignados ?: emptyList()
        intent.putExtra("cursosAsignados", ArrayList(cursosIds))
        intent.putExtra("correo", profesor.correo)

        startActivity(intent)
    }

    private fun confirmarEliminarProfesor(profesor: Users, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar profesor")
            .setMessage("¿Seguro que deseas eliminar a ${profesor.nombre} ${profesor.apellido}?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarProfesor(profesor, position)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarProfesor(profesor: Users, position: Int) {
        val id = profesor.uidAuth ?: return

        eliminarUsuarioCompletoBackend(id) { ok, mensaje ->
            runOnUiThread {
                if (ok) {
                    val textoItem = adapterProfesores.getItem(position)
                    if (textoItem != null) adapterProfesores.remove(textoItem)

                    listaProfesores.removeAt(position)
                    adapterProfesores.notifyDataSetChanged()

                    mostrarAlerta("Éxito", "Profesor eliminado correctamente.")
                } else {
                    mostrarAlerta("Error", "Error al eliminar: $mensaje")
                }
            }
        }
    }

    private fun eliminarUsuarioCompletoBackend(
        idDocumento: String,
        callback: (Boolean, String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("idUsuario", idDocumento)
        }

        val body = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(URL_ELIMINAR_USUARIO)
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

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .create()
            .show()
    }
}