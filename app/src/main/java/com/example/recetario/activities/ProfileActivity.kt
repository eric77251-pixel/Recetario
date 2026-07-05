package com.example.recetario.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.recetario.R
import com.example.recetario.data.RecipeManager
import com.example.recetario.data.SavedRecipeManager
import com.example.recetario.data.UserManager
import com.example.recetario.fragments.MyRecipesFragment
import com.example.recetario.fragments.SavedRecipesFragment
import com.example.recetario.model.Recipe
import com.example.recetario.utils.NavigationHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var botonEditarPerfil: Button
    private lateinit var btnTabGuardadas: Button
    private lateinit var btnTabMisRecetas: Button

    private lateinit var imgFotoPerfil: ImageView
    private lateinit var txtNombreUsuario: TextView
    private lateinit var txtCorreoUsuario: TextView
    private lateinit var txtCantidadRecetas: TextView
    private lateinit var txtFavoritas: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        inicializarVistas()
        configurarNavegacionInferior()
        configurarEventos()
        cargarUsuario()
        cargarEstadisticasPerfil()

        if (savedInstanceState == null) {
            mostrarFragmentoPerfil(SavedRecipesFragment())
            marcarTabActiva(esGuardadas = true)
        }
    }

    private fun inicializarVistas() {
        imgFotoPerfil = findViewById(R.id.imgFotoPerfil)
        txtNombreUsuario = findViewById(R.id.txtNombreUsuario)
        txtCorreoUsuario = findViewById(R.id.txtCorreoUsuario)
        txtCantidadRecetas = findViewById(R.id.txtCantidadRecetas)
        txtFavoritas = findViewById(R.id.txtFavoritas)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        botonEditarPerfil = findViewById(R.id.btnEditarPerfil)
        btnTabGuardadas = findViewById(R.id.btnTabGuardadas)
        btnTabMisRecetas = findViewById(R.id.btnTabMisRecetas)
    }

    private fun configurarEventos() {
        onBackPressedDispatcher.addCallback(this) {
            NavigationHelper.volverARecetas(this@ProfileActivity)
        }

        botonEditarPerfil.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnTabGuardadas.setOnClickListener {
            mostrarFragmentoPerfil(SavedRecipesFragment())
            marcarTabActiva(esGuardadas = true)
        }

        btnTabMisRecetas.setOnClickListener {
            mostrarFragmentoPerfil(MyRecipesFragment())
            marcarTabActiva(esGuardadas = false)
        }
    }

    private fun configurarNavegacionInferior() {
        bottomNavigation.selectedItemId = R.id.nav_perfil

        bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_recetas -> NavigationHelper.irRecetas(this)
                R.id.nav_add -> NavigationHelper.irPublicacion(this)
                R.id.nav_perfil -> true
                else -> false
            }
        }
    }

    private fun mostrarFragmentoPerfil(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.profileFragmentContainer, fragment)
            .commit()
    }

    private fun marcarTabActiva(esGuardadas: Boolean) {
        btnTabGuardadas.isSelected = esGuardadas
        btnTabMisRecetas.isSelected = !esGuardadas
    }

    private fun cargarUsuario() {
        lifecycleScope.launch {
            val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return@launch
            val usuario = UserManager.obtenerUsuario(firebaseUser.uid) ?: return@launch

            txtNombreUsuario.text = "${usuario.nombre} ${usuario.apellido}"
            txtCorreoUsuario.text = usuario.correo

            if (usuario.fotoPerfil.isNotBlank()) {
                imgFotoPerfil.load(usuario.fotoPerfil)
            } else {
                imgFotoPerfil.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        }
    }


    private fun cargarEstadisticasPerfil() {
        lifecycleScope.launch {
            val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return@launch

            val recetasPublicadas = RecipeManager.obtenerRecetasUsuario(firebaseUser.uid)
            val recetasGuardadas = SavedRecipeManager.obtenerFavoritos(firebaseUser.uid)

            actualizarCantidadPublicadas(recetasPublicadas.size)
            actualizarCantidadGuardadas(recetasGuardadas.size)
        }
    }

    fun actualizarCantidadGuardadas(cantidad: Int) {
        txtFavoritas.text = cantidad.toString()
    }

    fun actualizarCantidadPublicadas(cantidad: Int) {
        txtCantidadRecetas.text = cantidad.toString()
    }

    fun abrirDetalle(receta: Recipe) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("EXTRA_RECETA", receta)
            putExtra("ABRIR_DETALLE", true)
        }

        startActivity(intent)
    }
}
