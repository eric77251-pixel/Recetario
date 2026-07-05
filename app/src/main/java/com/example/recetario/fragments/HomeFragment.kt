package com.example.recetario.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.SearchView
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetario.activities.CreateRecipeActivity
import com.example.recetario.adapter.RecipeAdapter
import com.example.recetario.data.RecipeManager
import com.example.recetario.model.Recipe
import com.example.recetario.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var adapter: RecipeAdapter

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

        recyclerView = view.findViewById(R.id.recyclerReceta)
        searchView = view.findViewById(R.id.txtBuscarReceta)
        val agregarReceta = view.findViewById<FloatingActionButton>(R.id.AgregarReceta)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = RecipeAdapter(listaRecetas) { receta ->
            abrirDetalle(receta)
        }

        recyclerView.adapter = adapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                filtrarRecetas(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarRecetas(newText ?: "")
                return true
            }
        })


        agregarReceta.setOnClickListener {
            val intent = Intent(requireContext(), CreateRecipeActivity::class.java)
            startActivity(intent)
        }
        cargarRecetas()
    }

    private fun cargarRecetas() {

        viewLifecycleOwner.lifecycleScope.launch {

            val recetas = RecipeManager.obtenerRecetas()

            listaCompleta.clear()
            listaCompleta.addAll(recetas)

            listaRecetas.clear()
            listaRecetas.addAll(recetas)

            adapter.notifyDataSetChanged()
        }
    }

    private fun abrirDetalle(receta: Recipe) {

        val fragment = RecipeDetailFragment()

        val args = Bundle()
        args.putParcelable("EXTRA_RECETA", receta)

        fragment.arguments = args

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

            val resultado = listaCompleta.filter {

                it.nombre.contains(texto, ignoreCase = true)

            }

            listaRecetas.addAll(resultado)
        }

        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()

        cargarRecetas()
    }
}