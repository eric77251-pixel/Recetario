package com.example.recetario.Actividades

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetario.Adapter.PerfilAdapter
import com.example.recetario.Fragmentos.DetallesReceta
import com.example.recetario.Funciones.Navegacion
import com.example.recetario.Modelos.Receta
import com.example.recetario.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class Perfil : AppCompatActivity() {

    private lateinit var recyclerFavoritas: RecyclerView
    private lateinit var recyclerPublicadas: RecyclerView

    private lateinit var adapterFavoritas: PerfilAdapter
    private lateinit var adapterPublicadas: PerfilAdapter

    private var listaFavoritas = mutableListOf<Receta>()
    private var listaPublicadas = mutableListOf<Receta>()
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var bottonEditarPerfil: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.perfil)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        bottonEditarPerfil=findViewById(R.id.btnEditarPerfil)

        recyclerFavoritas = findViewById(R.id.recyclerFavoritas)
        recyclerPublicadas = findViewById(R.id.recyclerPublicadas)

        iniciarRecyclerViews()

        bottonEditarPerfil.setOnClickListener {
            val activity= Intent(this, EditarPerfil::class.java)
            startActivity(activity)
            this.finish()
        }

        bottomNavigation.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_recetas -> {
                    Navegacion.irRecetas(this)
                }

                R.id.nav_añadir -> {
                    Navegacion.irPublicacion(this)
                }

                R.id.nav_perfil -> {
                    Navegacion.irPerfil(this)
                }

                else -> false
            }
        }
    }



    private fun iniciarRecyclerViews() {

        listaFavoritas = obtenerFavoritas()
        listaPublicadas = obtenerPublicadas()

        adapterFavoritas =
            PerfilAdapter(listaFavoritas) { receta ->
                abrirDetalle(receta)
            }

        adapterPublicadas =
            PerfilAdapter(listaPublicadas) { receta ->
                abrirDetalle(receta)
            }

        recyclerFavoritas.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        recyclerPublicadas.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        recyclerFavoritas.adapter = adapterFavoritas
        recyclerPublicadas.adapter = adapterPublicadas
    }

    private fun obtenerFavoritas(): MutableList<Receta> {

        return mutableListOf(

            Receta(
                id = "1",
                usuario = "Dragon",
                nombre = "Arroz con Pollo",
                descripcion = "Receta favorita",
                imagenUrl = ""
            ),

            Receta(
                id = "2",
                usuario = "Dragon",
                nombre = "Lasagna",
                descripcion = "Receta favorita",
                imagenUrl = ""
            ),

            Receta(
                id = "3",
                usuario = "Dragon",
                nombre = "Panqueques",
                descripcion = "Receta favorita",
                imagenUrl = ""
            )
        )
    }

    private fun obtenerPublicadas(): MutableList<Receta> {

        return mutableListOf(

            Receta(
                id = "10",
                usuario = "Dragon",
                nombre = "Sancocho",
                descripcion = "Publicada por el usuario",
                imagenUrl = ""
            ),

            Receta(
                id = "11",
                usuario = "Dragon",
                nombre = "Arroz con Guandú",
                descripcion = "Publicada por el usuario",
                imagenUrl = ""
            ),

            Receta(
                id = "12",
                usuario = "Dragon",
                nombre = "Tamales",
                descripcion = "Publicada por el usuario",
                imagenUrl = ""
            )
        )
    }

    private fun abrirDetalle(receta: Receta) {

        val intent = Intent(
            this,
            DetallesReceta::class.java
        )

        intent.putExtra("receta", receta)

        startActivity(intent)
    }
}