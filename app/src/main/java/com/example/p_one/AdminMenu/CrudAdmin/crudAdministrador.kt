package com.example.p_one.AdminMenu.CrudAdmin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.AdminMenu.ListCrudAdmin.listcrudAdmin
import com.example.p_one.Models.Users
import com.example.p_one.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class crudAdministrador : AppCompatActivity() {

    private lateinit var firebase: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var txtNombreAdmin: TextInputEditText
    private lateinit var txtApellidoAdmin: TextInputEditText
    private lateinit var txtCorreoAdmin: TextInputEditText
    private lateinit var txtContrasenaAdmin: TextInputEditText

    private var documentoId: String? = null  // por si luego haces modo edición

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crud_administrador)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebase = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        txtNombreAdmin = findViewById(R.id.txt_nombre_admin)
        txtApellidoAdmin = findViewById(R.id.txt_apellido_admin)
        txtCorreoAdmin = findViewById(R.id.txt_correo_admin)
        txtContrasenaAdmin = findViewById(R.id.txt_contrasena_admin)
    }

    fun crearAdministrador(view: View) {
        val nombre = txtNombreAdmin.text?.toString()?.trim().orEmpty()
        val apellido = txtApellidoAdmin.text?.toString()?.trim().orEmpty()
        val correo = txtCorreoAdmin.text?.toString()?.trim().orEmpty()
        val contrasena = txtContrasenaAdmin.text?.toString()?.trim().orEmpty()

        if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty() || contrasena.isEmpty()) {
            mostrarAlerta("Error", "Completa todos los campos.")
            return
        }

        if (documentoId != null) {
            mostrarAlerta("Aviso", "Estás en modo edición. Usa Editar admin.")
            return
        }

        auth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: ""

                val adminUser = Users(
                    uidAuth = uid,
                    rol = "Administrador",
                    activo = false,
                    nombre = nombre,
                    apellido = apellido,
                    correo = correo,

                    idAdmin = uid,
                    // idProfesor, idAlumno quedan nulos por defecto

                    roles = listOf("MENU_ADMIN"),
                    nivelAcceso = 3,

                    emailVerificado = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = null
                )

                firebase.collection("users")
                    .document(uid)
                    .set(adminUser, SetOptions.merge())
                    .addOnSuccessListener {
                        val u = result.user
                        auth.setLanguageCode("es")
                        u?.sendEmailVerification()
                            ?.addOnCompleteListener { t ->
                                if (t.isSuccessful) {
                                    mostrarAlerta(
                                        "Éxito",
                                        "Administrador $nombre creado. Se envió verificación a su correo."
                                    )
                                } else {
                                    mostrarAlerta(
                                        "Aviso",
                                        "Administrador creado, pero no se pudo enviar la verificación."
                                    )
                                }
                                documentoId = uid
                                limpiarFormAdmin()
                            }
                    }
                    .addOnFailureListener { e ->
                        mostrarAlerta(
                            "Error",
                            e.message ?: "No se pudo guardar el administrador en users."
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

    private fun limpiarFormAdmin() {
        txtNombreAdmin.setText("")
        txtApellidoAdmin.setText("")
        txtCorreoAdmin.setText("")
        txtContrasenaAdmin.setText("")
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        val b = AlertDialog.Builder(this)
        b.setTitle(titulo)
        b.setMessage(mensaje)
        b.setPositiveButton("Aceptar", null)
        b.create().show()
    }
    fun curdlist(view: View){
        startActivity(Intent(this, listcrudAdmin::class.java))
    }
}
