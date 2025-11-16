package com.example.p_one.Main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.p_one.AdminMenu.CrudAdmin.crudAdministrador
import com.example.p_one.AdminMenu.CrudAdmin.crudAlumno
import com.example.p_one.AdminMenu.CrudAdmin.crudCursos
import com.example.p_one.AdminMenu.CrudAdmin.crudEditRol
import com.example.p_one.AdminMenu.CrudAdmin.crudProfesor
import com.example.p_one.AdminMenu.CrudAdmin.crudRoles
import com.example.p_one.R

class menuAdmin : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_admin)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    fun curdAdmin(view: View){
        startActivity(Intent(this, crudAdministrador::class.java))
    }
    fun curdAlumno(view: View){
        startActivity(Intent(this, crudAlumno::class.java))
    }
    fun curdProfesor(view: View){
        startActivity(Intent(this, crudProfesor::class.java))
    }
    fun curdRoles(view: View){
        startActivity(Intent(this, crudRoles::class.java))
    }
    fun curdCursos(view: View){
        startActivity(Intent(this, crudCursos::class.java))
    }
    fun curdEdit(view: View){
        startActivity(Intent(this, crudEditRol::class.java))
    }
}