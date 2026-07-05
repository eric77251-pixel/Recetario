package com.example.recetario.fragments

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetario.R
import com.example.recetario.activities.CreateRecipeActivity
import com.example.recetario.activities.MainActivity
import com.example.recetario.adapter.RecipeAdapter
import com.example.recetario.data.RecipeManager
import com.example.recetario.model.Recipe
import com.example.recetario.utils.AuthManager
import com.example.recetario.utils.NetworkUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var adapter: RecipeAdapter
    private lateinit var txtEstadoRecetas: TextView

    private val listaCompleta = mutableListOf<Recipe>()
    private val listaRecetas = mutableListOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verificación de seguridad básica al cargar
        if (AuthManager.obtenerUsuario() == null) {
            (requireActivity() as MainActivity).ocultarMenu()
            (requireActivity() as MainActivity).cambiarFragmento(LoginFragment(), false, false)
            return
        }

        recyclerView = view.findViewById(R.id.recyclerReceta)
        searchView = view.findViewById(R.id.txtBuscarReceta)
        txtEstadoRecetas = view.findViewById(R.id.txtEstadoRecetas)
        val agregarReceta = view.findViewById<FloatingActionButton>(R.id.AgregarReceta)

        // Adaptar columnas según orientación
        val orientacion = resources.configuration.orientation
        val columnas = if (orientacion == Configuration.ORIENTATION_LANDSCAPE) 3 else 2
        recyclerView.layoutManager = GridLayoutManager(requireContext(), columnas)

        adapter = RecipeAdapter(listaRecetas) { receta ->
            abrirDetalle(receta)
        }

        recyclerView.adapter = adapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filtrarRecetas(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarRecetas(newText.orEmpty())
                return true
            }
        })

        agregarReceta.setOnClickListener {
            startActivity(Intent(requireContext(), CreateRecipeActivity::class.java))
        }
    }

    private fun cargarRecetas() {
        if (!NetworkUtils.hayConexion(requireContext())) {
            mostrarEstado("Sin conexión. Revisa tu internet.")
            return
        }

        mostrarEstado("Cargando recetas...")

        viewLifecycleOwner.lifecycleScope.launch {
            val recetas = RecipeManager.obtenerRecetas()

            listaCompleta.clear()
            listaCompleta.addAll(recetas)

            listaRecetas.clear()
            listaRecetas.addAll(recetas)

            adapter.notifyDataSetChanged()

            if (recetas.isEmpty()) {
                mostrarEstado("Aún no hay recetas publicadas.")
            } else {
                ocultarEstado()
            }
        }
    }

    private fun abrirDetalle(receta: Recipe) {
        val fragment = RecipeDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable("EXTRA_RECETA", receta)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.contenedorFragments, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun filtrarRecetas(texto: String) {
        listaRecetas.clear()

        if (texto.isBlank()) {
            listaRecetas.addAll(listaCompleta)
        } else {
            listaRecetas.addAll(
                listaCompleta.filter {
                    it.nombre.contains(texto, ignoreCase = true) ||
                            it.descripcion.contains(texto, ignoreCase = true)
                }
            )
        }

        adapter.notifyDataSetChanged()

        if (listaRecetas.isEmpty()) {
            mostrarEstado("No encontramos resultados.")
        } else {
            ocultarEstado()
        }
    }

    private fun mostrarEstado(mensaje: String) {
        txtEstadoRecetas.text = mensaje
        txtEstadoRecetas.visibility = View.VISIBLE
    }

    private fun ocultarEstado() {
        txtEstadoRecetas.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            cargarRecetas()
        }
    }
}
