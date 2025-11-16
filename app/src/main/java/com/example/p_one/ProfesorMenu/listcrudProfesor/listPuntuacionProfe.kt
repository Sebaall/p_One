package com.example.p_one

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.Main.Login
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class listPuntuacionProfe : AppCompatActivity() {

    private lateinit var lvPuntuaciones: ListView
    private lateinit var btnVolver: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_list_puntuacion_profe)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()

        lvPuntuaciones = findViewById(R.id.lvPuntuaciones)
        btnVolver = findViewById(R.id.btnVolver)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)

        // HEADER USANDO item_ranking.xml
        val header = layoutInflater.inflate(R.layout.item_ranking, lvPuntuaciones, false)
        header.findViewById<TextView>(R.id.tvPosicion).text = "N°"
        header.findViewById<TextView>(R.id.tvApodo).text = "APODO"
        header.findViewById<TextView>(R.id.tvPorcentaje).text = "%"
        header.findViewById<TextView>(R.id.tvCorrectas).text = "CORRECTAS"
        lvPuntuaciones.addHeaderView(header)

        btnVolver.setOnClickListener { finish() }

        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val i = Intent(this, Login::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }

        cargarPuntuaciones()
    }

    private fun cargarPuntuaciones() {
        db.collection("mathQuizResultados")
            .orderBy("porcentaje", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->

                if (snap.isEmpty) {
                    Toast.makeText(this, "Sin resultados aún", Toast.LENGTH_SHORT).show()
                } else {

                    val filas = snap.documents.mapIndexed { index, doc ->
                        val apodo = doc.getString("apodo") ?: "Sin apodo"
                        val porcentaje = doc.getDouble("porcentaje") ?: 0.0
                        val correctas = doc.getLong("correctas") ?: 0L
                        val total = doc.getLong("totalPreguntas") ?: 0L

                        mapOf(
                            "posicion" to (index + 1).toString(),
                            "apodo" to apodo,
                            "porcentaje" to "${porcentaje.toInt()}%",
                            "correctas" to "$correctas/$total"
                        )
                    }

                    val adapter = object : ArrayAdapter<Map<String, String>>(
                        this,
                        R.layout.item_ranking,
                        filas
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val v = convertView ?: layoutInflater.inflate(
                                R.layout.item_ranking,
                                parent,
                                false
                            )

                            val item = getItem(position) ?: emptyMap()

                            v.findViewById<TextView>(R.id.tvPosicion).text = item["posicion"]
                            v.findViewById<TextView>(R.id.tvApodo).text = item["apodo"]
                            v.findViewById<TextView>(R.id.tvPorcentaje).text = item["porcentaje"]
                            v.findViewById<TextView>(R.id.tvCorrectas).text = item["correctas"]

                            return v
                        }
                    }

                    lvPuntuaciones.adapter = adapter
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}
