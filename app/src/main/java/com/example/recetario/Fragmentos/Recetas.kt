package com.example.recetario.Fragmentos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recetario.Actividades.Publicacion
import com.example.recetario.Adapter.RecetasAdapter
import com.example.recetario.Modelos.Receta
import com.example.recetario.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Recetas : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecetasAdapter
    private var listaRecetas = mutableListOf<Receta>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Únicamente inflamos la vista y la retornamos
        return inflater.inflate(R.layout.recetas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Obtener los componentes desde el XML inflado
        recyclerView = view.findViewById(R.id.recyclerReceta)
        val agregarReceta = view.findViewById<FloatingActionButton>(R.id.AgregarReceta)

        // 2. Inicializar el RecyclerView
        iniciarRecycler()

        // 3. Configurar el clic para abrir la Actividad de Publicación
        agregarReceta.setOnClickListener {
            val intent = Intent(requireContext(), Publicacion::class.java)
            startActivity(intent)
            // Se eliminó finish() porque estamos en un Fragment y queremos mantener esta pantalla de fondo
        }
    }

    // Configura el RecyclerView
    private fun iniciarRecycler() {
        listaRecetas = obtenerRecetas()

        adapter = RecetasAdapter(listaRecetas) { recetaSeleccionada ->
            abrirDetalle(recetaSeleccionada)
        }

        // Define que el RecyclerView será vertical con dos columnas
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter
    }

    // Se encargará de recibir todas las recetas ya existentes
    private fun obtenerRecetas(): MutableList<Receta> {
        return mutableListOf(
            Receta(
                id = "1",
                usuario = "Dragon",
                nombre = "Arroz con Pollo",
                descripcion = "Deliciosa receta tradicional panameña.",
                imagenUrl = "",
                proceso = listOf(
                    "Cocinar el pollo",
                    "Preparar el sofrito",
                    "Agregar el arroz",
                    "Cocinar hasta que esté listo"
                ),
                ingredientes = listOf(
                    "2 tazas de arroz",
                    "1 pollo",
                    "1 cebolla",
                    "1 pimentón"
                )
            ),
            Receta(
                id = "2",
                usuario = "María",
                nombre = "Lasagna",
                descripcion = "Lasagna casera con carne y queso.",
                imagenUrl = "",
                proceso = listOf(
                    "Preparar la salsa",
                    "Cocinar la carne",
                    "Montar las capas",
                    "Hornear"
                ),
                ingredientes = listOf(
                    "Pasta para lasagna",
                    "Carne molida",
                    "Queso mozzarella",
                    "Salsa de tomate"
                )
            )
        )
    }

    // Pasa los detalles de la receta cliqueada al fragment de detalle
    private fun abrirDetalle(receta: Receta) {
        val fragment = DetallesReceta()

        val args = Bundle().apply {
            putParcelable("EXTRA_RECETA", receta)
        }
        fragment.arguments = args

        parentFragmentManager
            .beginTransaction()
            .replace(R.id.contenedorFragments, fragment)
            .addToBackStack(null)
            .commit()
    }
}