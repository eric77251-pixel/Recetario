package com.example.recetario.Actividades

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.recetario.Fragmentos.DetallesReceta
import com.example.recetario.Fragmentos.IniciarSesion
import com.example.recetario.Fragmentos.Recetas
import com.example.recetario.Modelos.Receta
import com.example.recetario.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.recetario.Funciones.Navegacion

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        // Navegación inferior
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

        if (savedInstanceState == null) {

            cambiarFragmento(
                IniciarSesion(),
                agregarAlBackStack = false,
                mostrarMenu = false
            )
        }

        manejarIntentRedireccion(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        manejarIntentRedireccion(intent)
    }

    private fun manejarIntentRedireccion(intent: Intent?) {

        // Abrir detalle de receta
        if (intent?.getBooleanExtra("ABRIR_DETALLE", false) == true) {

            val receta =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                    intent.getParcelableExtra(
                        "EXTRA_RECETA",
                        Receta::class.java
                    )

                } else {

                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra("EXTRA_RECETA")
                }

            receta?.let {

                val fragment = DetallesReceta()

                fragment.arguments = Bundle().apply {
                    putParcelable("EXTRA_RECETA", it)
                }

                cambiarFragmento(
                    fragment,
                    agregarAlBackStack = true,
                    mostrarMenu = true
                )

                bottomNavigation.selectedItemId = R.id.nav_recetas
            }

            return
        }

        // Abrir listado de recetas
        val destino = intent?.getStringExtra("FORZAR_FRAGMENTO")

        if (destino == "Recetas") {

            cambiarFragmento(
                Recetas(),
                agregarAlBackStack = false,
                mostrarMenu = true
            )

            bottomNavigation.selectedItemId = R.id.nav_recetas
        }
    }

    fun cambiarFragmento(
        fragment: Fragment,
        agregarAlBackStack: Boolean = true,
        mostrarMenu: Boolean = true
    ) {

        bottomNavigation.visibility =
            if (mostrarMenu) View.VISIBLE
            else View.GONE

        val transaccion = supportFragmentManager.beginTransaction()

        transaccion.replace(
            R.id.contenedorFragments,
            fragment
        )

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