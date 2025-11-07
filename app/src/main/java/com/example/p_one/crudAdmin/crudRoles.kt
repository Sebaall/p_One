package com.example.p_one.crudAdmin

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.Models.Rol
import com.example.p_one.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class crudRoles : AppCompatActivity() {

    private lateinit var firebase: FirebaseFirestore
    private lateinit var txtNombreRol: EditText
    private lateinit var txtDescripcionRol: EditText
    private lateinit var chipGroupPermisos: ChipGroup
    private lateinit var chipAlumnos: Chip
    private lateinit var chipProfesor: Chip
    private lateinit var chipAdmin: Chip

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

        txtNombreRol = findViewById(R.id.txtNombreRol)
        txtDescripcionRol = findViewById(R.id.txtDescripcionRol)
        chipGroupPermisos = findViewById(R.id.chipGroupPermisos)
        chipAlumnos = findViewById(R.id.chipAlumnos)
        chipProfesor = findViewById(R.id.chipProfesor)
        chipAdmin = findViewById(R.id.chipAdmin)
    }

    // Enlázala desde el XML con android:onClick="crearRol" si quieres
    fun crearRol(view: View) {
        val nombre = txtNombreRol.text.toString().trim()
        val descripcion = txtDescripcionRol.text.toString().trim()

        if (nombre.isEmpty() || descripcion.isEmpty()) {
            mostrarAlerta("Error", "Ingresa nombre y descripción.")
            return
        }

        // Permisos desde chips en CÓDIGO
        val permisos = mutableListOf<String>()
        if (chipAlumnos.isChecked) permisos.add("MENU_ALUMNOS")
        if (chipProfesor.isChecked) permisos.add("MENU_PROFESOR")
        if (chipAdmin.isChecked)    permisos.add("MENU_ADMIN")

        if (permisos.isEmpty()) {
            mostrarAlerta("Permisos", "Selecciona al menos un menú.")
            return
        }

        // nivelAcceso derivado (oculto)
        val nivelAcceso = if (permisos.contains("MENU_ADMIN")) 3
        else if (permisos.contains("MENU_PROFESOR")) 2
        else 1

        // creadoPor desde el correo logeado, sanitizado (sin el carácter 64)
        val correoRaw = FirebaseAuth.getInstance().currentUser?.email ?: "desconocido"
        val creadoPorSan = correoRaw.replace(Char(64).toString(), "(at)")

        // id autogenerado + fecha
        val rolesRef = firebase.collection("Roles")
        val docRef = rolesRef.document()
        val idAuto = docRef.id
        val fecha = com.google.firebase.Timestamp.now()

        val rol = Rol(
            idRol = idAuto,
            nombreRol = nombre,
            descripcionRol = descripcion,
            nivelAcceso = nivelAcceso,
            permisos = permisos,
            fechaCreacion = fecha,
            creadoPor = creadoPorSan
        )

        // === Validar duplicado de nombreRol (MISMO ESTILO QUE crudAlumno) ===
        firebase.collection("Roles")
            .whereEqualTo("nombreRol", nombre)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    mostrarAlerta("Error", "Ya existe un rol con ese nombre.")
                    txtNombreRol.text.clear()
                } else {
                    // Guardar
                    docRef.set(rol)
                        .addOnSuccessListener {
                            mostrarAlerta("Éxito", "Rol '$nombre' creado correctamente.")
                            limpiarForm()
                        }
                        .addOnFailureListener { e ->
                            mostrarAlerta("Error", e.message ?: "No se pudo guardar el rol.")
                        }
                }
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", e.message ?: "Error al verificar duplicados.")
            }
    }

    private fun limpiarForm() {
        txtNombreRol.text.clear()
        txtDescripcionRol.text.clear()
        for (i in 0 until chipGroupPermisos.childCount) {
            val c = chipGroupPermisos.getChildAt(i)
            if (c is Chip) c.isChecked = false
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
