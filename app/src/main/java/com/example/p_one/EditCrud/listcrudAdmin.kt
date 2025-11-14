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

class listcrudAdmin : AppCompatActivity() {

    private lateinit var lvAdmins: ListView
    private lateinit var db: FirebaseFirestore

    private val listaAdmins = mutableListOf<Users>()
    private lateinit var adapterAdmins: ArrayAdapter<String>

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

    private fun confirmarEliminarAdmin(admin: Users, position: Int) {
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

        db.collection("users")
            .document(id)
            .delete()
            .addOnSuccessListener {
                mostrarAlerta("Éxito", "Administrador eliminado.")

                listaAdmins.removeAt(position)
                adapterAdmins.remove(adapterAdmins.getItem(position))
                adapterAdmins.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                mostrarAlerta("Error", "Error al eliminar: ${e.message}")
            }
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        val b = AlertDialog.Builder(this)
        b.setTitle(titulo)
        b.setMessage(mensaje)
        b.setPositiveButton("Aceptar", null)
        b.create().show()
    }
}
