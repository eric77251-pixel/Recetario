package com.example.recetario.Fragmentos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.recetario.Funciones.ZoomListener
import com.example.recetario.Modelos.Receta
import com.example.recetario.R

class DetallesReceta : Fragment() {

    private lateinit var imgReceta: ImageView
    private lateinit var txtNombre: TextView
    private lateinit var txtUsuario: TextView
    private lateinit var txtDescripcion: TextView
    private lateinit var txtIngredientes: TextView
    private lateinit var txtProceso: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.detalle_receta,
            container,
            false
        )

        // Inicialización de vistas
        imgReceta = view.findViewById(R.id.imgReceta)
        txtNombre = view.findViewById(R.id.txtNombre)
        txtUsuario = view.findViewById(R.id.txtUsuario)
        txtDescripcion = view.findViewById(R.id.txtDescripcion)
        txtIngredientes = view.findViewById(R.id.txtIngredientes)
        txtProceso = view.findViewById(R.id.txtProceso)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Asignar el listener externo para habilitar zoom por doble toque y pinza
        imgReceta.setOnTouchListener(ZoomListener(requireContext()))

        // 1. Recuperar el objeto Receta de los argumentos de forma segura según la API
        val receta = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("EXTRA_RECETA", Receta::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable<Receta>("EXTRA_RECETA")
        }

        // 2. Asignar los datos a los componentes de la interfaz
        receta?.let { data ->
            txtNombre.text = data.nombre
            txtUsuario.text = "Por: ${data.usuario}"
            txtDescripcion.text = data.descripcion

            // Convierte la lista de ingredientes a texto separado por saltos de línea
            txtIngredientes.text = data.ingredientes.joinToString("\n") { "- $it" }

            // Convierte la lista del proceso enumerándolos automáticamente
            txtProceso.text = data.proceso.mapIndexed { index, paso -> "${index + 1}. $paso" }.joinToString("\n")
        }
    }
}