package com.example.recetario.Adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recetario.Modelos.Receta
import com.example.recetario.R

class RecetasViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val imgPost: ImageView =
        view.findViewById(R.id.imgPost)

    private val txtRecetaNombre: TextView =
        view.findViewById(R.id.txtRecetaNombre)

    private val txtDescripcion: TextView =
        view.findViewById(R.id.txtDescripcion)

    private val txtRecetaUsuario: TextView =
        view.findViewById(R.id.txtRecetaUsuario)

    fun render(
        receta: Receta,
        onClickListener: (Receta) -> Unit
    ) {

        txtRecetaNombre.text = receta.nombre

        txtDescripcion.text = receta.descripcion

        txtRecetaUsuario.text = receta.usuario

        itemView.setOnClickListener {
            onClickListener(receta)
        }
    }
}