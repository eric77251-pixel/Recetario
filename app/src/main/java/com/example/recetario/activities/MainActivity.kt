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

    // Usamos NavigationBarView para soportar tanto BottomNav como NavigationRail
    private lateinit var navigationBar: NavigationBarView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // El ID es el mismo en ambos layouts (land y portrait)
        navigationBar = findViewById(R.id.bottomNavigation)
        SystemBarUtils.aplicarInsets(findViewById(R.id.main))

        configurarNavegacion()

        if (savedInstanceState == null) {
            mostrarPantallaInicial()
        } else {
            actualizarInterfazSegunSesion()
        }

        manejarIntentRedireccion(intent)
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
            cambiarFragmento(HomeFragment(), agregarAlBackStack = false, mostrarMenu = true)
            navigationBar.selectedItemId = R.id.nav_recetas
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        manejarIntentRedireccion(intent)
    }

    private fun manejarIntentRedireccion(intent: Intent?) {
        if (AuthManager.obtenerUsuario() == null) return

        if (intent?.getBooleanExtra("ABRIR_DETALLE", false) == true) {
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
                cambiarFragmento(fragment, agregarAlBackStack = true, mostrarMenu = true)
                navigationBar.selectedItemId = R.id.nav_recetas
            }
            return
        }

        if (intent?.getStringExtra("FORZAR_FRAGMENTO") == "Recetas") {
            cambiarFragmento(HomeFragment(), agregarAlBackStack = false, mostrarMenu = true)
            navigationBar.selectedItemId = R.id.nav_recetas
        }
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
