package com.example.recetario.fragments

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
import com.example.recetario.model.Recipe
import com.example.recetario.utils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MyRecipesFragment : Fragment() {

    private lateinit var recyclerMyRecipes: RecyclerView
    private lateinit var txtEmptyMyRecipes: TextView
    private lateinit var adapter: ProfileRecipeAdapter

    private val myRecipes = mutableListOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_my_recipes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerMyRecipes = view.findViewById(R.id.recyclerMyRecipes)
        txtEmptyMyRecipes = view.findViewById(R.id.txtEmptyMyRecipes)

        adapter = ProfileRecipeAdapter(myRecipes) { recipe ->
            (activity as? ProfileActivity)?.abrirDetalle(recipe)
        }

        recyclerMyRecipes.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerMyRecipes.adapter = adapter
        recyclerMyRecipes.isNestedScrollingEnabled = false

        cargarMisRecetas()
    }

    /**
     * Obtiene las publicaciones creadas por el usuario autenticado.
     */
    private fun cargarMisRecetas() {
        if (!NetworkUtils.hayConexion(requireContext())) {
            Toast.makeText(requireContext(), "Sin conexión. No se pudieron cargar tus recetas.", Toast.LENGTH_LONG).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val usuario = FirebaseAuth.getInstance().currentUser ?: return@launch
            val recetas = RecipeManager.obtenerRecetasUsuario(usuario.uid)

            myRecipes.clear()
            myRecipes.addAll(recetas)

            adapter.notifyDataSetChanged()
            txtEmptyMyRecipes.visibility = if (myRecipes.isEmpty()) View.VISIBLE else View.GONE
            (activity as? ProfileActivity)?.actualizarCantidadPublicadas(myRecipes.size)
        }
    }
}
