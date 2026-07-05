package com.example.recetario.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.recetario.model.Recipe
import com.example.recetario.R

class ProfileRecipeAdapter(
    private val listaRecetas: List<Recipe>,
    private val onClick: (Recipe) -> Unit
) : RecyclerView.Adapter<ProfileRecipeViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProfileRecipeViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_profile_recipe,
                parent,
                false
            )

        return ProfileRecipeViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProfileRecipeViewHolder,
        position: Int
    ) {

        holder.render(
            listaRecetas[position],
            onClick
        )
    }

    override fun getItemCount(): Int {
        return listaRecetas.size
    }
}