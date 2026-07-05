package com.example.recetario.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.view.ViewGroup
import android.widget.LinearLayout
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.request.CachePolicy
import com.example.recetario.R
import com.example.recetario.utils.SystemBarUtils
import com.example.recetario.utils.SquareCropImageView
import com.example.recetario.data.UserManager
import com.example.recetario.utils.NetworkUtils
import com.example.recetario.utils.PermissionManager
import com.example.recetario.utils.SessionManager
import com.example.recetario.utils.ValidationUtils
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.min

class EditProfileActivity : AppCompatActivity() {

    private lateinit var imgFotoEditarPerfil: ImageView
    private lateinit var txtCambiarFoto: TextView
    private lateinit var etEditarNombre: TextInputEditText
    private lateinit var etEditarApellido: TextInputEditText
    private lateinit var btnGuardarCambios: Button
    private lateinit var btnIrCambiarContrasena: Button

    private val permissionManager = PermissionManager(this)
    private var fotoUriSeleccionada: Uri? = null
    private var fotoBitmapRecortada: Bitmap? = null

    private val seleccionarFotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            mostrarEditorRecorte(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        SystemBarUtils.aplicarInsets(findViewById(R.id.rootEditProfile))

        inicializarVistas()
        configurarBotonAtras()
        configurarEventos()
        cargarUsuario()
    }

