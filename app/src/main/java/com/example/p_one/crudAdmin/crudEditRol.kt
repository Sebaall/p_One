package com.example.p_one.crudAdmin

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class crudEditRol : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    private lateinit var spinnerUsuario: Spinner
    private lateinit var spinnerNuevoRol: Spinner

    private lateinit var tvNombreUsuario: TextView
    private lateinit var tvCorreoUsuario: TextView
    private lateinit var tvRolActualUsuario: TextView
    private lateinit var progressEditRol: ProgressBar

    private val listaUserIds = mutableListOf<String>()
    private val listaUserLabels = mutableListOf<String>()

    // listas de roles obtenidos desde "Roles"
    private val listaRolesNombres = mutableListOf<String>()
    private val listaRolesPermisos = mutableListOf<String>()
    private val listaRolesNivelAcceso = mutableListOf<Long>()

    // rol actual del usuario seleccionado (para reflejarlo en el spinner)
    private var rolActualUsuario: String? = null

    // ⚠️ ADMIN QUE NO SE PUEDE EDITAR ROL
    private val adminProtegidoUid = "9o51Mc4SWvZIV02pZOpSACFxJSZ2"
    private val adminProtegidoCorreo = "sebastian.leon1@virginiogomez.cl"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crud_edit_rol)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()

        spinnerUsuario = findViewById(R.id.spinnerUsuario)
        spinnerNuevoRol = findViewById(R.id.spinnerNuevoRol)

        tvNombreUsuario = findViewById(R.id.tvNombreUsuario)
        tvCorreoUsuario = findViewById(R.id.tvCorreoUsuario)
        tvRolActualUsuario = findViewById(R.id.tvRolActualUsuario)
        progressEditRol = findViewById(R.id.progressEditRol)

        cargarUsuariosEnSpinner()
        cargarRolesEnSpinner()

        spinnerUsuario.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position >= 0 && position < listaUserIds.size && listaUserIds[position].isNotEmpty()) {
                    cargarDatosUsuario(listaUserIds[position])
                } else {
                    limpiarInfoUsuario()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun cargarUsuariosEnSpinner() {
        progressEditRol.visibility = View.VISIBLE

        db.collection("users")
            .get()
            .addOnSuccessListener { snap ->
                listaUserIds.clear()
                listaUserLabels.clear()

                for (doc in snap.documents) {
                    try {
                        val uid = doc.id
                        val nombre = (doc.getString("nombre") ?: "").trim()
                        val apellido = (doc.getString("apellido") ?: "").trim()
                        val correo = (doc.getString("correo") ?: "").trim()

                        val label = when {
                            nombre.isNotEmpty() || apellido.isNotEmpty() ->
                                listOf(nombre, apellido)
                                    .filter { it.isNotEmpty() }
                                    .joinToString(" ")
                            correo.isNotEmpty() -> correo
                            else -> uid
                        }

                        listaUserIds.add(uid)
                        listaUserLabels.add(label)
                    } catch (_: Exception) { }
                }

                if (listaUserLabels.isEmpty()) {
                    listaUserLabels.add("No hay usuarios")
                    listaUserIds.add("")
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    listaUserLabels
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerUsuario.adapter = adapter

                progressEditRol.visibility = View.GONE
            }
            .addOnFailureListener {
                progressEditRol.visibility = View.GONE
                mostrarAlerta("Error", "No se pudo cargar usuarios.")
            }
    }

    private fun cargarDatosUsuario(uid: String) {
        progressEditRol.visibility = View.VISIBLE

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    try {
                        val nombre = listOfNotNull(
                            doc.getString("nombre"),
                            doc.getString("apellido")
                        )
                            .filter { !it.isNullOrBlank() }
                            .joinToString(" ")
                            .ifBlank { "-" }

                        val correo = doc.getString("correo") ?: "-"
                        val rol = doc.getString("rol") ?: "-"

                        tvNombreUsuario.text = "Nombre: $nombre"
                        tvCorreoUsuario.text = "Correo: $correo"
                        tvRolActualUsuario.text = "Rol actual: $rol"

                        // guardamos el rol actual del usuario para reflejarlo en el spinner
                        rolActualUsuario = rol
                        seleccionarRolActualEnSpinner()

                    } catch (_: Exception) {
                        limpiarInfoUsuario()
                    }
                } else {
                    limpiarInfoUsuario()
                }

                progressEditRol.visibility = View.GONE
            }
            .addOnFailureListener {
                progressEditRol.visibility = View.GONE
                limpiarInfoUsuario()
                mostrarAlerta("Error", "No se pudo cargar los datos.")
            }
    }

    private fun cargarRolesEnSpinner() {
        listaRolesNombres.clear()
        listaRolesPermisos.clear()
        listaRolesNivelAcceso.clear()

        db.collection("Roles")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    listaRolesNombres.addAll(listOf("Alumno", "Profesor", "Administrador"))
                    listaRolesPermisos.addAll(listOf("MENU_ALUMNOS", "MENU_PROFESOR", "MENU_ADMIN"))
                    listaRolesNivelAcceso.addAll(listOf(1L, 2L, 3L))
                } else {
                    for (doc in snap.documents) {
                        val nombreRol = doc.getString("nombreRol") ?: continue
                        val permisos = doc.get("permisos") as? List<*> ?: emptyList<Any>()
                        val permisoClave = (permisos.firstOrNull() as? String) ?: "MENU_ALUMNOS"
                        val nivelAcceso = doc.getLong("nivelAcceso") ?: 1L

                        listaRolesNombres.add(nombreRol)
                        listaRolesPermisos.add(permisoClave)
                        listaRolesNivelAcceso.add(nivelAcceso)
                    }

                    if (listaRolesNombres.isEmpty()) {
                        listaRolesNombres.addAll(listOf("Alumno", "Profesor", "Administrador"))
                        listaRolesPermisos.addAll(listOf("MENU_ALUMNOS", "MENU_PROFESOR", "MENU_ADMIN"))
                        listaRolesNivelAcceso.addAll(listOf(1L, 2L, 3L))
                    }
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    listaRolesNombres
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerNuevoRol.adapter = adapter

                // cuando ya tenemos la lista de roles, intentamos seleccionar el rol actual
                seleccionarRolActualEnSpinner()
            }
            .addOnFailureListener {
                listaRolesNombres.clear()
                listaRolesPermisos.clear()
                listaRolesNivelAcceso.clear()

                listaRolesNombres.addAll(listOf("Alumno", "Profesor", "Administrador"))
                listaRolesPermisos.addAll(listOf("MENU_ALUMNOS", "MENU_PROFESOR", "MENU_ADMIN"))
                listaRolesNivelAcceso.addAll(listOf(1L, 2L, 3L))

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    listaRolesNombres
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerNuevoRol.adapter = adapter

                seleccionarRolActualEnSpinner()
            }
    }

    // ajusta el spinner de roles al rolActualUsuario (si existe en la lista)
    private fun seleccionarRolActualEnSpinner() {
        val rol = rolActualUsuario ?: return
        if (listaRolesNombres.isEmpty()) return

        val idx = listaRolesNombres.indexOfFirst { it.equals(rol, ignoreCase = true) }
        if (idx >= 0) {
            spinnerNuevoRol.setSelection(idx)
        }
    }

    private fun limpiarInfoUsuario() {
        tvNombreUsuario.text = "Nombre: -"
        tvCorreoUsuario.text = "Correo: -"
        tvRolActualUsuario.text = "Rol actual: -"
        rolActualUsuario = null
        // si quieres, también puedes resetear el spinner de rol:
        // if (listaRolesNombres.isNotEmpty()) spinnerNuevoRol.setSelection(0)
    }

    fun aplicarRolUsuarioOnClick(view: View) {
        val idxUser = spinnerUsuario.selectedItemPosition

        if (idxUser < 0 || idxUser >= listaUserIds.size || listaUserIds[idxUser].isEmpty()) {
            mostrarAlerta("Error", "Selecciona un usuario válido.")
            return
        }

        val uidUser = listaUserIds[idxUser]

        // ⚠️ BLOQUEO DEL ADMIN PROTEGIDO
        val correoTexto = tvCorreoUsuario.text.toString()
        if (uidUser == adminProtegidoUid || correoTexto.contains(adminProtegidoCorreo)) {
            mostrarAlerta("Acceso denegado", "No se le puede cambiar el rol a este administrador.")
            return
        }

        val idxRol = spinnerNuevoRol.selectedItemPosition
        if (idxRol < 0 || idxRol >= listaRolesNombres.size) {
            mostrarAlerta("Error", "Selecciona un rol válido.")
            return
        }

        val rolTexto = listaRolesNombres[idxRol]
        val permisoClave = listaRolesPermisos[idxRol]
        val nivelAcceso = listaRolesNivelAcceso[idxRol]

        progressEditRol.visibility = View.VISIBLE

        val updates = hashMapOf<String, Any>(
            "rol" to rolTexto,
            "roles" to listOf(permisoClave),
            "nivelAcceso" to nivelAcceso,
            "updatedAt" to System.currentTimeMillis()
        )

        when (permisoClave) {
            "MENU_ADMIN" -> {
                updates["idAdmin"] = uidUser
                updates["idAlumno"] = FieldValue.delete()
                updates["idProfesor"] = FieldValue.delete()
            }
            "MENU_PROFESOR" -> {
                updates["idProfesor"] = uidUser
                updates["idAlumno"] = FieldValue.delete()
                updates["idAdmin"] = FieldValue.delete()
            }
            "MENU_ALUMNOS" -> {
                updates["idAlumno"] = uidUser
                updates["idProfesor"] = FieldValue.delete()
                updates["idAdmin"] = FieldValue.delete()
            }
        }

        db.collection("users")
            .document(uidUser)
            .update(updates)
            .addOnSuccessListener {
                progressEditRol.visibility = View.GONE
                tvRolActualUsuario.text = "Rol actual: $rolTexto"
                rolActualUsuario = rolTexto
                mostrarAlerta("Listo", "Rol actualizado correctamente.")
            }
            .addOnFailureListener {
                progressEditRol.visibility = View.GONE
                mostrarAlerta("Error", "No se pudo actualizar el rol.")
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
