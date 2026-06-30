package com.example.recetario.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.recetario.Modelos.Receta
import com.example.recetario.R
import com.example.recetario.Adapter.PerfilViewHolder

class PerfilAdapter(
    private val listaRecetas: List<Receta>,
    private val onClick: (Receta) -> Unit
) : RecyclerView.Adapter<PerfilViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PerfilViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.perfil_view_holder,
                parent,
                false
            )

        return PerfilViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: PerfilViewHolder,
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