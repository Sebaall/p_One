package com.example.p_one.EditCrud

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.Models.Curso
import com.example.p_one.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class crudProfesorEdit : AppCompatActivity() {

    private lateinit var firebase: FirebaseFirestore

    private lateinit var txtNombreProf: TextInputEditText
    private lateinit var txtApellidoProf: TextInputEditText
    private lateinit var tvCursosSeleccionadosProf: MaterialTextView
    private lateinit var btnSeleccionarCursos: MaterialButton
    private lateinit var tvCorreoProf: MaterialTextView
    private lateinit var txtContrasenaProf: TextInputEditText

    private var documentoId: String? = null

    private var nombreOriginal = ""
    private var apellidoOriginal = ""
    private var cursosOriginal = emptyList<String>()
    private var correoOriginal = ""

    private val listaCursos = mutableListOf<Curso>()
    private val listaLabelsCursos = mutableListOf<String>()
    private val listaIdsCursos = mutableListOf<String>()
    private val cursosSeleccionadosIds = mutableSetOf<String>()

    private val client = OkHttpClient()
    private val BASE_URL = "https://pone-backend-kz8c.onrender.com"

    private val URL_CAMBIAR_CLAVE =
        "$BASE_URL/cambiarPasswordUsuario"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crud_profesor_edit)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }

        firebase = FirebaseFirestore.getInstance()

        txtNombreProf = findViewById(R.id.txt_nombre_prof)
        txtApellidoProf = findViewById(R.id.txt_apellido_prof)
        tvCursosSeleccionadosProf = findViewById(R.id.tvCursosSeleccionadosProf)
        btnSeleccionarCursos = findViewById(R.id.btnSeleccionarCursosProfEdit)
        tvCorreoProf = findViewById(R.id.tvCorreoProf)
        txtContrasenaProf = findViewById(R.id.txt_contrasena_profesor)

        documentoId = intent.getStringExtra("docId")

        nombreOriginal = intent.getStringExtra("nombre") ?: ""
        apellidoOriginal = intent.getStringExtra("apellido") ?: ""
        cursosOriginal = intent.getStringArrayListExtra("cursosAsignados") ?: emptyList()
        correoOriginal = intent.getStringExtra("correo") ?: ""

        txtNombreProf.setText(nombreOriginal)
        txtApellidoProf.setText(apellidoOriginal)
        tvCorreoProf.text = "Correo: $correoOriginal"

        cursosSeleccionadosIds.clear()
        cursosSeleccionadosIds.addAll(cursosOriginal)

        btnSeleccionarCursos.setOnClickListener {
            mostrarDialogoCursos()
        }

        if (documentoId.isNullOrEmpty()) {
            mostrarAlerta("Error", "No se encontró el profesor a editar.")
            finish()
        }

        cargarCursos()
    }

    private fun cargarCursos() {
        firebase.collection("Cursos")
            .get()
            .addOnSuccessListener { result ->
                listaCursos.clear()
                listaIdsCursos.clear()
                listaLabelsCursos.clear()

                for (doc in result) {
                    val curso = Curso(
                        idCurso = doc.getString("idCurso") ?: doc.id,
                        nombreCurso = doc.getString("nombreCurso"),
                        nivel = doc.getString("nivel"),
                        profesorId = doc.getString("profesorId")
                    )

                    val label = "${curso.nombreCurso} ${curso.nivel ?: ""}"

                    listaCursos.add(curso)
                    listaIdsCursos.add(curso.idCurso!!)
                    listaLabelsCursos.add(label.trim())
                }

                actualizarTextoCursos()
            }
            .addOnFailureListener {
                mostrarAlerta("Error", "No se pudieron cargar los cursos.")
            }
    }

    private fun mostrarDialogoCursos() {
        if (listaIdsCursos.isEmpty()) {
            mostrarAlerta("Aviso", "No hay cursos disponibles.")
            return
        }

        val labelsArray = listaLabelsCursos.toTypedArray()

        val checkedItems = BooleanArray(listaIdsCursos.size) { index ->
            cursosSeleccionadosIds.contains(listaIdsCursos[index])
        }

        AlertDialog.Builder(this)
            .setTitle("Seleccionar cursos")
            .setMultiChoiceItems(labelsArray, checkedItems) { _, which, isChecked ->
                val idCurso = listaIdsCursos[which]
                if (isChecked) cursosSeleccionadosIds.add(idCurso)
                else cursosSeleccionadosIds.remove(idCurso)
            }
            .setPositiveButton("Aceptar") { dialog, _ ->
                actualizarTextoCursos()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarTextoCursos() {
        if (cursosSeleccionadosIds.isEmpty()) {
            tvCursosSeleccionadosProf.text = "Sin cursos seleccionados"
            return
        }

        val nombres = cursosSeleccionadosIds.mapNotNull { id ->
            val index = listaIdsCursos.indexOf(id)
            if (index != -1) listaLabelsCursos[index] else null
        }

        tvCursosSeleccionadosProf.text = nombres.joinToString("\n")
    }

    fun editarProfesor(view: View) {
        val id = documentoId ?: return

        val nombreNuevo = txtNombreProf.text.toString().trim()
        val apellidoNuevo = txtApellidoProf.text.toString().trim()
        val contrasenaNueva = txtContrasenaProf.text?.toString()?.trim().orEmpty()

        val datos = mutableMapOf<String, Any>()

        if (nombreNuevo != nombreOriginal) datos["nombre"] = nombreNuevo
        if (apellidoNuevo != apellidoOriginal) datos["apellido"] = apellidoNuevo

        datos["cursosAsignados"] = cursosSeleccionadosIds.toList()

        if (datos.isNotEmpty() || contrasenaNueva.isNotEmpty()) {
            datos["updatedAt"] = System.currentTimeMillis()
        }

        if (datos.isEmpty() && contrasenaNueva.isEmpty()) {
            mostrarAlerta("Aviso", "No hay cambios para guardar.")
            return
        }

        firebase.collection("users")
            .document(id)
            .update(datos)
            .addOnSuccessListener {
                if (contrasenaNueva.isNotEmpty()) {
                    cambiarClaveBackend(id, contrasenaNueva) { ok, mensaje ->
                        runOnUiThread {
                            if (ok) {
                                mostrarAlerta("Éxito", "Profesor actualizado y contraseña cambiada.")
                            } else {
                                mostrarAlerta(
                                    "Aviso",
                                    "Datos actualizados, pero la contraseña no se pudo cambiar.\n$mensaje"
                                )
                            }

                            Handler(Looper.getMainLooper()).postDelayed({
                                val intent = Intent(this, listcrudProfesor::class.java)
                                startActivity(intent)
                                finish()
                            }, 2500)
                        }
                    }
                } else {
                    mostrarAlerta("Éxito", "Profesor actualizado correctamente.")
                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this, listcrudProfesor::class.java)
                        startActivity(intent)
                        finish()
                    }, 2500)
                }
            }
            .addOnFailureListener {
                mostrarAlerta("Error", it.message ?: "No se pudo actualizar el profesor.")
            }
    }

    private fun cambiarClaveBackend(
        idUsuario: String,
        nuevaClave: String,
        callback: (Boolean, String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("idUsuario", idUsuario)
            put("nuevaPassword", nuevaClave)
        }

        val body = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(URL_CAMBIAR_CLAVE)
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

    fun cancelarEdicion(view: View) {
        finish()
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .show()
    }
}
