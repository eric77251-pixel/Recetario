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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var btnEditarPerfil: Button
    private lateinit var btnCerrarSesion: Button

    private lateinit var imgFotoPerfil: ImageView
    private lateinit var txtNombreUsuario: TextView
    private lateinit var txtCorreoUsuario: TextView
    private lateinit var txtCantidadRecetas: TextView
    private lateinit var txtFavoritas: TextView

    private lateinit var tabGuardadas: LinearLayout
    private lateinit var tabMisRecetas: LinearLayout
    private lateinit var txtTabGuardadas: TextView
    private lateinit var txtTabMisRecetas: TextView
    private lateinit var indicatorGuardadas: View
    private lateinit var indicatorMisRecetas: View
    private lateinit var viewPagerProfile: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        bottomNavigation = findViewById(R.id.bottomNavigation)
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
        tabGuardadas = findViewById(R.id.tabGuardadas)
        tabMisRecetas = findViewById(R.id.tabMisRecetas)
        txtTabGuardadas = findViewById(R.id.txtTabGuardadas)
        txtTabMisRecetas = findViewById(R.id.txtTabMisRecetas)
        indicatorGuardadas = findViewById(R.id.indicatorGuardadas)
        indicatorMisRecetas = findViewById(R.id.indicatorMisRecetas)
        viewPagerProfile = findViewById(R.id.viewPagerProfile)
    }

    private fun configurarNavegacion() {
        bottomNavigation.selectedItemId = R.id.nav_perfil

        bottomNavigation.setOnItemSelectedListener {
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
    }

    /**
     * Usa ViewPager2 para permitir cambiar entre secciones del perfil
     * con gestos horizontales, manteniendo una experiencia más similar
     * a Instagram o X.
     */
    private fun configurarSeccionesDeslizables() {
        viewPagerProfile.adapter = ProfileSectionsPagerAdapter(this)
        viewPagerProfile.offscreenPageLimit = 2

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

        val guardadasActiva = position == 0

        txtTabGuardadas.setTextColor(if (guardadasActiva) colorActivo else colorInactivo)
        txtTabMisRecetas.setTextColor(if (guardadasActiva) colorInactivo else colorActivo)

        txtTabGuardadas.alpha = if (guardadasActiva) 1f else 0.72f
        txtTabMisRecetas.alpha = if (guardadasActiva) 0.72f else 1f

        indicatorGuardadas.visibility = if (guardadasActiva) View.VISIBLE else View.INVISIBLE
        indicatorMisRecetas.visibility = if (guardadasActiva) View.INVISIBLE else View.VISIBLE
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
            Toast.makeText(this, "Sin conexión. No se pudo cargar el perfil.", Toast.LENGTH_LONG).show()
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
                // La foto de perfil puede cambiar usando el mismo usuario; se fuerza recarga real.
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
