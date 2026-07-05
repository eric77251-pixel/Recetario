package com.example.recetario.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.recetario.R
import com.example.recetario.data.UserManager
import com.example.recetario.fragments.HomeFragment
import com.example.recetario.fragments.LoginFragment
import com.example.recetario.fragments.RegisterFragment
import com.example.recetario.fragments.RecipeDetailFragment
import com.example.recetario.model.Recipe
import com.example.recetario.utils.AuthManager
import com.example.recetario.utils.NavigationHelper
import com.example.recetario.utils.SessionManager
import com.example.recetario.utils.SystemBarUtils
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var navigationBar: NavigationBarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        navigationBar = findViewById(R.id.bottomNavigation)
        SystemBarUtils.aplicarInsets(findViewById(R.id.main))

        configurarNavegacion()

        if (savedInstanceState == null) {
            // Iniciamos el flujo de verificación
            mostrarPantallaInicial()
        } else {
            actualizarInterfazSegunSesion()
        }
    }

    private fun actualizarInterfazSegunSesion() {
        val firebaseUser = AuthManager.obtenerUsuario()
        val currentFragment = supportFragmentManager.findFragmentById(R.id.contenedorFragments)

        if (firebaseUser == null) {
            ocultarMenu()
            if (currentFragment !is LoginFragment && currentFragment !is RegisterFragment) {
                cambiarFragmento(LoginFragment(), agregarAlBackStack = false, mostrarMenu = false)
            }
        } else {
            if (currentFragment is LoginFragment || currentFragment is RegisterFragment) {
                ocultarMenu()
            } else {
                mostrarMenu()
            }
        }
    }

    private fun configurarNavegacion() {
        navigationBar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_recetas -> NavigationHelper.irRecetas(this)
                R.id.nav_add -> NavigationHelper.irPublicacion(this)
                R.id.nav_perfil -> NavigationHelper.irPerfil(this)
                else -> false
            }
        }
    }

    private fun mostrarPantallaInicial() {
        val firebaseUser = AuthManager.obtenerUsuario()

        if (firebaseUser == null) {
            cambiarFragmento(LoginFragment(), agregarAlBackStack = false, mostrarMenu = false)
            return
        }

        ocultarMenu()

        lifecycleScope.launch {
            val usuario = UserManager.obtenerUsuario(firebaseUser.uid)

            if (usuario == null) {
                AuthManager.cerrarSesion()
                cambiarFragmento(LoginFragment(), agregarAlBackStack = false, mostrarMenu = false)
                return@launch
            }

            SessionManager.usuario = usuario
            
            // IMPORTANTE: Primero verificamos si hay una redirección pendiente antes de cargar Home
            if (!manejarIntentRedireccion(intent)) {
                // Solo cargamos Home si no veníamos a ver una receta específica
                cambiarFragmento(HomeFragment(), agregarAlBackStack = false, mostrarMenu = true)
                navigationBar.selectedItemId = R.id.nav_recetas
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        manejarIntentRedireccion(intent)
    }

    /**
     * Procesa el intent para abrir detalles de receta.
     * @return true si se procesó una redirección, false en caso contrario.
     */
    private fun manejarIntentRedireccion(intent: Intent?): Boolean {
        if (AuthManager.obtenerUsuario() == null || intent == null) return false

        if (intent.getBooleanExtra("ABRIR_DETALLE", false)) {
            val receta = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("EXTRA_RECETA", Recipe::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("EXTRA_RECETA")
            }

            receta?.let {
                val fragment = RecipeDetailFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("EXTRA_RECETA", it)
                    }
                }
                // Cargamos el detalle con posibilidad de volver atrás al Home
                cambiarFragmento(fragment, agregarAlBackStack = true, mostrarMenu = true)
                navigationBar.selectedItemId = R.id.nav_recetas
                return true
            }
        }

        if (intent.getStringExtra("FORZAR_FRAGMENTO") == "Recetas") {
            cambiarFragmento(HomeFragment(), agregarAlBackStack = false, mostrarMenu = true)
            navigationBar.selectedItemId = R.id.nav_recetas
            return true
        }
        
        return false
    }

    fun cambiarFragmento(
        fragment: Fragment,
        agregarAlBackStack: Boolean = true,
        mostrarMenu: Boolean = true
    ) {
        (navigationBar as View).visibility = if (mostrarMenu) View.VISIBLE else View.GONE

        val transaccion = supportFragmentManager.beginTransaction()
            .replace(R.id.contenedorFragments, fragment)

        if (agregarAlBackStack) transaccion.addToBackStack(null)
        transaccion.commit()
    }

    fun mostrarMenu() {
        (navigationBar as View).visibility = View.VISIBLE
    }

    fun ocultarMenu() {
        (navigationBar as View).visibility = View.GONE
    }
}
