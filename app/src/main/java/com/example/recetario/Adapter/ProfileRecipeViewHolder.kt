package com.example.recetario.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.recetario.model.Recipe
import com.example.recetario.R

class ProfileRecipeViewHolder(
    view: View
) : RecyclerView.ViewHolder(view) {

    private val imgReceta: ImageView =
        view.findViewById(R.id.imgReceta)

    private val txtNombreReceta: TextView =
        view.findViewById(R.id.txtNombreReceta)

    fun render(
        receta: Recipe,
        onClick: (Recipe) -> Unit
    ) {

        txtNombreReceta.text = receta.nombre

        if (receta.imagenUrl.isNotBlank()) {

            imgReceta.load(receta.imagenUrl)

        } else {

            imgReceta.setImageResource(R.drawable.ic_launcher_foreground)
        }

        itemView.setOnClickListener {
            onClick(receta)
        }
    }
}