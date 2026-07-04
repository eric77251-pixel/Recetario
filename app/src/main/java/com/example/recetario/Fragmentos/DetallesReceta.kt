package com.example.recetario.Fragmentos

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import com.example.recetario.Funciones.ZoomListener
import com.example.recetario.Manager.IngredientesManager
import com.example.recetario.Manager.PasosManager
import com.example.recetario.Manager.UsuarioManager
import com.example.recetario.Manager.RecetaManager
import com.example.recetario.Modelos.Receta
import com.example.recetario.R
import kotlinx.coroutines.launch
import android.os.Build
import coil.load
import com.example.recetario.Manager.GuardadosManager
import com.example.recetario.Modelos.Guardado
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import androidx.activity.addCallback
import com.example.recetario.Actividades.MainActivity
class DetallesReceta : Fragment() {

    private lateinit var btnEliminarReceta: MaterialButton
    private lateinit var imgReceta: ImageView
    private lateinit var txtNombre: TextView
    private lateinit var txtUsuario: TextView
    private lateinit var txtDescripcion: TextView
    private lateinit var txtIngredientes: TextView
    private lateinit var txtProceso: TextView
    private lateinit var btnFavorito: MaterialButton
    private var esFavorito = false
    private lateinit var layoutZoom: FrameLayout
    private lateinit var imgZoom: ImageView

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
        btnFavorito = view.findViewById(R.id.btnFavorito)
        btnEliminarReceta = view.findViewById(R.id.btnEliminarReceta)
        layoutZoom = view.findViewById(R.id.layoutZoom)
        imgZoom = view.findViewById(R.id.imgZoom)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


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
            val usuarioActual = FirebaseAuth.getInstance().currentUser
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
                viewLifecycleOwner.lifecycleScope.launch {

                    try {

                        val pasos = PasosManager.obtenerPasos(data.id)

                        txtProceso.text = if (pasos.isNotEmpty()) {
                            pasos.joinToString("\n") { paso ->
                                "${paso.numero}. ${paso.descripcion}"
                            }
                        } else {
                            "No hay pasos disponibles"
                        }

                    } catch (e: Exception) {

                        e.printStackTrace()

                        txtProceso.text = "Error cargando pasos"
                    }
                }

                imgReceta.load(data.imagenUrl)
                imgZoom.load(data.imagenUrl)
                imgZoom.setOnClickListener {    }

                usuarioActual?.let {

                    esFavorito = GuardadosManager.esFavorito(
                        it.uid,
                        data.id
                    )

                    actualizarBotonFavorito()
                }

            }
            btnFavorito.setOnClickListener {
                val usuarioActual = FirebaseAuth.getInstance().currentUser
                    ?: return@setOnClickListener
                viewLifecycleOwner.lifecycleScope.launch {
                    if (esFavorito) {
                        val eliminado = GuardadosManager.eliminarFavorito(
                            usuarioActual.uid,
                            data.id
                        )
                        if (eliminado) {
                            esFavorito = false
                            actualizarBotonFavorito()
                        }
                    } else {
                        val guardado = Guardado(
                            id = "${usuarioActual.uid}_${data.id}",
                            usuarioId = usuarioActual.uid,
                            recetaId = data.id
                        )
                        val agregado = GuardadosManager.agregarFavorito(
                            guardado
                        )
                        if (agregado) {
                            esFavorito = true
                            actualizarBotonFavorito()
                        }
                    }
                }
            }
            imgReceta.setOnClickListener {
                layoutZoom.visibility = View.VISIBLE
                imgZoom.scaleType = ImageView.ScaleType.FIT_CENTER
                imgZoom.setOnTouchListener(ZoomListener(requireContext()))
            }
            layoutZoom.setOnTouchListener { _, event ->

                if (event.action == android.view.MotionEvent.ACTION_DOWN) {

                    val location = IntArray(2)
                    imgZoom.getLocationOnScreen(location)

                    val left = location[0]
                    val top = location[1]
                    val right = left + imgZoom.width
                    val bottom = top + imgZoom.height

                    if (event.rawX < left ||
                        event.rawX > right ||
                        event.rawY < top ||
                        event.rawY > bottom
                    ) {

                        layoutZoom.visibility = View.GONE
                        imgZoom.setOnTouchListener(null)
                        imgZoom.scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                }

                false
            }

            if (usuarioActual != null && usuarioActual.uid == data.usuarioId) {
                btnEliminarReceta.visibility = View.VISIBLE
                btnEliminarReceta.isEnabled = true

            } else {
                btnEliminarReceta.visibility = View.GONE
                btnEliminarReceta.isEnabled = false
            }
            btnEliminarReceta.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar receta")
                    .setMessage("¿Seguro que deseas eliminar esta receta?")
                    .setPositiveButton("Sí") { _, _ ->
                        eliminarReceta(data)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {

            if (layoutZoom.visibility == View.VISIBLE) {

                layoutZoom.visibility = View.GONE
                imgZoom.setOnTouchListener(null)
                imgZoom.scaleType = ImageView.ScaleType.FIT_CENTER
            } else {
                (requireActivity() as MainActivity).cambiarFragmento(
                    Recetas(),
                    agregarAlBackStack = false,
                    mostrarMenu = true
                )
            }
        }

    }
    private fun eliminarReceta(receta: Receta) {

        viewLifecycleOwner.lifecycleScope.launch {

            try {
                IngredientesManager.eliminarIngredientes(receta.id)
                PasosManager.eliminarPasos(receta.id)
                val ok = RecetaManager.eliminarReceta(receta.id)

                if (ok) {

                    Toast.makeText(
                        requireContext(),
                        "Receta eliminada",
                        Toast.LENGTH_SHORT
                    ).show()

                    parentFragmentManager.popBackStack()

                } else {

                    Toast.makeText(
                        requireContext(),
                        "No se pudo eliminar",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    "Error al eliminar receta",
                    Toast.LENGTH_SHORT
                ).show()

                e.printStackTrace()
            }
        }
    }
    private fun actualizarBotonFavorito() {

        if (esFavorito) {

            btnFavorito.setIconResource(R.drawable.outline_bookmark_24)

            btnFavorito.text = "Guardada"

        } else {

            btnFavorito.setIconResource(R.drawable.outline_bookmark_add_24)

            btnFavorito.text = "Guardar receta"
        }
    }

}