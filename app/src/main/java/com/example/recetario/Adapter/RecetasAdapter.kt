package com.example.recetario.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.recetario.Modelos.Recipe
import com.example.recetario.R

class RecetasAdapter(private val recetaContent: List<Recipe>, private val onClickListener: (Recipe) -> Unit) : RecyclerView.Adapter<RecetasViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecetasViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.recetas_view_holder,
                parent,
                false
            )

        return RecetasViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecetasViewHolder, position: Int) {
        holder.render(recetaContent[position], onClickListener)
    }

    override fun getItemCount(): Int {
        return recetaContent.size
    }
}