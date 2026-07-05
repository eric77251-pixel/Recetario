package com.example.recetario.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetario.adapter.ProfileRecipeAdapter
import com.example.recetario.utils.NavigationHelper
import androidx.activity.addCallback
import com.example.recetario.model.Recipe
import com.example.recetario.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.lifecycle.lifecycleScope
import com.example.recetario.data.SavedRecipeManager
import com.example.recetario.data.RecipeManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import android.widget.ImageView
import android.widget.TextView
import coil.load
import com.example.recetario.data.UserManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var recyclerFavoritas: RecyclerView
    private lateinit var recyclerPublicadas: RecyclerView

    private lateinit var adapterFavoritas: ProfileRecipeAdapter
    private lateinit var adapterPublicadas: ProfileRecipeAdapter

    private var listaFavoritas = mutableListOf<Recipe>()
    private var listaPublicadas = mutableListOf<Recipe>()
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var bottonEditarPerfil: Button
    private lateinit var imgFotoPerfil: ImageView

    private lateinit var txtNombreUsuario: TextView
    private lateinit var txtCorreoUsuario: TextView

    private lateinit var txtCantidadRecetas: TextView
    private lateinit var txtFavoritas: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_profile)
        imgFotoPerfil = findViewById(R.id.imgFotoPerfil)

        txtNombreUsuario = findViewById(R.id.txtNombreUsuario)
        txtCorreoUsuario = findViewById(R.id.txtCorreoUsuario)

        txtCantidadRecetas = findViewById(R.id.txtCantidadRecetas)
        txtFavoritas = findViewById(R.id.txtFavoritas)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        bottonEditarPerfil=findViewById(R.id.btnEditarPerfil)

        recyclerFavoritas = findViewById(R.id.recyclerFavoritas)
        recyclerPublicadas = findViewById(R.id.recyclerPublicadas)

        iniciarRecyclerViews()
        onBackPressedDispatcher.addCallback(this) {
            NavigationHelper.volverARecetas(this@ProfileActivity)
        }
        bottonEditarPerfil.setOnClickListener {
            val activity= Intent(this, EditProfileActivity::class.java)
            startActivity(activity)
            this.finish()
        }

        bottomNavigation.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_recetas -> {
                    NavigationHelper.irRecetas(this)
                }

                R.id.nav_add -> {
                    NavigationHelper.irPublicacion(this)
                }

                R.id.nav_perfil -> {
                    NavigationHelper.irPerfil(this)
                }

                else -> false
            }
        }
        cargarUsuario()
    }


    private fun cargarUsuario() {

        lifecycleScope.launch {

            val firebaseUser =
                FirebaseAuth.getInstance().currentUser
                    ?: return@launch

            val usuario =
                UserManager.obtenerUsuario(firebaseUser.uid)
                    ?: return@launch

            txtNombreUsuario.text =
                "${usuario.nombre} ${usuario.apellido}"

            txtCorreoUsuario.text =
                usuario.correo

            if (usuario.fotoPerfil.isNotBlank()) {

                imgFotoPerfil.load(usuario.fotoPerfil)

            } else {

                imgFotoPerfil.setImageResource(
                    android.R.drawable.sym_def_app_icon
                )
            }

        }
    }


    private fun iniciarRecyclerViews() {

        adapterFavoritas =
            ProfileRecipeAdapter(listaFavoritas) { receta ->
                abrirDetalle(receta)
            }

        adapterPublicadas =
            ProfileRecipeAdapter(listaPublicadas) { receta ->
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

        obtenerFavoritas()
        obtenerPublicadas()
    }
    private fun obtenerFavoritas() {

        lifecycleScope.launch {

            val usuario = FirebaseAuth.getInstance().currentUser ?: return@launch

            val favoritos =
                SavedRecipeManager.obtenerFavoritos(usuario.uid)

            listaFavoritas.clear()

            for (favorito in favoritos) {

                val receta =
                    RecipeManager.obtenerReceta(favorito.recetaId)

                if (receta != null) {
                    listaFavoritas.add(receta)
                }
            }
            txtFavoritas.text =
                listaFavoritas.size.toString()

            adapterFavoritas.notifyDataSetChanged()
        }
    }
    private fun obtenerPublicadas() {

        lifecycleScope.launch {

            val usuario = FirebaseAuth.getInstance().currentUser ?: return@launch

            val recetas =
                RecipeManager.obtenerRecetasUsuario(usuario.uid)

            listaPublicadas.clear()
            listaPublicadas.addAll(recetas)
            txtCantidadRecetas.text =
                listaPublicadas.size.toString()

            adapterPublicadas.notifyDataSetChanged()
        }
    }


    private fun abrirDetalle(receta: Recipe) {

        val intent = Intent(this, MainActivity::class.java)

        intent.putExtra("EXTRA_RECETA", receta)
        intent.putExtra("ABRIR_DETALLE", true)

        startActivity(intent)
    }
}