package com.example.p_one.crudAdmin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.EditCrud.listcrudProfesor
import com.example.p_one.Models.Curso
import com.example.p_one.Models.Users
import com.example.p_one.R
import com.example.p_one.listPuntuacionProfe
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class crudProfesor : AppCompatActivity() {

    private lateinit var firebase: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var txtNombre: TextInputEditText
    private lateinit var txtApellido: TextInputEditText
    private lateinit var txtCorreo: TextInputEditText
    private lateinit var txtContrasena: TextInputEditText

    private lateinit var btnSeleccionarCursos: MaterialButton
    private lateinit var tvCursosSeleccionados: TextView

    private val listaCursos = mutableListOf<Curso>()
    private val listaIdsCursos = mutableListOf<String>()
    private val listaLabelsCursos = mutableListOf<String>()

    // aquí guardamos los IDs de cursos que el profe tendrá asignados (1 o más)
    private val cursosSeleccionadosIds = mutableSetOf<String>()

    private var documentoId: String? = null   // por si después quieres modo edición

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crud_profesor)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebase = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        txtNombre = findViewById(R.id.txt_nombre_prof)
        txtApellido = findViewById(R.id.txt_apellido_prof)
        txtCorreo = findViewById(R.id.txt_correo_prof)
        txtContrasena = findViewById(R.id.txt_contrasena_prof)

        btnSeleccionarCursos = findViewById(R.id.btnSeleccionarCursosProf)
        tvCursosSeleccionados = findViewById(R.id.tvCursosSeleccionadosProf)

        btnSeleccionarCursos.setOnClickListener {
            mostrarDialogoSeleccionCursos()
        }

        cargarCursos()
    }

    fun crearProfesor(view: View) {
        val nombre = txtNombre.text?.toString()?.trim().orEmpty()
        val apellido = txtApellido.text?.toString()?.trim().orEmpty()
        val correo = txtCorreo.text?.toString()?.trim().orEmpty()
        val contrasena = txtContrasena.text?.toString()?.trim().orEmpty()

        if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty() || contrasena.isEmpty()) {
            mostrarAlerta("Error", "Completa todos los campos.")
            return
        }

        if (cursosSeleccionadosIds.isEmpty()) {
            mostrarAlerta("Error", "Selecciona al menos un curso para el profesor.")
            return
        }

        if (documentoId != null) {
            mostrarAlerta("Aviso", "Estás en modo edición. Usa Editar profesor.")
            return
        }

        // Crear usuario en Auth
        auth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""

                val cursosAsignados = cursosSeleccionadosIds.toList()

                val profesor = Users(
                    uidAuth = uid,
                    rol = "Profesor",
                    activo = false,
                    nombre = nombre,
                    apellido = apellido,
                    correo = correo,

                    // exclusivos profesor
                    idProfesor = uid,
                    cursosAsignados = cursosAsignados,

                    // roles y permisos
                    roles = listOf("MENU_PROFESOR"),
                    nivelAcceso = 2,

                    // auditoría
                    emailVerificado = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = null
                )

                firebase.collection("users")
                    .document(uid)
                    .set(profesor, SetOptions.merge())
                    .addOnSuccessListener {
                        val u = result.user
                        auth.setLanguageCode("es")
                        u?.sendEmailVerification()
                            ?.addOnCompleteListener { t ->
                                // Actualizar cursos en la colección "Cursos" para dejar profesorId = uid
                                actualizarCursosDeProfesor(uid)

                                if (t.isSuccessful) {
                                    mostrarAlerta(
                                        "Éxito",
                                        "Profesor $nombre creado. Se envió verificación a su correo y se asignaron los cursos."
                                    )
                                } else {
                                    mostrarAlerta(
                                        "Aviso",
                                        "Profesor creado y cursos asignados, pero no se pudo enviar la verificación."
                                    )
                                }
                                documentoId = uid
                                limpiarFormProfesor()
                            }
                    }
                    .addOnFailureListener { e ->
                        mostrarAlerta(
                            "Error",
                            e.message ?: "No se pudo guardar el profesor en users."
                        )
                    }
            }
            .addOnFailureListener { e ->
                if (e is FirebaseAuthUserCollisionException) {
                    mostrarAlerta("Error", "El correo ya está registrado.")
                } else {
                    mostrarAlerta("Error", e.message ?: "No se pudo crear el usuario en Auth.")
                }
            }
    }

    fun curdprofe(view: View) {
        startActivity(Intent(this, listcrudProfesor::class.java))
    }

    private fun cargarCursos() {
        firebase.collection("Cursos")
            .get()
            .addOnSuccessListener { result ->
                listaCursos.clear()
                listaLabelsCursos.clear()
                listaIdsCursos.clear()

                for (document in result) {
                    val curso = Curso(
                        idCurso = document.getString("idCurso") ?: document.id,
                        nombreCurso = document.getString("nombreCurso"),
                        nivel = document.getString("nivel"),
                        profesorId = document.getString("profesorId")
                    )

                    val label = when {
                        !curso.nombreCurso.isNullOrBlank() && !curso.nivel.isNullOrBlank() ->
                            "${curso.nombreCurso} – ${curso.nivel}"
                        !curso.nombreCurso.isNullOrBlank() -> curso.nombreCurso!!
                        !curso.nivel.isNullOrBlank() -> curso.nivel!!
                        else -> "Curso sin nombre"
                    }

                    listaCursos.add(curso)
                    listaLabelsCursos.add(label)
                    listaIdsCursos.add(curso.idCurso ?: document.id)
                }

                // si ya están seleccionados, refrescamos el texto mostrado
                actualizarTextoCursosSeleccionados()
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", e.message ?: "No se pudo cargar cursos.")
            }
    }

    private fun mostrarDialogoSeleccionCursos() {
        if (listaLabelsCursos.isEmpty()) {
            mostrarAlerta("Aviso", "No hay cursos disponibles para asignar.")
            return
        }

        val labelsArray = listaLabelsCursos.toTypedArray()
        val checkedItems = BooleanArray(listaIdsCursos.size) { index ->
            cursosSeleccionadosIds.contains(listaIdsCursos[index])
        }

        AlertDialog.Builder(this)
            .setTitle("Selecciona cursos")
            .setMultiChoiceItems(labelsArray, checkedItems) { _, which, isChecked ->
                val idCurso = listaIdsCursos[which]
                if (isChecked) {
                    cursosSeleccionadosIds.add(idCurso)
                } else {
                    cursosSeleccionadosIds.remove(idCurso)
                }
            }
            .setPositiveButton("Aceptar") { dialog, _ ->
                actualizarTextoCursosSeleccionados()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .create()
            .show()
    }

    private fun actualizarTextoCursosSeleccionados() {
        if (cursosSeleccionadosIds.isEmpty()) {
            tvCursosSeleccionados.text = "Sin cursos seleccionados"
        } else {
            val nombresSeleccionados = mutableListOf<String>()
            for (i in listaIdsCursos.indices) {
                val id = listaIdsCursos[i]
                if (cursosSeleccionadosIds.contains(id)) {
                    nombresSeleccionados.add(listaLabelsCursos[i])
                }
            }
            tvCursosSeleccionados.text = nombresSeleccionados.joinToString("\n")
        }
    }

    private fun limpiarFormProfesor() {
        txtNombre.setText("")
        txtApellido.setText("")
        txtCorreo.setText("")
        txtContrasena.setText("")
        cursosSeleccionadosIds.clear()
        actualizarTextoCursosSeleccionados()
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        val b = AlertDialog.Builder(this)
        b.setTitle(titulo)
        b.setMessage(mensaje)
        b.setPositiveButton("Aceptar", null)
        b.create().show()
    }

    // --- NUEVO: actualiza los documentos de "Cursos" con el profesor creado ---
    private fun actualizarCursosDeProfesor(idProfesor: String) {
        if (cursosSeleccionadosIds.isEmpty()) return

        for (idCurso in cursosSeleccionadosIds) {
            firebase.collection("Cursos")
                .document(idCurso)
                .update("profesorId", idProfesor)
                .addOnFailureListener { e ->
                    // Si quieres puedes mostrar algo, pero lo dejo silencioso para no spamear alertas
                    // mostrarAlerta("Aviso", "No se pudo actualizar el curso $idCurso: ${e.message}")
                }
        }
    }
}
