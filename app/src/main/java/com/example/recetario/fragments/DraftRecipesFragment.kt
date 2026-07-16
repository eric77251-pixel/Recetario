package com.example.recetario.fragments

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetario.R
import com.example.recetario.activities.CreateRecipeActivity
import com.example.recetario.adapter.ProfileRecipeAdapter
import com.example.recetario.data.LocalDraftManager
import com.example.recetario.model.Recipe

class DraftRecipesFragment : Fragment() {

    private lateinit var recyclerDraftRecipes: RecyclerView
    private lateinit var txtEmptyDraftRecipes: TextView
    private lateinit var adapter: ProfileRecipeAdapter

    private val draftRecipes = mutableListOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_draft_recipes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerDraftRecipes = view.findViewById(R.id.recyclerDraftRecipes)
        txtEmptyDraftRecipes = view.findViewById(R.id.txtEmptyDraftRecipes)

        // 🌟 CAMBIO CLAVE: Un borrador ahora se abre directamente en modo edición
        adapter = ProfileRecipeAdapter(draftRecipes) { recipe ->
            val intent = Intent(requireContext(), CreateRecipeActivity::class.java).apply {
                putExtra("EXTRA_RECETA_EDITAR", recipe)
            }
            startActivity(intent)
        }

        val columnas = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2
        recyclerDraftRecipes.layoutManager = GridLayoutManager(requireContext(), columnas)
        recyclerDraftRecipes.adapter = adapter
        recyclerDraftRecipes.isNestedScrollingEnabled = false

        cargarBorradores()
    }

    override fun onResume() {
        super.onResume()
        cargarBorradores()
    }

    private fun cargarBorradores() {
        val borradoresLocales = LocalDraftManager.obtenerTodos(requireContext())
        val recetasLocales = borradoresLocales.map { it.recipe }

        draftRecipes.clear()
        draftRecipes.addAll(recetasLocales)

        adapter.notifyDataSetChanged()
        txtEmptyDraftRecipes.visibility = if (draftRecipes.isEmpty()) View.VISIBLE else View.GONE
    }
}