package com.example.p_one.AdminMenu.ListCrudAdmin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.AdminMenu.CrudAdmin.crudAlumno
import com.example.p_one.AdminMenu.CrudAdmin.crudEditRol
import com.example.p_one.AdminMenu.EditCrudAdmin.crudAlumnoEditar
import com.example.p_one.Models.Curso
import com.example.p_one.Models.Users
import com.example.p_one.R
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class listcrudAlumno : AppCompatActivity() {

    private lateinit var lvAlumnos: ListView
    private lateinit var db: FirebaseFirestore

    private val listaAlumnos = mutableListOf<Users>()
    private val cursosMap = mutableMapOf<String, String>()
    private lateinit var adapterAlumnos: AlumnoAdapter

    private val client = OkHttpClient()
    private val BASE_URL = "https://pone-backend-kz8c.onrender.com"
    private val URL_ELIMINAR_USUARIO = "$BASE_URL/eliminarUsuarioCompleto"

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

        cargarAlumnos()
        configurarEventosLista()
    }

    private fun cargarAlumnos() {
        listaAlumnos.clear()
        cursosMap.clear()

        db.collection("cursos")
            .get()
            .addOnSuccessListener { snapCursos ->
                for (doc in snapCursos.documents) {
                    val curso = doc.toObject(Curso::class.java) ?: continue

                    val idCursoCampo = (curso.idCurso ?: "").trim()
                    val idCursoDoc = doc.id.trim()

                    val nombre = curso.nombreCurso?.trim().orEmpty()
                    val nivel = curso.nivel?.trim().orEmpty()

                    val textoCurso = when {
                        nombre.isNotEmpty() && nivel.isNotEmpty() -> "$nombre $nivel"
                        nombre.isNotEmpty() -> nombre
                        nivel.isNotEmpty() -> nivel
                        else -> idCursoDoc
                    }

                    if (idCursoCampo.isNotEmpty()) {
                        cursosMap[idCursoCampo] = textoCurso
                    }
                    if (idCursoDoc.isNotEmpty()) {
                        cursosMap[idCursoDoc] = textoCurso
                    }
                }

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
                            }

                            adapterAlumnos = AlumnoAdapter(this, listaAlumnos)
                            lvAlumnos.adapter = adapterAlumnos
                        }
                    }
                    .addOnFailureListener { e ->
                        mostrarAlerta("Error", "Error al cargar alumnos: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", "Error al cargar cursos: ${e.message}")
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
    fun curdvolver(view: View){
        startActivity(Intent(this, crudAlumno::class.java))
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

        eliminarUsuarioCompletoBackend(id) { ok, mensaje ->
            runOnUiThread {
                if (ok) {
                    listaAlumnos.removeAt(position)
                    adapterAlumnos.notifyDataSetChanged()
                    mostrarAlerta("Éxito", "Alumno eliminado correctamente.")
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
        val json = JSONObject()
        json.put("idUsuario", idDocumento)

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
                callback(false, e.message ?: "")
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

    inner class AlumnoAdapter(
        private val context: Context,
        private val alumnos: MutableList<Users>
    ) : ArrayAdapter<Users>(context, 0, alumnos) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(
                R.layout.item_alumno,
                parent,
                false
            )

            val alumno = alumnos[position]

            val tvNombre = view.findViewById<TextView>(R.id.tvFilaNombreAlumno)
            val tvApodo = view.findViewById<TextView>(R.id.tvFilaApodoAlumno)
            val tvCurso = view.findViewById<TextView>(R.id.tvFilaCursoAlumno)
            val tvEdad = view.findViewById<TextView>(R.id.tvFilaEdadAlumno)
            val tvCorreo = view.findViewById<TextView>(R.id.tvFilaCorreoAlumno)

            val nombreCompleto = "${alumno.nombre ?: ""} ${alumno.apellido ?: ""}".trim()
            tvNombre.text = nombreCompleto

            tvApodo.text = alumno.apodoAlumno ?: ""

            val idCursoRaw = alumno.idCurso?.trim().orEmpty()
            val idCursoKey = idCursoRaw.substringAfterLast("/").trim()

            val textoCurso = when {
                idCursoKey.isNotEmpty() && cursosMap.containsKey(idCursoKey) -> cursosMap[idCursoKey]
                idCursoRaw.isNotEmpty() && cursosMap.containsKey(idCursoRaw) -> cursosMap[idCursoRaw]
                else -> idCursoRaw
            }

            tvCurso.text = textoCurso

            tvEdad.text = alumno.edadAlumno?.toString() ?: "-"
            tvCorreo.text = alumno.correo ?: ""

            return view
        }
    }
}
