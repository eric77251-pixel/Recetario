package com.example.recetario.Fragmentos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetario.Actividades.Publicacion
import com.example.recetario.Adapter.RecetasAdapter
import com.example.recetario.Manager.RecetaManager
import com.example.recetario.Modelos.Receta
import com.example.recetario.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class Recetas : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecetasAdapter

    private val listaRecetas = mutableListOf<Receta>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        return inflater.inflate(R.layout.recetas, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerReceta)

        val agregarReceta = view.findViewById<FloatingActionButton>(R.id.AgregarReceta)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        adapter = RecetasAdapter(listaRecetas) { receta ->
            abrirDetalle(receta)
        }

        recyclerView.adapter = adapter
        agregarReceta.setOnClickListener {

            val intent = Intent(requireContext(), Publicacion::class.java)
            startActivity(intent)
        }
    }

    private fun cargarRecetas() {

        viewLifecycleOwner.lifecycleScope.launch {

            val recetas = RecetaManager.obtenerRecetas()

            listaRecetas.clear()
            listaRecetas.addAll(recetas)
            adapter.notifyDataSetChanged()
        }
    }
    private fun abrirDetalle(
        receta: Receta
    ) {

        val fragment = DetallesReceta()

        val args = Bundle()

        args.putParcelable("EXTRA_RECETA", receta)
        fragment.arguments = args
        parentFragmentManager.beginTransaction().replace(R.id.contenedorFragments, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        cargarRecetas()
    }
}