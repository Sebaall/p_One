package com.example.p_one.AdminMenu.CrudAdmin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.AdminMenu.ListCrudAdmin.listcrudRoles
import com.example.p_one.Models.Rol
import com.example.p_one.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class crudRoles : AppCompatActivity() {

    private lateinit var firebase: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var txtNombreRol: EditText
    private lateinit var txtDescripcionRol: EditText
    private lateinit var tvCreadorNombre: TextView
    private lateinit var spinnerPermisoMenu: Spinner
    private lateinit var progressRol: ProgressBar

    private var uidCreador: String = ""
    private var nombreCreador: String = ""   // <-- nombre del admin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crud_roles)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        firebase = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        txtNombreRol = findViewById(R.id.txtNombreRol)
        txtDescripcionRol = findViewById(R.id.txtDescripcionRol)
        tvCreadorNombre = findViewById(R.id.tvCreadorNombre)
        spinnerPermisoMenu = findViewById(R.id.spinnerPermisoMenu)
        progressRol = findViewById(R.id.progressRol)

        cargarCreadorActual()
        cargarMenusEnSpinner()
    }

    private fun cargarCreadorActual() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            uidCreador = ""
            nombreCreador = ""
            tvCreadorNombre.text = "No identificado"
            return
        }

        uidCreador = uid
        progressRol.visibility = View.VISIBLE

        firebase.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val nombre = doc.getString("nombre")?.trim().orEmpty()
                val apellido = doc.getString("apellido")?.trim().orEmpty()
                val correo = doc.getString("correo")?.trim().orEmpty()

                val display = when {
                    nombre.isNotEmpty() || apellido.isNotEmpty() ->
                        listOf(nombre, apellido).filter { it.isNotEmpty() }.joinToString(" ")
                    correo.isNotEmpty() -> correo
                    else -> uid
                }

                nombreCreador = display.ifEmpty { uid }
                tvCreadorNombre.text = nombreCreador
                progressRol.visibility = View.GONE
            }
            .addOnFailureListener {
                tvCreadorNombre.text = "Desconocido"
                nombreCreador = ""
                progressRol.visibility = View.GONE
            }
    }

    private fun cargarMenusEnSpinner() {
        val opcionesVisibles = listOf(
            "Menú alumnos",
            "Menú profesor",
            "Menú admin"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opcionesVisibles
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPermisoMenu.adapter = adapter
    }

    fun crearRol(view: View) {
        val nombre = txtNombreRol.text.toString().trim()
        val descripcion = txtDescripcionRol.text.toString().trim()

        if (nombre.isEmpty() || descripcion.isEmpty()) {
            mostrarAlerta("Error", "Ingresa nombre y descripción.")
            return
        }

        if (uidCreador.isEmpty()) {
            mostrarAlerta("Error", "No se pudo identificar al creador del rol. Inicia sesión nuevamente.")
            return
        }

        val idxMenu = spinnerPermisoMenu.selectedItemPosition
        if (idxMenu == Spinner.INVALID_POSITION) {
            mostrarAlerta("Permiso", "Selecciona un menú.")
            return
        }

        val permisoClave = when (idxMenu) {
            0 -> "MENU_ALUMNOS"
            1 -> "MENU_PROFESOR"
            2 -> "MENU_ADMIN"
            else -> "MENU_ALUMNOS"
        }

        val permisos = listOf(permisoClave)

        val nivelAcceso = when (permisoClave) {
            "MENU_ADMIN" -> 3
            "MENU_PROFESOR" -> 2
            "MENU_ALUMNOS" -> 1
            else -> 1
        }

        val rolesRef = firebase.collection("Roles")
        val docRef = rolesRef.document()
        val idAuto = docRef.id
        val fecha = Timestamp.now()

        val rol = Rol(
            idRol = idAuto,
            nombreRol = nombre,
            descripcionRol = descripcion,
            nivelAcceso = nivelAcceso,
            permisos = permisos,
            fechaCreacion = fecha,
            creadoPor = nombreCreador   // <-- aquí va el nombre del admin
        )

        progressRol.visibility = View.VISIBLE

        firebase.collection("Roles")
            .whereEqualTo("nombreRol", nombre)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    progressRol.visibility = View.GONE
                    mostrarAlerta("Error", "Ya existe un rol con ese nombre.")
                    txtNombreRol.text.clear()
                } else {
                    docRef.set(rol)
                        .addOnSuccessListener {
                            progressRol.visibility = View.GONE
                            mostrarAlerta("Éxito", "Rol '$nombre' creado correctamente.")
                            limpiarForm()
                        }
                        .addOnFailureListener { e ->
                            progressRol.visibility = View.GONE
                            mostrarAlerta("Error", e.message ?: "No se pudo guardar el rol.")
                        }
                }
            }
            .addOnFailureListener { e ->
                progressRol.visibility = View.GONE
                mostrarAlerta("Error", e.message ?: "Error al verificar duplicados.")
            }
    }

    fun curdlistroles(view: View){
        startActivity(Intent(this, listcrudRoles::class.java))
    }

    private fun limpiarForm() {
        txtNombreRol.text.clear()
        txtDescripcionRol.text.clear()
        if (spinnerPermisoMenu.adapter != null && spinnerPermisoMenu.adapter.count > 0) {
            spinnerPermisoMenu.setSelection(0)
        }
        txtNombreRol.requestFocus()
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        val b = AlertDialog.Builder(this)
        b.setTitle(titulo)
        b.setMessage(mensaje)
        b.setPositiveButton("Aceptar", null)
        b.create().show()
    }
}
