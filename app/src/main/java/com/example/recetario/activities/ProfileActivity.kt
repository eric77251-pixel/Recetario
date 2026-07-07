package com.example.recetario.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import coil.load
import coil.request.CachePolicy
import com.example.recetario.R
import com.example.recetario.adapter.ProfileSectionsPagerAdapter
import com.example.recetario.data.RecipeManager
import com.example.recetario.data.SavedRecipeManager
import com.example.recetario.data.UserManager
import com.example.recetario.model.Recipe
import com.example.recetario.utils.AuthManager
import com.example.recetario.utils.NavigationHelper
import com.example.recetario.utils.NetworkUtils
import com.example.recetario.utils.SessionManager
import com.example.recetario.utils.SystemBarUtils
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var navigationBar: NavigationBarView
    private lateinit var btnEditarPerfil: Button
    private lateinit var btnCerrarSesion: Button

    private lateinit var imgFotoPerfil: ImageView
    private lateinit var txtNombreUsuario: TextView
    private lateinit var txtCorreoUsuario: TextView
    private lateinit var txtCantidadRecetas: TextView
    private lateinit var txtFavoritas: TextView

    // Vistas de las pestañas
    private lateinit var tabGuardadas: LinearLayout
    private lateinit var tabMisRecetas: LinearLayout
    private lateinit var tabBorradores: LinearLayout // NUEVO

    private lateinit var txtTabGuardadas: TextView
    private lateinit var txtTabMisRecetas: TextView
    private lateinit var txtTabBorradores: TextView // NUEVO

    private lateinit var indicatorGuardadas: View
    private lateinit var indicatorMisRecetas: View
    private lateinit var indicatorBorradores: View // NUEVO

    private lateinit var viewPagerProfile: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AuthManager.obtenerUsuario() == null) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_profile)
        SystemBarUtils.aplicarInsets(findViewById(R.id.rootProfile))

        inicializarVistas()
        configurarNavegacion()
        configurarEventos()
        configurarSeccionesDeslizables()
        cargarDatosPerfil()
    }

    private fun inicializarVistas() {
        imgFotoPerfil = findViewById(R.id.imgFotoPerfil)
        txtNombreUsuario = findViewById(R.id.txtNombreUsuario)
        txtCorreoUsuario = findViewById(R.id.txtCorreoUsuario)
        txtCantidadRecetas = findViewById(R.id.txtCantidadRecetas)
        txtFavoritas = findViewById(R.id.txtFavoritas)
        navigationBar = findViewById(R.id.bottomNavigation)
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)

        tabGuardadas = findViewById(R.id.tabGuardadas)
        tabMisRecetas = findViewById(R.id.tabMisRecetas)
        tabBorradores = findViewById(R.id.tabBorradores) // NUEVO

        txtTabGuardadas = findViewById(R.id.txtTabGuardadas)
        txtTabMisRecetas = findViewById(R.id.txtTabMisRecetas)
        txtTabBorradores = findViewById(R.id.txtTabBorradores) // NUEVO

        indicatorGuardadas = findViewById(R.id.indicatorGuardadas)
        indicatorMisRecetas = findViewById(R.id.indicatorMisRecetas)
        indicatorBorradores = findViewById(R.id.indicatorBorradores) // NUEVO

        viewPagerProfile = findViewById(R.id.viewPagerProfile)
    }

    private fun configurarNavegacion() {
        navigationBar.selectedItemId = R.id.nav_perfil

        navigationBar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_recetas -> NavigationHelper.irRecetas(this)
                R.id.nav_add -> NavigationHelper.irPublicacion(this)
                R.id.nav_perfil -> true
                else -> false
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            NavigationHelper.volverARecetas(this@ProfileActivity)
        }
    }

    private fun configurarEventos() {
        btnEditarPerfil.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        btnCerrarSesion.setOnClickListener {
            confirmarCierreSesion()
        }

        tabGuardadas.setOnClickListener {
            viewPagerProfile.currentItem = 0
        }

        tabMisRecetas.setOnClickListener {
            viewPagerProfile.currentItem = 1
        }

        // NUEVO: Evento para la pestaña de borradores
        tabBorradores.setOnClickListener {
            viewPagerProfile.currentItem = 2
        }
    }

    private fun configurarSeccionesDeslizables() {
        viewPagerProfile.adapter = ProfileSectionsPagerAdapter(this)
        viewPagerProfile.offscreenPageLimit = 3 // MODIFICADO: Ahora mantenemos 3 pantallas cargadas

        viewPagerProfile.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                actualizarEstadoTabs(position)
            }
        })

        actualizarEstadoTabs(0)
    }

    private fun actualizarEstadoTabs(position: Int) {
        val colorActivo = ContextCompat.getColor(this, R.color.recipe_primary)
        val colorInactivo = ContextCompat.getColor(this, R.color.recipe_text_secondary)

        // 1. Restablecemos todos los textos y opacidades a inactivo por defecto
        txtTabGuardadas.setTextColor(colorInactivo)
        txtTabMisRecetas.setTextColor(colorInactivo)
        txtTabBorradores.setTextColor(colorInactivo)

        txtTabGuardadas.alpha = 0.72f
        txtTabMisRecetas.alpha = 0.72f
        txtTabBorradores.alpha = 0.72f

        // 2. Ocultamos todas las barras indicadoras
        indicatorGuardadas.visibility = View.INVISIBLE
        indicatorMisRecetas.visibility = View.INVISIBLE
        indicatorBorradores.visibility = View.INVISIBLE

        // 3. Activamos solo la pestaña que corresponde a la posición actual
        when (position) {
            0 -> {
                txtTabGuardadas.setTextColor(colorActivo)
                txtTabGuardadas.alpha = 1f
                indicatorGuardadas.visibility = View.VISIBLE
            }
            1 -> {
                txtTabMisRecetas.setTextColor(colorActivo)
                txtTabMisRecetas.alpha = 1f
                indicatorMisRecetas.visibility = View.VISIBLE
            }
            2 -> {
                txtTabBorradores.setTextColor(colorActivo)
                txtTabBorradores.alpha = 1f
                indicatorBorradores.visibility = View.VISIBLE
            }
        }
    }

    private fun confirmarCierreSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Quieres salir de tu cuenta?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Cerrar sesión") { _, _ ->
                AuthManager.cerrarSesion()
                SessionManager.usuario = null

                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)
                finish()
            }
            .show()
    }

    private fun cargarDatosPerfil() {
        if (!NetworkUtils.hayConexion(this)) {
            return
        }

        lifecycleScope.launch {
            cargarUsuario()
            cargarEstadisticasPerfil()
        }
    }

    private suspend fun cargarUsuario() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
        val usuario = UserManager.obtenerUsuario(firebaseUser.uid) ?: return

        SessionManager.usuario = usuario
        txtNombreUsuario.text = "${usuario.nombre} ${usuario.apellido}"
        txtCorreoUsuario.text = usuario.correo

        if (usuario.fotoPerfil.isNotBlank()) {
            imgFotoPerfil.load(urlSinCache(usuario.fotoPerfil)) {
                memoryCachePolicy(CachePolicy.DISABLED)
                diskCachePolicy(CachePolicy.DISABLED)
            }
        } else {
            imgFotoPerfil.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }

    private suspend fun cargarEstadisticasPerfil() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
        val recetasPublicadas = RecipeManager.obtenerRecetasUsuario(firebaseUser.uid)
        val recetasGuardadas = SavedRecipeManager.obtenerFavoritos(firebaseUser.uid)

        actualizarCantidadPublicadas(recetasPublicadas.size)
        actualizarCantidadGuardadas(recetasGuardadas.size)
    }

    private fun urlSinCache(url: String): String {
        val separador = if (url.contains("?")) "&" else "?"
        return "$url${separador}v=${System.currentTimeMillis()}"
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

    override fun onResume() {
        super.onResume()
        cargarDatosPerfil()
    }
}