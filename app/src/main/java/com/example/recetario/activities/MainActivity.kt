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
import com.example.recetario.fragments.RecipeDetailFragment
import com.example.recetario.model.Recipe
import com.example.recetario.utils.AuthManager
import com.example.recetario.utils.NavigationHelper
import com.example.recetario.utils.SessionManager
import com.example.recetario.utils.SystemBarUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        SystemBarUtils.aplicarInsets(findViewById(R.id.main))

        configurarNavegacionInferior()

        if (savedInstanceState == null) {
            mostrarPantallaInicial()
        }

        manejarIntentRedireccion(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        manejarIntentRedireccion(intent)
    }

    /**
     * Decide si se muestra login o recetas según el usuario autenticado en Firebase.
     * Esta validación restringe el acceso a la pantalla principal si no hay sesión activa.
     */
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
                Toast.makeText(
                    this@MainActivity,
                    "Debe iniciar sesión nuevamente.",
                    Toast.LENGTH_SHORT
                ).show()

                cambiarFragmento(LoginFragment(), agregarAlBackStack = false, mostrarMenu = false)
                return@launch
            }

            SessionManager.usuario = usuario
            cambiarFragmento(HomeFragment(), agregarAlBackStack = false, mostrarMenu = true)
            bottomNavigation.selectedItemId = R.id.nav_recetas
        }
    }

    private fun configurarNavegacionInferior() {
        bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_recetas -> NavigationHelper.irRecetas(this)
                R.id.nav_add -> NavigationHelper.irPublicacion(this)
                R.id.nav_perfil -> NavigationHelper.irPerfil(this)
                else -> false
            }
        }
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
                bottomNavigation.selectedItemId = R.id.nav_recetas
            }
            return
        }

        if (intent?.getStringExtra("FORZAR_FRAGMENTO") == "Recetas") {
            cambiarFragmento(HomeFragment(), agregarAlBackStack = false, mostrarMenu = true)
            bottomNavigation.selectedItemId = R.id.nav_recetas
        }
    }

    fun cambiarFragmento(
        fragment: Fragment,
        agregarAlBackStack: Boolean = true,
        mostrarMenu: Boolean = true
    ) {
        bottomNavigation.visibility = if (mostrarMenu) View.VISIBLE else View.GONE

        val transaccion = supportFragmentManager.beginTransaction()
            .replace(R.id.contenedorFragments, fragment)

        if (agregarAlBackStack) {
            transaccion.addToBackStack(null)
        }

        transaccion.commit()
    }

    fun mostrarMenu() {
        bottomNavigation.visibility = View.VISIBLE
    }

    fun ocultarMenu() {
        bottomNavigation.visibility = View.GONE
    }
}
