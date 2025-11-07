package com.example.p_one.crudAdmin

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
import com.example.p_one.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp

class crudAlumno : AppCompatActivity() {

    private lateinit var firebase: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var txt_nombre: EditText
    private lateinit var txt_apellido: EditText
    private lateinit var txt_apodo: EditText
    private lateinit var txt_edad: EditText
    private lateinit var txt_correo: TextInputEditText
    private lateinit var txt_contrasena: TextInputEditText
    private val listaCursos = mutableListOf<Curso>()
    private val listaIds = mutableListOf<String>()
    private val listaRegistro = mutableListOf<String>()
    private lateinit var adaptador: ArrayAdapter<String>
    private var documentoId: String? = null
    private lateinit var spCursos: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crud_alumno)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebase = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        txt_nombre = findViewById(R.id.txt_nombre)
        txt_apellido = findViewById(R.id.txt_apellido)
        txt_apodo = findViewById(R.id.txt_apodo)
        txt_edad = findViewById(R.id.txt_edad)
        txt_correo = findViewById(R.id.txt_correo)
        txt_contrasena = findViewById(R.id.txt_contrasena)
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
        val edadTxt = txt_edad.text.toString().trim()
        val correo = txt_correo.text.toString().trim()
        val contrasena = txt_contrasena.text.toString().trim()

        if (name.isEmpty() || apellido.isEmpty() || apodo.isEmpty() || edadTxt.isEmpty()
            || correo.isEmpty() || contrasena.isEmpty() || spCursos.selectedItem == null) {
            mostrarAlerta("Error", "Completa todos los campos y selecciona un curso.")
            return
        }

        val edad = edadTxt.toIntOrNull()
        if (edad == null) {
            mostrarAlerta("Error", "La edad debe ser un número.")
            return
        }

        val idx = spCursos.selectedItemPosition
        if (idx !in listaIds.indices) {
            mostrarAlerta("Error", "Curso no válido.")
            return
        }
        val idcurso = listaIds[idx]

        if (documentoId == null) {
            firebase.collection("Alumnos")
                .document(apodo)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        mostrarAlerta("Error", "Ya existe un alumno con ese apodo.")
                        txt_apodo.text.clear()
                    } else {
                        // ===== Correlativo numAlumno (transacción) =====
                        firebase.runTransaction { tx ->
                            val ref = firebase.collection("contadores").document("alumnos")
                            val snap = tx.get(ref)
                            val actual = snap.getLong("seq") ?: 0L
                            val nuevo = actual + 1
                            tx.set(ref, mapOf("seq" to nuevo))
                            nuevo // valor que retorna la transacción
                        }
                            .addOnSuccessListener { numAlumno ->
                                // ===== Crear usuario en Auth (correo único) =====
                                auth.createUserWithEmailAndPassword(correo, contrasena)
                                    .addOnSuccessListener { result ->
                                        val uid = result.user?.uid ?: ""

                                        val alumno = Alumno(
                                            idAlumno = uid,
                                            nombreAlumno = name,
                                            apellidoAlumno = apellido,
                                            apodoAlumno = apodo,
                                            edadAlumno = edad,
                                            idCurso = idcurso,
                                            correoAlumno = correo
                                        )

                                        // Guardar alumno (doc = apodo)
                                        firebase.collection("Alumnos")
                                            .document(apodo)
                                            .set(alumno)
                                            .addOnSuccessListener {
                                                // Guardar numAlumno en el mismo doc
                                                firebase.collection("Alumnos")
                                                    .document(apodo)
                                                    .set(mapOf("numAlumno" to numAlumno), SetOptions.merge())

                                                // Perfil base + rol Alumno + flags de verificación
                                                val dataRol = hashMapOf(
                                                    "nombre" to name,
                                                    "apellido" to apellido,
                                                    "correo" to correo,
                                                    "roles" to listOf("MENU_ALUMNOS"),
                                                    "nivelAcceso" to 1,
                                                    "emailVerificado" to false,
                                                    "activo" to false,
                                                    "createdAt" to Timestamp.now()
                                                )
                                                firebase.collection("users")
                                                    .document(uid)
                                                    .set(dataRol, SetOptions.merge())
                                                    .addOnSuccessListener {
                                                        // Enviar correo de verificación
                                                        val u = result.user
                                                        auth.setLanguageCode("es")
                                                        u?.sendEmailVerification()
                                                            ?.addOnCompleteListener { t ->
                                                                if (t.isSuccessful) {
                                                                    mostrarAlerta(
                                                                        "Éxito",
                                                                        "Alumno $name creado (N° $numAlumno). Se envió verificación a su correo."
                                                                    )
                                                                } else {
                                                                    mostrarAlerta(
                                                                        "Aviso",
                                                                        "Alumno creado, pero no se pudo enviar la verificación. Intenta reenviarla más tarde."
                                                                    )
                                                                }
                                                                documentoId = apodo
                                                                limpiarForm()
                                                            }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        mostrarAlerta("Error", e.message ?: "No se pudo guardar el perfil del alumno.")
                                                    }
                                            }
                                            .addOnFailureListener { e ->
                                                mostrarAlerta("Error", e.message ?: "No se pudo guardar el alumno.")
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        if (e is FirebaseAuthUserCollisionException) {
                                            mostrarAlerta("Error", "El correo ya está registrado.")
                                        } else {
                                            mostrarAlerta("Error", e.message ?: "No se pudo crear el usuario.")
                                        }
                                    }
                            }
                            .addOnFailureListener {
                                mostrarAlerta("Error", "No se pudo generar el correlativo.")
                            }
                        // ===== FIN correlativo + auth =====
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
        txt_correo.setText("")
        txt_contrasena.setText("")
        spCursos.setSelection(0)
    }

    private fun cargarcomboCursos() {
        firebase.collection("Cursos")
            .get()
            .addOnSuccessListener { result ->
                listaCursos.clear()
                listaRegistro.clear()
                listaIds.clear()

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
                    listaRegistro.add(label)
                    listaIds.add(curso.idCurso ?: document.id)
                }

                adaptador.notifyDataSetChanged()
                if (listaRegistro.isNotEmpty()) spCursos.setSelection(0)
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", e.message ?: "No se pudo cargar cursos.")
            }
    }
}
