package com.example.recetario.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetario.R
import com.example.recetario.activities.ProfileActivity
import com.example.recetario.adapter.ProfileRecipeAdapter
import com.example.recetario.data.RecipeManager
import com.example.recetario.data.SavedRecipeManager
import com.example.recetario.model.Recipe
import com.example.recetario.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SavedRecipesFragment : Fragment() {

    private lateinit var recyclerSavedRecipes: RecyclerView
    private lateinit var txtEmptySavedRecipes: TextView
    private lateinit var adapter: ProfileRecipeAdapter

    private val savedRecipes = mutableListOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_saved_recipes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerSavedRecipes = view.findViewById(R.id.recyclerSavedRecipes)
        txtEmptySavedRecipes = view.findViewById(R.id.txtEmptySavedRecipes)

        adapter = ProfileRecipeAdapter(savedRecipes) { recipe ->
            (activity as? ProfileActivity)?.abrirDetalle(recipe)
        }

        // Adaptar columnas: 3 en horizontal, 2 en vertical
        val columnas = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2
        recyclerSavedRecipes.layoutManager = GridLayoutManager(requireContext(), columnas)
        recyclerSavedRecipes.adapter = adapter
        recyclerSavedRecipes.isNestedScrollingEnabled = false

        cargarRecetasGuardadas()
    }

    /**
     * Carga las recetas guardadas del usuario autenticado y actualiza el contador del perfil.
     */
    private fun cargarRecetasGuardadas() {
        if (!NetworkUtils.hayConexion(requireContext())) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val usuario = FirebaseAuth.getInstance().currentUser ?: return@launch
            val favoritos = SavedRecipeManager.obtenerFavoritos(usuario.uid)

            savedRecipes.clear()

            for (favorito in favoritos) {
                val receta = RecipeManager.obtenerReceta(favorito.recetaId)
                if (receta != null) {
                    savedRecipes.add(receta)
                }
            }

            adapter.notifyDataSetChanged()
            txtEmptySavedRecipes.visibility = if (savedRecipes.isEmpty()) View.VISIBLE else View.GONE
            (activity as? ProfileActivity)?.actualizarCantidadGuardadas(savedRecipes.size)
        }
    }
}
