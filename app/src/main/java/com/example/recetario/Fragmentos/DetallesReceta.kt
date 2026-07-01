package com.example.recetario.Fragmentos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.recetario.Funciones.ZoomListener
import com.example.recetario.Manager.IngredientesManager
import com.example.recetario.Manager.PasosManager
import com.example.recetario.Manager.UsuarioManager
import com.example.recetario.Modelos.Receta
import com.example.recetario.R
import kotlinx.coroutines.launch
import android.os.Build
import coil.load

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
        val receta =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arguments?.getParcelable(
                    "EXTRA_RECETA",
                    Receta::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                arguments?.getParcelable("EXTRA_RECETA")
            }

        receta?.let { data ->

            txtNombre.text = data.nombre
            txtDescripcion.text = data.descripcion

            viewLifecycleOwner.lifecycleScope.launch {

                // Obtener usuario
                val usuario = UsuarioManager.obtenerUsuario(
                    data.usuarioId
                )

                txtUsuario.text =
                    if (usuario != null) {
                        "${usuario.nombre} ${usuario.apellido}"
                    } else {
                        "Usuario desconocido"
                    }

                // Obtener ingredientes
                val ingredientes =
                    IngredientesManager.obtenerIngredientes(data.id)

                txtIngredientes.text =
                    ingredientes.joinToString("\n") {
                        it.nombre
                    }

                // Obtener pasos
                val pasos =
                    PasosManager.obtenerPasos(data.id)

                txtProceso.text =
                    pasos.joinToString("\n") {
                        "${it.numero}. ${it.descripcion}"
                    }

                imgReceta.load(data.imagenUrl)
            }
        }
    }
}