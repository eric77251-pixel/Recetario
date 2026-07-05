package com.example.recetario.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.recetario.R
import com.example.recetario.utils.SystemBarUtils
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

    private val seleccionarFotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            fotoUriSeleccionada = uri
            imgFotoEditarPerfil.load(uri)
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
            imgFotoEditarPerfil.load(usuario.fotoPerfil)
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

                if (fotoUriSeleccionada != null) {
                    val bytes = convertirImagenABytes(fotoUriSeleccionada!!)

                    if (bytes == null) {
                        mostrarMensaje("No se pudo procesar la foto seleccionada.")
                        return@launch
                    }

                    val nuevaUrl = UserManager.subirFotoPerfil(
                        nombreArchivo = "perfil_${usuarioActual.id}.jpg",
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

    private fun convertirImagenABytes(uri: Uri): ByteArray? {
        return try {
            val bitmapOriginal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }

            val bitmapAjustado = ajustarImagen(bitmapOriginal, 900)
            ByteArrayOutputStream().use { stream ->
                bitmapAjustado.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                stream.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun ajustarImagen(bitmap: Bitmap, maxSize: Int): Bitmap {
        val ratio = min(
            maxSize.toFloat() / bitmap.width,
            maxSize.toFloat() / bitmap.height
        ).coerceAtMost(1f)

        val nuevoAncho = (bitmap.width * ratio).toInt()
        val nuevoAlto = (bitmap.height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, nuevoAncho, nuevoAlto, true)
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