    private fun inicializarVistas() {
        imgFotoEditarPerfil = findViewById(R.id.imgFotoEditarPerfil)
        txtCambiarFoto = findViewById(R.id.txtCambiarFoto)
        etEditarNombre = findViewById(R.id.etEditarNombre)
        etEditarApellido = findViewById(R.id.etEditarApellido)
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios)
        btnIrCambiarContrasena = findViewById(R.id.btnIrCambiarContrasena)
    }

    private fun configurarEventos() {
        txtCambiarFoto.setOnClickListener { solicitarGaleria() }
        imgFotoEditarPerfil.setOnClickListener { solicitarGaleria() }

        btnIrCambiarContrasena.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        btnGuardarCambios.setOnClickListener {
            validarYGuardar()
        }
    }

    private fun cargarUsuario() {
        val usuario = SessionManager.usuario ?: return

        etEditarNombre.setText(usuario.nombre)
        etEditarApellido.setText(usuario.apellido)

        if (usuario.fotoPerfil.isNotBlank()) {
            imgFotoEditarPerfil.load(urlSinCache(usuario.fotoPerfil)) {
                // Evita que Coil muestre una foto vieja guardada en caché.
                memoryCachePolicy(CachePolicy.DISABLED)
                diskCachePolicy(CachePolicy.DISABLED)
            }
        } else {
            imgFotoEditarPerfil.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }

    private fun configurarBotonAtras() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    volverAlPerfil()
                }
            }
        )
    }

    private fun solicitarGaleria() {
        if (permissionManager.permisosMultimedia(100)) {
            abrirGaleria()
        }
    }

    private fun abrirGaleria() {
        seleccionarFotoLauncher.launch("image/*")
    }

    /**
     * Valida nombre, apellido e imagen antes de actualizar Firebase y Supabase.
     */
    private fun validarYGuardar() {
        val nombre = etEditarNombre.text.toString().trim()
        val apellido = etEditarApellido.text.toString().trim()

        if (ValidationUtils.campoVacio(nombre)) {
            etEditarNombre.error = "Ingrese su nombre"
            etEditarNombre.requestFocus()
            return
        }

        if (ValidationUtils.campoVacio(apellido)) {
            etEditarApellido.error = "Ingrese su apellido"
            etEditarApellido.requestFocus()
            return
        }

        if (!NetworkUtils.hayConexion(this)) {
            Toast.makeText(this, "Sin conexión. Intente nuevamente.", Toast.LENGTH_LONG).show()
            return
        }

        btnGuardarCambios.isEnabled = false

        lifecycleScope.launch {
            try {
                val usuarioActual = SessionManager.usuario ?: return@launch
                var urlFoto = usuarioActual.fotoPerfil

                if (fotoBitmapRecortada != null) {
                    val bytes = convertirBitmapABytes(fotoBitmapRecortada!!)

                    val nuevaUrl = UserManager.subirFotoPerfil(
                        // Nombre único para que la URL cambie y la app no reutilice la imagen anterior.
                        nombreArchivo = "perfil_${usuarioActual.id}_${System.currentTimeMillis()}.jpg",
                        bytesImagen = bytes
                    )

                    if (nuevaUrl == null) {
                        mostrarMensaje("No se pudo subir la foto de perfil.")
                        return@launch
                    }

                    urlFoto = nuevaUrl
                }

                val usuarioActualizado = usuarioActual.copy(
                    nombre = nombre,
                    apellido = apellido,
                    fotoPerfil = urlFoto
                )

                val actualizado = UserManager.actualizarUsuario(usuarioActualizado)

                if (actualizado) {
                    SessionManager.usuario = usuarioActualizado
                    mostrarMensaje("Perfil actualizado correctamente.")
                    volverAlPerfil()
                } else {
                    mostrarMensaje("No fue posible actualizar el perfil.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mostrarMensaje("Ocurrió un error al actualizar el perfil.")
            } finally {
                btnGuardarCambios.isEnabled = true
            }
        }
    }

    private fun mostrarEditorRecorte(uri: Uri) {
        val bitmapOriginal = cargarBitmapDesdeUri(uri)

        if (bitmapOriginal == null) {
            mostrarMensaje("No se pudo abrir la foto seleccionada.")
            return
        }

        val bitmapParaRecorte = ajustarImagen(bitmapOriginal, 1600)
        val cropView = SquareCropImageView(this).apply {
            setImageBitmapForCrop(bitmapParaRecorte)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(390)
            ).apply {
                topMargin = dp(12)
            }
        }

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(12))

            addView(TextView(this@EditProfileActivity).apply {
                text = "Recortar foto 1:1"
                textSize = 20f
                setTextColor(getColorCompat(R.color.recipe_text_primary))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })

            addView(TextView(this@EditProfileActivity).apply {
                text = "Mueve la imagen y usa dos dedos para hacer zoom. Solo se subirá el área cuadrada."
                textSize = 14f
                setTextColor(getColorCompat(R.color.recipe_text_secondary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(6)
                }
            })

            addView(cropView)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Usar foto", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val fotoRecortada = cropView.obtenerBitmapRecortado(900)

                if (fotoRecortada == null) {
                    mostrarMensaje("No se pudo recortar la foto.")
                    return@setOnClickListener
                }

                fotoUriSeleccionada = uri
                fotoBitmapRecortada = fotoRecortada
                imgFotoEditarPerfil.setImageBitmap(fotoRecortada)
                mostrarMensaje("Foto recortada en formato 1:1.")
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun cargarBitmapDesdeUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun convertirBitmapABytes(bitmap: Bitmap): ByteArray {
        val bitmapAjustado = Bitmap.createScaledBitmap(bitmap, 900, 900, true)
        return ByteArrayOutputStream().use { stream ->
            bitmapAjustado.compress(Bitmap.CompressFormat.JPEG, 88, stream)
            stream.toByteArray()
        }
    }

    private fun ajustarImagen(bitmap: Bitmap, maxSize: Int): Bitmap {
        val ratio = min(
            maxSize.toFloat() / bitmap.width,
            maxSize.toFloat() / bitmap.height
        ).coerceAtMost(1f)

        val nuevoAncho = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val nuevoAlto = (bitmap.height * ratio).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(bitmap, nuevoAncho, nuevoAlto, true)
    }

    private fun urlSinCache(url: String): String {
        val separador = if (url.contains("?")) "&" else "?"
        return "$url${separador}v=${System.currentTimeMillis()}"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun getColorCompat(colorRes: Int): Int {
        return androidx.core.content.ContextCompat.getColor(this, colorRes)
    }

    private fun volverAlPerfil() {
        val intent = Intent(this, ProfileActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            val concedidos = grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (concedidos) {
                abrirGaleria()
            } else {
                Toast.makeText(
                    this,
                    "El permiso de imágenes es necesario para cambiar la foto.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
