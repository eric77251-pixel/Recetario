package com.example.recetario.fragments

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.recetario.R
import com.example.recetario.activities.CreateRecipeActivity
import com.example.recetario.activities.MainActivity
import com.example.recetario.data.ChecklistManager
import com.example.recetario.data.IngredientManager
import com.example.recetario.data.RecipeManager
import com.example.recetario.data.SavedRecipeManager
import com.example.recetario.data.StepManager
import com.example.recetario.data.UserManager
import com.example.recetario.model.Ingredient
import com.example.recetario.model.Recipe
import com.example.recetario.model.SavedRecipe
import com.example.recetario.model.Step
import com.example.recetario.utils.NetworkUtils
import com.example.recetario.utils.ZoomListener
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class RecipeDetailFragment : Fragment() {

    private lateinit var btnEliminarReceta: MaterialButton
    private lateinit var btnEditarReceta: MaterialButton
    private lateinit var imgReceta: ImageView
    private lateinit var txtNombre: TextView
    private lateinit var txtUsuario: TextView
    private lateinit var txtDescripcion: TextView
    private lateinit var contenedorChecklist: LinearLayout
    private lateinit var tvProgress: TextView
    private lateinit var btnResetChecklist: MaterialButton
    private lateinit var txtProceso: TextView
    private lateinit var btnFavorito: MaterialButton
    private lateinit var btnVolverDetalle: MaterialButton
    private lateinit var layoutZoom: FrameLayout
    private lateinit var imgZoom: ImageView
    private lateinit var btnCerrarZoom: MaterialButton

    private var esFavorito = false
    private var recetaActual: Recipe? = null
    private val listaIngredientes = mutableListOf<Ingredient>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_recipe_detail, container, false)
        inicializarVistas(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val receta = obtenerRecetaDesdeArgumentos()
        if (receta == null) {
            mostrarMensaje("No se pudo abrir la receta.")
            volverAListado()
            return
        }

        recetaActual = receta
        cargarInformacionReceta(receta)
        configurarEventos(receta)
        configurarBotonAtras()
    }

    private fun inicializarVistas(view: View) {
        imgReceta = view.findViewById(R.id.imgReceta)
        txtNombre = view.findViewById(R.id.txtNombre)
        txtUsuario = view.findViewById(R.id.txtUsuario)
        txtDescripcion = view.findViewById(R.id.txtDescripcion)
        contenedorChecklist = view.findViewById(R.id.contenedorChecklist)
        tvProgress = view.findViewById(R.id.tvProgress)
        btnResetChecklist = view.findViewById(R.id.btnResetChecklist)
        txtProceso = view.findViewById(R.id.txtProceso)
        btnFavorito = view.findViewById(R.id.btnFavorito)
        btnVolverDetalle = view.findViewById(R.id.btnVolverDetalle)
        btnEditarReceta = view.findViewById(R.id.btnEditarReceta)
        btnEliminarReceta = view.findViewById(R.id.btnEliminarReceta)
        layoutZoom = view.findViewById(R.id.layoutZoom)
        imgZoom = view.findViewById(R.id.imgZoom)
        btnCerrarZoom = view.findViewById(R.id.btnCerrarZoom)
    }

    private fun obtenerRecetaDesdeArgumentos(): Recipe? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("EXTRA_RECETA", Recipe::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("EXTRA_RECETA")
        }
    }

    private fun cargarInformacionReceta(receta: Recipe) {
        pintarDatosBase(receta)
        configurarBotonesPropietario(esPropietario(receta))

        if (!NetworkUtils.hayConexion(requireContext())) {
            mostrarMensaje("Sin conexión. La información puede estar incompleta.", Toast.LENGTH_LONG)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            cargarAutor(receta)
            cargarIngredientes(receta.id)
            cargarPasos(receta.id)
            cargarEstadoFavorito(receta)
        }
    }

    private fun pintarDatosBase(receta: Recipe) {
        txtNombre.text = receta.nombre
        txtDescripcion.text = receta.descripcion
        imgReceta.load(receta.imagenUrl)
        imgZoom.load(receta.imagenUrl)
    }

    private fun esPropietario(receta: Recipe): Boolean {
        return FirebaseAuth.getInstance().currentUser?.uid == receta.usuarioId
    }

    private suspend fun cargarAutor(receta: Recipe) {
        val usuario = UserManager.obtenerUsuario(receta.usuarioId)
        txtUsuario.text = if (usuario != null) {
            "Por ${usuario.nombre} ${usuario.apellido}"
        } else {
            "Por usuario desconocido"
        }
    }

    private suspend fun cargarIngredientes(recetaId: String) {
        val ingredientes = IngredientManager.obtenerIngredientes(recetaId)
        listaIngredientes.clear()
        listaIngredientes.addAll(ingredientes)
        actualizarChecklist()
    }

    private suspend fun cargarPasos(recetaId: String) {
        val pasos = StepManager.obtenerPasos(recetaId)
        txtProceso.text = formatearListaPasos(pasos)
    }

    private suspend fun cargarEstadoFavorito(receta: Recipe) {
        val usuarioActual = FirebaseAuth.getInstance().currentUser ?: return
        esFavorito = SavedRecipeManager.esFavorito(usuarioActual.uid, receta.id)
        actualizarBotonFavorito()
    }

    private fun formatearListaPasos(pasos: List<Step>): String {
        if (pasos.isEmpty()) return "No hay pasos disponibles."

        return pasos.joinToString("\n\n") { paso ->
            buildString {
                append("${paso.numero}. ${paso.descripcion}")
                if (paso.tiempoSegundos > 0) {
                    append(" (${formatearTiempo(paso.tiempoSegundos)})")
                }
            }
        }
    }

    private fun formatearTiempo(totalSegundos: Int): String {
        val h = totalSegundos / 3600
        val m = (totalSegundos % 3600) / 60
        val s = totalSegundos % 60

        return buildString {
            if (h > 0) append("${h}h ")
            if (m > 0) append("${m}m ")
            if (s > 0 || (h == 0 && m == 0)) append("${s}s")
        }.trim()
    }

    private fun actualizarChecklist() {
        contenedorChecklist.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        listaIngredientes.forEach { ingredient ->
            val itemView = crearItemChecklist(inflater, ingredient)
            contenedorChecklist.addView(itemView)
        }

        actualizarProgreso()
    }

    private fun crearItemChecklist(inflater: LayoutInflater, ingredient: Ingredient): View {
        val itemView = inflater.inflate(R.layout.item_ingredient_checklist, contenedorChecklist, false)
        val checkBox = itemView.findViewById<CheckBox>(R.id.cbIngredient)
        val tvIngredient = itemView.findViewById<TextView>(R.id.tvIngredientText)
        val isChecked = ChecklistManager.isIngredientChecked(requireContext(), recetaActual?.id ?: "", ingredient.id)

        checkBox.isChecked = isChecked
        tvIngredient.text = "• ${formatearIngrediente(ingredient)}"
        actualizarEstiloIngrediente(tvIngredient, isChecked)

        checkBox.setOnCheckedChangeListener { _, checked ->
            ChecklistManager.setIngredientChecked(requireContext(), recetaActual?.id ?: "", ingredient.id, checked)
            actualizarEstiloIngrediente(tvIngredient, checked)
            actualizarProgreso()
        }

        itemView.setOnClickListener { checkBox.toggle() }
        return itemView
    }

    private fun formatearIngrediente(ingredient: Ingredient): String {
        return buildString {
            if (ingredient.cantidad.isNotBlank()) append("${ingredient.cantidad} ")
            if (ingredient.unidad.isNotBlank()) append("${ingredient.unidad} ")
            append(ingredient.nombre)
        }.trim()
    }

    private fun actualizarProgreso() {
        val checkedCount = contarIngredientesMarcados()
        tvProgress.text = "$checkedCount de ${listaIngredientes.size}"
    }

    private fun contarIngredientesMarcados(): Int {
        var checkedCount = 0
        for (i in 0 until contenedorChecklist.childCount) {
            val itemView = contenedorChecklist.getChildAt(i)
            val checkBox = itemView.findViewById<CheckBox>(R.id.cbIngredient)
            if (checkBox.isChecked) checkedCount++
        }
        return checkedCount
    }

    private fun actualizarEstiloIngrediente(textView: TextView, isChecked: Boolean) {
        if (isChecked) {
            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            textView.alpha = 0.5f
        } else {
            textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            textView.alpha = 1.0f
        }
    }

    private fun configurarEventos(receta: Recipe) {
        btnVolverDetalle.setOnClickListener { volverAListado() }
        btnFavorito.setOnClickListener { alternarFavorito(receta) }
        btnEditarReceta.setOnClickListener { abrirEdicion(receta) }
        btnEliminarReceta.setOnClickListener { confirmarEliminacion(receta) }
        btnResetChecklist.setOnClickListener { reiniciarChecklist(receta) }
        imgReceta.setOnClickListener { abrirZoom() }
        btnCerrarZoom.setOnClickListener { cerrarZoom() }
    }

    private fun abrirEdicion(receta: Recipe) {
        val intent = Intent(requireContext(), CreateRecipeActivity::class.java).apply {
            putExtra("EXTRA_RECETA_EDITAR", receta)
        }
        startActivity(intent)
    }

    private fun confirmarEliminacion(receta: Recipe) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar receta")
            .setMessage("¿Seguro que deseas eliminar esta receta?")
            .setPositiveButton("Sí") { _, _ -> eliminarReceta(receta) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun reiniciarChecklist(receta: Recipe) {
        ChecklistManager.clearChecklist(requireContext(), receta.id, listaIngredientes.map { it.id })
        actualizarChecklist()
    }

    private fun abrirZoom() {
        layoutZoom.visibility = View.VISIBLE
        imgZoom.setOnTouchListener(ZoomListener(requireContext()))
    }

    private fun configurarBotonesPropietario(esPropietario: Boolean) {
        val visibilidad = if (esPropietario) View.VISIBLE else View.GONE
        btnEditarReceta.visibility = visibilidad
        btnEliminarReceta.visibility = visibilidad
        btnEditarReceta.isEnabled = esPropietario
        btnEliminarReceta.isEnabled = esPropietario
    }

    private fun alternarFavorito(receta: Recipe) {
        val usuarioActual = FirebaseAuth.getInstance().currentUser
        if (usuarioActual == null) {
            mostrarMensaje("Debe iniciar sesión.")
            return
        }

        if (!NetworkUtils.hayConexion(requireContext())) {
            mostrarMensaje("Sin conexión. Intente nuevamente.", Toast.LENGTH_LONG)
            return
        }

        btnFavorito.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            guardarEstadoFavorito(usuarioActual.uid, receta)
        }
    }

    private suspend fun guardarEstadoFavorito(usuarioId: String, receta: Recipe) {
        val correcto = if (esFavorito) {
            SavedRecipeManager.eliminarFavorito(usuarioId, receta.id)
        } else {
            SavedRecipeManager.agregarFavorito(
                SavedRecipe(
                    id = "${usuarioId}_${receta.id}",
                    usuarioId = usuarioId,
                    recetaId = receta.id
                )
            )
        }

        if (correcto) {
            esFavorito = !esFavorito
            actualizarBotonFavorito()
        } else {
            mostrarMensaje("No se pudo actualizar guardados.")
        }

        btnFavorito.isEnabled = true
    }

    private fun eliminarReceta(receta: Recipe) {
        if (!NetworkUtils.hayConexion(requireContext())) {
            mostrarMensaje("Sin conexión. Intente nuevamente.", Toast.LENGTH_LONG)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                eliminarDatosRelacionados(receta.id)
                val ok = RecipeManager.eliminarReceta(receta.id)

                if (ok) {
                    mostrarMensaje("Receta eliminada.")
                    volverAListado()
                } else {
                    mostrarMensaje("No se pudo eliminar la receta.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mostrarMensaje("Error al eliminar receta.")
            }
        }
    }

    private suspend fun eliminarDatosRelacionados(recetaId: String) {
        IngredientManager.eliminarIngredientes(recetaId)
        StepManager.eliminarPasos(recetaId)
    }

    private fun configurarBotonAtras() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (layoutZoom.visibility == View.VISIBLE) {
                cerrarZoom()
            } else {
                volverAListado()
            }
        }
    }

    private fun cerrarZoom() {
        layoutZoom.visibility = View.GONE
        imgZoom.setOnTouchListener(null)
        imgZoom.scaleType = ImageView.ScaleType.FIT_CENTER
    }

    private fun volverAListado() {
        (requireActivity() as MainActivity).cambiarFragmento(
            HomeFragment(),
            agregarAlBackStack = false,
            mostrarMenu = true
        )
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

    private fun mostrarMensaje(mensaje: String, duracion: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), mensaje, duracion).show()
    }
}