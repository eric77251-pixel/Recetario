package com.example.recetario.activities

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.recetario.R
import com.example.recetario.data.IngredientManager
import com.example.recetario.data.LocalDraftManager
import com.example.recetario.data.RecipeManager
import com.example.recetario.data.StepManager
import com.example.recetario.model.Ingredient
import com.example.recetario.model.Recipe
import com.example.recetario.model.Step
import com.example.recetario.utils.AuthManager
import com.example.recetario.utils.NavigationHelper
import com.example.recetario.utils.NetworkUtils
import com.example.recetario.utils.PermissionManager
import com.example.recetario.utils.SessionManager
import com.example.recetario.utils.SystemBarUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.math.min

class CreateRecipeActivity : AppCompatActivity() {

    private var selectedMediaUri: Uri? = null
    private var recetaEnEdicion: Recipe? = null

    private lateinit var navigationBar: NavigationBarView
    private lateinit var lblNuevaReceta: TextView
    private lateinit var imagePreview: ImageView
    private lateinit var tvMediaHint: TextView
    private lateinit var btnPostImagen: MaterialButton
    private lateinit var etPostNombre: TextInputEditText
    private lateinit var etPostDescripcion: TextInputEditText
    private lateinit var contenedorIngredientes: LinearLayout
    private lateinit var contenedorPasos: LinearLayout
    private lateinit var btnAgregarIngrediente: Button
    private lateinit var btnAgregarPaso: Button
    private lateinit var btnGuardarPost: Button
    private lateinit var btnCancelarPost: Button

    private val ingredientRows = mutableListOf<IngredientRow>()
    private val stepRows = mutableListOf<StepRow>()
    private val permissionManager = PermissionManager(this)

