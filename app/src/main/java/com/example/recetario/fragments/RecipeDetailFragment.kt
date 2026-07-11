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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recetaActual = obtenerRecetaDesdeArgumentos()

        val receta = recetaActual
        if (receta == null) {
            Toast.makeText(requireContext(), "No se pudo abrir la receta.", Toast.LENGTH_SHORT).show()
            volverAListado()
            return
        }

        cargarDatosBase(receta)
        configurarEventos(receta)
        configurarBotonAtras()
    }

    private fun obtenerRecetaDesdeArgumentos(): Recipe? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("EXTRA_RECETA", Recipe::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("EXTRA_RECETA")
        }
    }

    private fun cargarDatosBase(receta: Recipe) {
        val usuarioActual = FirebaseAuth.getInstance().currentUser

        txtNombre.text = receta.nombre
        txtDescripcion.text = receta.descripcion
        imgReceta.load(receta.imagenUrl)
        imgZoom.load(receta.imagenUrl)

        configurarBotonesPropietario(usuarioActual?.uid == receta.usuarioId)

        if (!NetworkUtils.hayConexion(requireContext())) {
            Toast.makeText(requireContext(), "Sin conexión. La información puede estar incompleta.", Toast.LENGTH_LONG).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val usuario = UserManager.obtenerUsuario(receta.usuarioId)
            txtUsuario.text = if (usuario != null) {
                "Por ${usuario.nombre} ${usuario.apellido}"
            } else {
                "Por usuario desconocido"
            }

            val ingredientes = IngredientManager.obtenerIngredientes(receta.id)
            listaIngredientes.clear()
            listaIngredientes.addAll(ingredientes)
            actualizarChecklist()

            val pasos = StepManager.obtenerPasos(receta.id)
            txtProceso.text = if (pasos.isNotEmpty()) {
                pasos.joinToString("\n\n") { paso ->
                    var texto = "${paso.numero}. ${paso.descripcion}"
                    if (paso.tiempoSegundos > 0) {
                        texto += " (${formatearTiempo(paso.tiempoSegundos)})"
                    }
                    texto
                }
            } else {
                "No hay pasos disponibles."
            }

            if (usuarioActual != null) {
                esFavorito = SavedRecipeManager.esFavorito(usuarioActual.uid, receta.id)
                actualizarBotonFavorito()
            }
        }
    }

    private fun formatearTiempo(totalSegundos: Int): String {
        val h = totalSegundos / 3600
        val m = (totalSegundos % 3600) / 60
        val s = totalSegundos % 60
        
        val sb = StringBuilder()
        if (h > 0) sb.append("${h}h ")
        if (m > 0) sb.append("${m}m ")
        if (s > 0 || (h == 0 && m == 0)) sb.append("${s}s")
        return sb.toString().trim()
    }

    private fun actualizarChecklist() {
        contenedorChecklist.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        var checkedCount = 0

        listaIngredientes.forEach { ingredient ->
            val itemView = inflater.inflate(R.layout.item_ingredient_checklist, contenedorChecklist, false)
            val checkBox = itemView.findViewById<CheckBox>(R.id.cbIngredient)
            val tvIngredient = itemView.findViewById<TextView>(R.id.tvIngredientText)

            val isChecked = ChecklistManager.isIngredientChecked(requireContext(), recetaActual?.id ?: "", ingredient.id)
            checkBox.isChecked = isChecked

            val displayStr = StringBuilder()
            if (ingredient.cantidad.isNotBlank()) displayStr.append("${ingredient.cantidad} ")
            if (ingredient.unidad.isNotBlank()) displayStr.append("${ingredient.unidad} ")
            displayStr.append(ingredient.nombre)
            tvIngredient.text = "• ${displayStr.toString().trim()}"

            actualizarEstiloIngrediente(tvIngredient, isChecked)
            if (isChecked) checkedCount++

            checkBox.setOnCheckedChangeListener { _, checked ->
                ChecklistManager.setIngredientChecked(requireContext(), recetaActual?.id ?: "", ingredient.id, checked)
                actualizarEstiloIngrediente(tvIngredient, checked)
                actualizarProgreso()
            }

            itemView.setOnClickListener { checkBox.toggle() }

            contenedorChecklist.addView(itemView)
        }
        tvProgress.text = "$checkedCount de ${listaIngredientes.size}"
    }

    private fun actualizarProgreso() {
        var checkedCount = 0
        for (i in 0 until contenedorChecklist.childCount) {
            val itemView = contenedorChecklist.getChildAt(i)
            val checkBox = itemView.findViewById<CheckBox>(R.id.cbIngredient)
            if (checkBox.isChecked) checkedCount++
        }
        tvProgress.text = "$checkedCount de ${listaIngredientes.size}"
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
        btnVolverDetalle.setOnClickListener {
            volverAListado()
        }

        btnFavorito.setOnClickListener {
            alternarFavorito(receta)
        }

        btnEditarReceta.setOnClickListener {
            val intent = Intent(requireContext(), CreateRecipeActivity::class.java).apply {
                putExtra("EXTRA_RECETA_EDITAR", receta)
            }
            startActivity(intent)
        }

        btnEliminarReceta.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar receta")
                .setMessage("¿Seguro que deseas eliminar esta receta?")
                .setPositiveButton("Sí") { _, _ -> eliminarReceta(receta) }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnResetChecklist.setOnClickListener {
            ChecklistManager.clearChecklist(requireContext(), receta.id, listaIngredientes.map { it.id })
            actualizarChecklist()
        }

        imgReceta.setOnClickListener {
            layoutZoom.visibility = View.VISIBLE
            imgZoom.setOnTouchListener(ZoomListener(requireContext()))
        }

        btnCerrarZoom.setOnClickListener {
            cerrarZoom()
        }
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
            Toast.makeText(requireContext(), "Debe iniciar sesión.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!NetworkUtils.hayConexion(requireContext())) {
            Toast.makeText(requireContext(), "Sin conexión. Intente nuevamente.", Toast.LENGTH_LONG).show()
            return
        }

        btnFavorito.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val correcto = if (esFavorito) {
                SavedRecipeManager.eliminarFavorito(usuarioActual.uid, receta.id)
            } else {
                SavedRecipeManager.agregarFavorito(
                    SavedRecipe(
                        id = "${usuarioActual.uid}_${receta.id}",
                        usuarioId = usuarioActual.uid,
                        recetaId = receta.id
                    )
                )
            }

            if (correcto) {
                esFavorito = !esFavorito
                actualizarBotonFavorito()
            } else {
                Toast.makeText(requireContext(), "No se pudo actualizar guardados.", Toast.LENGTH_SHORT).show()
            }

            btnFavorito.isEnabled = true
        }
    }

    private fun eliminarReceta(receta: Recipe) {
        if (!NetworkUtils.hayConexion(requireContext())) {
            Toast.makeText(requireContext(), "Sin conexión. Intente nuevamente.", Toast.LENGTH_LONG).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                IngredientManager.eliminarIngredientes(receta.id)
                StepManager.eliminarPasos(receta.id)
                val ok = RecipeManager.eliminarReceta(receta.id)

                if (ok) {
                    Toast.makeText(requireContext(), "Receta eliminada.", Toast.LENGTH_SHORT).show()
                    volverAListado()
                } else {
                    Toast.makeText(requireContext(), "No se pudo eliminar la receta.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al eliminar receta.", Toast.LENGTH_SHORT).show()
            }
        }
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
}