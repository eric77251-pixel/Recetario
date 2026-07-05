package com.example.recetario.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.recetario.model.Recipe
import com.example.recetario.R

class RecipeAdapter(private val recetaContent: List<Recipe>, private val onClickListener: (Recipe) -> Unit) : RecyclerView.Adapter<RecipeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_recipe,
                parent,
                false
            )

        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.render(recetaContent[position], onClickListener)
    }

    override fun getItemCount(): Int {
        return recetaContent.size
    }
}