    private val sugerenciasIngredientes = arrayOf(
        "Harina", "Azúcar", "Sal", "Huevo", "Leche", "Mantequilla", "Aceite",
        "Pollo", "Carne", "Pescado", "Arroz", "Pasta", "Cebolla", "Ajo",
        "Tomate", "Pimienta", "Canela", "Vainilla", "Levadura", "Agua"
    )

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedMediaUri = uri
            mostrarVistaPrevia(uri)
        }
    }

    inner class IngredientRow(val view: View) {
        val etNombre: AutoCompleteTextView = view.findViewById(R.id.etNombre)
        val etCantidad: TextInputEditText = view.findViewById(R.id.etCantidad)
        val etUnidad: TextInputEditText = view.findViewById(R.id.etUnidad)
        val btnEliminar: MaterialButton = view.findViewById(R.id.btnEliminar)
    }

    inner class StepRow(val view: View) {
        val tvNumero: TextView = view.findViewById(R.id.tvNumeroPaso)
        val etDescripcion: TextInputEditText = view.findViewById(R.id.etDescripcion)
        val etTiempo: TextInputEditText = view.findViewById(R.id.etTiempoSeleccionado)
        val btnEliminar: MaterialButton = view.findViewById(R.id.btnEliminar)
        var totalSegundos: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AuthManager.obtenerUsuario() == null) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_create_recipe)
        SystemBarUtils.aplicarInsets(findViewById(R.id.rootCreateRecipe))

        inicializarVistas()
        configurarNavegacion()
        configurarEventos()
        cargarModoFormulario()

        onBackPressedDispatcher.addCallback(this) {
            mostrarDialogoDeBorrador()
        }
    }

    private fun inicializarVistas() {
        navigationBar = findViewById(R.id.bottomNavigation)
        lblNuevaReceta = findViewById(R.id.lblNuevaReceta)
        imagePreview = findViewById(R.id.imagePreview)
        tvMediaHint = findViewById(R.id.tvMediaHint)
        btnPostImagen = findViewById(R.id.btnPostImagen)
        etPostNombre = findViewById(R.id.etPostNombre)
        etPostDescripcion = findViewById(R.id.etPostDescripcion)
        contenedorIngredientes = findViewById(R.id.contenedorIngredientes)
        contenedorPasos = findViewById(R.id.contenedorPasos)
        btnAgregarIngrediente = findViewById(R.id.btnAgregarIngrediente)
        btnAgregarPaso = findViewById(R.id.btnAgregarPaso)
        btnGuardarPost = findViewById(R.id.btnGuardarPost)
        btnCancelarPost = findViewById(R.id.btnCancelarPost)
    }

    private fun configurarEventos() {
        btnPostImagen.setOnClickListener { if (permissionManager.permisosMultimedia(100)) abrirGaleria() }
        btnAgregarIngrediente.setOnClickListener { agregarCampoIngrediente() }
        btnAgregarPaso.setOnClickListener { agregarCampoPaso() }
        btnCancelarPost.setOnClickListener { mostrarDialogoDeBorrador() }
        btnGuardarPost.setOnClickListener { validarYGuardarReceta() }
    }

    private fun mostrarDialogoDeBorrador() {
        MaterialAlertDialogBuilder(this)
            .setTitle("¿Qué deseas hacer con la receta?")
            .setMessage("Tienes cambios sin guardar. Puedes guardar un borrador para publicarlo luego o salir sin guardar.")
            .setPositiveButton("Guardar borrador (Local)") { _, _ -> guardarComoBorrador() }
            .setNegativeButton("Salir sin guardar") { _, _ -> NavigationHelper.volverARecetas(this@CreateRecipeActivity) }
            .setNeutralButton("Seguir editando") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun guardarComoBorrador() {
        val nombre = etPostNombre.text.toString().trim()
        val descripcion = etPostDescripcion.text.toString().trim()
        val ingredientes = ingredientRows.map { row ->
            Ingredient(
                nombre = row.etNombre.text.toString().trim(),
                cantidad = row.etCantidad.text.toString().trim(),
                unidad = row.etUnidad.text.toString().trim()
            )
        }.filter { it.nombre.isNotBlank() }

        val pasos = stepRows.mapIndexed { index, row ->
            Step(
                numero = index + 1,
                descripcion = row.etDescripcion.text.toString().trim(),
                tiempoSegundos = row.totalSegundos
            )
        }.filter { it.descripcion.isNotBlank() }

        if (nombre.isEmpty()) {
            Toast.makeText(this, "Escribe al menos el nombre para guardar el borrador", Toast.LENGTH_SHORT).show()
            etPostNombre.error = "Falta el nombre"
            return
        }

        btnCancelarPost.isEnabled = false
        Toast.makeText(this, "Guardando en tu teléfono...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val receta = Recipe(
                    id = recetaEnEdicion?.id ?: UUID.randomUUID().toString(),
                    usuarioId = AuthManager.obtenerUsuario()?.uid.orEmpty(),
                    nombreUsuario = "${SessionManager.usuario?.nombre} ${SessionManager.usuario?.apellido}",
                    nombre = nombre,
                    descripcion = descripcion,
                    imagenUrl = recetaEnEdicion?.imagenUrl ?: "",
                    estado = "borrador_local"
                )

                LocalDraftManager.guardarBorrador(
                    this@CreateRecipeActivity,
                    receta,
                    ingredientes,
                    pasos,
                    selectedMediaUri
                )

                Toast.makeText(this@CreateRecipeActivity, "Borrador guardado localmente", Toast.LENGTH_SHORT).show()
                NavigationHelper.volverARecetas(this@CreateRecipeActivity)
            } catch (e: Exception) {
                e.printStackTrace()
                btnCancelarPost.isEnabled = true
                Toast.makeText(this@CreateRecipeActivity, "Error al guardar localmente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun configurarNavegacion() {
        navigationBar.selectedItemId = R.id.nav_add
        navigationBar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_recetas -> NavigationHelper.irRecetas(this)
                R.id.nav_add -> true
                R.id.nav_perfil -> NavigationHelper.irPerfil(this)
                else -> false
            }
        }
    }

    private fun cargarModoFormulario() {
        recetaEnEdicion = obtenerRecetaDesdeIntent()
        val receta = recetaEnEdicion

        if (receta == null) {
            lblNuevaReceta.text = "Nueva receta"
            btnGuardarPost.text = "Publicar receta"
            agregarCampoIngrediente()
            agregarCampoPaso()
            return
        }

        lblNuevaReceta.text = if (receta.estado == "borrador_local") "Editar borrador" else "Editar receta"
        btnGuardarPost.text = if (receta.estado == "borrador_local") "Publicar borrador" else "Guardar cambios"

        etPostNombre.setText(receta.nombre)
        etPostDescripcion.setText(receta.descripcion)

        if (receta.imagenUrl.isNotBlank()) {
            tvMediaHint.visibility = View.GONE
            imagePreview.visibility = View.VISIBLE
            imagePreview.load(File(receta.imagenUrl).takeIf { it.exists() } ?: receta.imagenUrl)
            btnPostImagen.text = "Cambiar imagen"
        }

        lifecycleScope.launch {
            val ingredientes: List<Ingredient>
            val pasos: List<Step>

            if (receta.estado == "borrador_local") {
                val draftData = LocalDraftManager.obtenerTodos(this@CreateRecipeActivity).find { it.recipe.id == receta.id }
                ingredientes = draftData?.ingredientes ?: emptyList()
                pasos = draftData?.pasos ?: emptyList()
            } else {
                ingredientes = IngredientManager.obtenerIngredientes(receta.id)
                pasos = StepManager.obtenerPasos(receta.id)
            }

            contenedorIngredientes.removeAllViews()
            ingredientRows.clear()
            if (ingredientes.isEmpty()) agregarCampoIngrediente()
            else ingredientes.forEach { agregarCampoIngrediente(it) }

            contenedorPasos.removeAllViews()
            stepRows.clear()
            if (pasos.isEmpty()) agregarCampoPaso()
            else pasos.forEach { agregarCampoPaso(it) }
        }
    }

    private fun agregarCampoIngrediente(ingrediente: Ingredient? = null) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.item_ingredient_row, contenedorIngredientes, false)
        val row = IngredientRow(view)

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sugerenciasIngredientes)
        row.etNombre.setAdapter(adapter)

        if (ingrediente != null) {
            row.etNombre.setText(ingrediente.nombre)
            row.etCantidad.setText(ingrediente.cantidad)
            row.etUnidad.setText(ingrediente.unidad)
        }

        row.btnEliminar.setOnClickListener {
            if (ingredientRows.size > 1) {
                ingredientRows.remove(row)
                contenedorIngredientes.removeView(view)
            } else {
                row.etNombre.text.clear()
                row.etCantidad.text?.clear()
                row.etUnidad.text?.clear()
            }
        }

        contenedorIngredientes.addView(view)
        ingredientRows.add(row)
    }

    private fun agregarCampoPaso(paso: Step? = null) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.item_step_row, contenedorPasos, false)
        val row = StepRow(view)

        row.tvNumero.text = "Paso ${stepRows.size + 1}"

        if (paso != null) {
            row.etDescripcion.setText(paso.descripcion)
            row.totalSegundos = paso.tiempoSegundos
            actualizarTextoTiempo(row)
        }

        row.etTiempo.setOnClickListener {
            mostrarPickerDuracion(row.totalSegundos) { nuevosSegundos ->
                row.totalSegundos = nuevosSegundos
                actualizarTextoTiempo(row)
            }
        }

        row.btnEliminar.setOnClickListener {
            if (stepRows.size > 1) {
                stepRows.remove(row)
                contenedorPasos.removeView(view)
                renumerarPasos()
            } else {
                row.etDescripcion.text?.clear()
                row.totalSegundos = 0
                actualizarTextoTiempo(row)
            }
        }

        contenedorPasos.addView(view)
        stepRows.add(row)
    }

    private fun actualizarTextoTiempo(row: StepRow) {
        val h = row.totalSegundos / 3600
        val m = (row.totalSegundos % 3600) / 60
        val s = row.totalSegundos % 60
        row.etTiempo.setText(String.format("%02d:%02d:%02d", h, m, s))
    }

    private fun mostrarPickerDuracion(segundosActuales: Int, onResult: (Int) -> Unit) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_duration_picker, null)
        val pickerH = view.findViewById<NumberPicker>(R.id.pickerHours).apply { minValue = 0; maxValue = 23; value = segundosActuales / 3600 }
        val pickerM = view.findViewById<NumberPicker>(R.id.pickerMinutes).apply { minValue = 0; maxValue = 59; value = (segundosActuales % 3600) / 60 }
        val pickerS = view.findViewById<NumberPicker>(R.id.pickerSeconds).apply { minValue = 0; maxValue = 59; value = segundosActuales % 60 }

        MaterialAlertDialogBuilder(this)
            .setView(view)
            .setPositiveButton("Aceptar") { _, _ ->
                val total = (pickerH.value * 3600) + (pickerM.value * 60) + pickerS.value
                onResult(total)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun renumerarPasos() {
        stepRows.forEachIndexed { index, row ->
            row.tvNumero.text = "Paso ${index + 1}"
        }
    }

    private fun abrirGaleria() { pickMedia.launch("image/*") }

    private fun mostrarVistaPrevia(uri: Uri) {
        tvMediaHint.visibility = View.GONE
        imagePreview.visibility = View.VISIBLE
        btnPostImagen.text = "Cambiar imagen"
        val bitmap = ajustarImagen(uri, 900)
        if (bitmap != null) imagePreview.setImageBitmap(bitmap) else imagePreview.load(uri)
    }

    private fun validarYGuardarReceta() {
        val nombre = etPostNombre.text.toString().trim()
        val descripcion = etPostDescripcion.text.toString().trim()
        val ingredientes = ingredientRows.map { row ->
            Ingredient(
                nombre = row.etNombre.text.toString().trim(),
                cantidad = row.etCantidad.text.toString().trim(),
                unidad = row.etUnidad.text.toString().trim()
            )
        }.filter { it.nombre.isNotBlank() }

        val pasos = stepRows.mapIndexed { index, row ->
            Step(
                numero = index + 1,
                descripcion = row.etDescripcion.text.toString().trim(),
                tiempoSegundos = row.totalSegundos
            )
        }.filter { it.descripcion.isNotBlank() }

        if (recetaEnEdicion == null && selectedMediaUri == null) {
            Toast.makeText(this, "Selecciona una imagen", Toast.LENGTH_SHORT).show()
            return
        }
        if (nombre.isEmpty()) { etPostNombre.error = "Falta el nombre"; return }
        if (ingredientes.isEmpty()) { Toast.makeText(this, "Añade al menos un ingrediente", Toast.LENGTH_SHORT).show(); return }
        if (pasos.isEmpty()) { Toast.makeText(this, "Añade al menos un paso", Toast.LENGTH_SHORT).show(); return }
        if (!NetworkUtils.hayConexion(this)) { Toast.makeText(this, "Sin conexión", Toast.LENGTH_SHORT).show(); return }

        btnGuardarPost.isEnabled = false
        Toast.makeText(this, "Publicando receta...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val urlImagen = obtenerUrlImagen()
                if (urlImagen == null) {
                    Toast.makeText(this@CreateRecipeActivity, "Error al subir imagen", Toast.LENGTH_SHORT).show()
                    btnGuardarPost.isEnabled = true
                    return@launch
                }

                val receta = Recipe(
                    id = recetaEnEdicion?.id ?: UUID.randomUUID().toString(),
                    usuarioId = AuthManager.obtenerUsuario()?.uid.orEmpty(),
                    nombreUsuario = "${SessionManager.usuario?.nombre} ${SessionManager.usuario?.apellido}",
                    nombre = nombre,
                    descripcion = descripcion,
                    imagenUrl = urlImagen,
                    estado = "publicado"
                )

                val exito = if (recetaEnEdicion == null || recetaEnEdicion?.estado == "borrador_local") {
                    RecipeManager.crearReceta(receta) != null
                } else {
                    RecipeManager.actualizarReceta(receta)
                }

                if (exito) {
                    if (recetaEnEdicion != null && recetaEnEdicion?.estado != "borrador_local") {
                        IngredientManager.eliminarIngredientes(receta.id)
                        StepManager.eliminarPasos(receta.id)
                    }
                    ingredientes.forEach {
                        it.recetaId = receta.id
                        it.id = UUID.randomUUID().toString()
                        IngredientManager.crearIngrediente(it)
                    }
                    pasos.forEach {
                        it.recetaId = receta.id
                        it.id = UUID.randomUUID().toString()
                        StepManager.crearPaso(it)
                    }

                    if (recetaEnEdicion?.estado == "borrador_local") {
                        LocalDraftManager.eliminarBorrador(this@CreateRecipeActivity, receta.id)
                    }

                    Toast.makeText(this@CreateRecipeActivity, "¡Receta publicada!", Toast.LENGTH_SHORT).show()
                    NavigationHelper.volverARecetas(this@CreateRecipeActivity)
                } else {
                    Toast.makeText(this@CreateRecipeActivity, "Error al publicar", Toast.LENGTH_SHORT).show()
                    btnGuardarPost.isEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                btnGuardarPost.isEnabled = true
            }
        }
    }

    private suspend fun obtenerUrlImagen(): String? {
        if (selectedMediaUri != null) {
            val bitmap = ajustarImagen(selectedMediaUri!!, 900) ?: return null
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            return RecipeManager.subirImagen("receta_${System.currentTimeMillis()}.jpg", stream.toByteArray())

        } else if (recetaEnEdicion?.estado == "borrador_local" && recetaEnEdicion?.imagenUrl?.isNotBlank() == true) {
            val file = File(recetaEnEdicion!!.imagenUrl)
            if (file.exists()) {
                val bytes = file.readBytes()
                return RecipeManager.subirImagen("receta_${System.currentTimeMillis()}.jpg", bytes)
            }
        }
        return recetaEnEdicion?.imagenUrl
    }

    private fun ajustarImagen(uri: Uri, maxSize: Int): Bitmap? {
        return try {
            val original = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            else @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val ratio = min(maxSize.toFloat() / original.width, maxSize.toFloat() / original.height).coerceAtMost(1f)
            Bitmap.createScaledBitmap(original, (original.width * ratio).toInt(), (original.height * ratio).toInt(), true)
        } catch (e: Exception) { null }
    }

    private fun obtenerRecetaDesdeIntent(): Recipe? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("EXTRA_RECETA_EDITAR", Recipe::class.java) ?: intent.getParcelableExtra("EXTRA_RECETA", Recipe::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra("EXTRA_RECETA_EDITAR") ?: intent.getParcelableExtra("EXTRA_RECETA")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) abrirGaleria()
    }
}