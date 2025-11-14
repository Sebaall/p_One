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
import com.example.p_one.crudAdmin.crudAdministradorEditar
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class listcrudAdmin : AppCompatActivity() {

    private lateinit var lvAdmins: ListView
    private lateinit var db: FirebaseFirestore

    private val listaAdmins = mutableListOf<Users>()
    private lateinit var adapterAdmins: ArrayAdapter<String>

    // ⚠️ ADMIN QUE NO SE PUEDE ELIMINAR
    private val adminProtegidoUid = "9o51Mc4SWvZIV02pZOpSACFxJSZ2"
    private val adminProtegidoCorreo = "sebastian.leon1@virginiogomez.cl"

    // ---------------- BACKEND NODE ----------------
    private val client = OkHttpClient()
    private val BASE_URL = "https://pone-backend-kz8c.onrender.com"

    private val URL_ELIMINAR_USUARIO =
        "$BASE_URL/eliminarUsuarioCompleto"
    // ------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_listcrud_admin)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        lvAdmins = findViewById(R.id.lvAdmins)

        adapterAdmins = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf<String>()
        )
        lvAdmins.adapter = adapterAdmins

        cargarAdministradores()
        configurarEventosLista()
    }

    private fun cargarAdministradores() {
        listaAdmins.clear()
        adapterAdmins.clear()

        db.collection("users")
            .whereEqualTo("rol", "Administrador")
            .get()
            .addOnSuccessListener { snap ->

                if (snap.isEmpty) {
                    mostrarAlerta("Aviso", "No hay administradores registrados.")
                } else {
                    for (doc in snap.documents) {
                        val admin = doc.toObject(Users::class.java) ?: continue
                        admin.uidAuth = doc.id

                        listaAdmins.add(admin)

                        val texto = "${admin.nombre} ${admin.apellido}\n${admin.correo}"
                        adapterAdmins.add(texto)
                    }
                }

                adapterAdmins.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", "Error al cargar admins: ${e.message}")
            }
    }

    private fun configurarEventosLista() {
        lvAdmins.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { _, _, position, _ ->

                val admin = listaAdmins[position]

                val opciones = arrayOf("Editar", "Eliminar", "Cancelar")

                AlertDialog.Builder(this)
                    .setTitle("Acciones del administrador")
                    .setItems(opciones) { dialog, which ->
                        when (which) {
                            0 -> irAEditarAdmin(admin)
                            1 -> confirmarEliminarAdmin(admin, position)
                            else -> dialog.dismiss()
                        }
                    }.show()

                true
            }
    }

    private fun irAEditarAdmin(admin: Users) {
        val intent = Intent(this, crudAdministradorEditar::class.java)
        intent.putExtra("docId", admin.uidAuth)
        intent.putExtra("nombre", admin.nombre)
        intent.putExtra("apellido", admin.apellido)
        intent.putExtra("correo", admin.correo)
        startActivity(intent)
    }

    // ⚠️ CHEQUEA SI ES EL ADMIN BLOQUEADO
    private fun esAdminProtegido(admin: Users): Boolean {
        val uid = admin.uidAuth ?: ""
        val correo = admin.correo ?: ""

        return uid == adminProtegidoUid || correo == adminProtegidoCorreo
    }

    private fun confirmarEliminarAdmin(admin: Users, position: Int) {

        // 🚫 Bloquear eliminación del admin especial
        if (esAdminProtegido(admin)) {
            mostrarAlerta("Aviso", "No puedes eliminar este administrador.")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar administrador")
            .setMessage("¿Seguro que deseas eliminar a ${admin.nombre} ${admin.apellido}?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarAdmin(admin, position)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarAdmin(admin: Users, position: Int) {
        val id = admin.uidAuth ?: return

        eliminarUsuarioCompletoBackend(id) { ok, mensaje ->
            runOnUiThread {
                if (ok) {
                    val textoItem = adapterAdmins.getItem(position)
                    if (textoItem != null) adapterAdmins.remove(textoItem)

                    listaAdmins.removeAt(position)
                    adapterAdmins.notifyDataSetChanged()

                    mostrarAlerta("Éxito", "Administrador eliminado correctamente.")
                } else {
                    mostrarAlerta("Error", "Error al eliminar: $mensaje")
                }
            }
        }
    }

    // -------------- FUNCIÓN PARA LLAMAR AL BACKEND --------------
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
    // ------------------------------------------------------------

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        val b = AlertDialog.Builder(this)
        b.setTitle(titulo)
        b.setMessage(mensaje)
        b.setPositiveButton("Aceptar", null)
        b.create().show()
    }
}
