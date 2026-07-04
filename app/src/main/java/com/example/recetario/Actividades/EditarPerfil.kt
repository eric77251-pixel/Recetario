package com.example.recetario.Actividades

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import com.example.recetario.Funciones.Sesion
import com.example.recetario.Funciones.Validaciones
import com.example.recetario.Manager.UsuarioManager
import com.example.recetario.R
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class EditarPerfil : AppCompatActivity() {

    private lateinit var imgFotoEditarPerfil: ImageView
    private lateinit var txtCambiarFoto: TextView
    private lateinit var etEditarNombre: TextInputEditText
    private lateinit var etEditarApellido: TextInputEditText
    private lateinit var btnGuardarCambios: Button
    private lateinit var btnIrCambiarContrasena: Button

    private var fotoUriSeleccionada: Uri? = null

    private val seleccionarFotoLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                val data = result.data

                data?.data?.let { uri ->

                    fotoUriSeleccionada = uri

                    imgFotoEditarPerfil.setImageURI(uri)

                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.editar_perfil)

        inicializarVistas()
        configurarBotonAtras()
        cargarUsuario()

        txtCambiarFoto.setOnClickListener {
            abrirGaleria()
        }

        imgFotoEditarPerfil.setOnClickListener {
            abrirGaleria()
        }

        btnIrCambiarContrasena.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    CambiarContraseña::class.java
                )
            )

        }

        btnGuardarCambios.setOnClickListener {
            validarYGuardar()
        }
    }

    private fun inicializarVistas() {

        imgFotoEditarPerfil = findViewById(R.id.imgFotoEditarPerfil)
        txtCambiarFoto = findViewById(R.id.txtCambiarFoto)
        etEditarNombre = findViewById(R.id.etEditarNombre)
        etEditarApellido = findViewById(R.id.etEditarApellido)
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios)
        btnIrCambiarContrasena =
            findViewById(R.id.btnIrCambiarContrasena)

    }

    private fun cargarUsuario() {

        val usuario = Sesion.usuario ?: return

        etEditarNombre.setText(usuario.nombre)
        etEditarApellido.setText(usuario.apellido)

        if (usuario.fotoPerfil.isNotBlank()) {

            imgFotoEditarPerfil.load(usuario.fotoPerfil)

        }

    }

    private fun configurarBotonAtras() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    val intent =
                        Intent(
                            this@EditarPerfil,
                            Perfil::class.java
                        ).apply {

                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }

                    startActivity(intent)
                    finish()

                }
            })

    }

    private fun abrirGaleria() {

        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }

        seleccionarFotoLauncher.launch(intent)

    }

    private fun validarYGuardar() {

        val nombre =
            etEditarNombre.text.toString().trim()

        val apellido =
            etEditarApellido.text.toString().trim()

        if (Validaciones.campoVacio(nombre)) {

            etEditarNombre.error = "Ingrese su nombre"
            return

        }

        if (Validaciones.campoVacio(apellido)) {

            etEditarApellido.error = "Ingrese su apellido"
            return

        }

        btnGuardarCambios.isEnabled = false

        lifecycleScope.launch {

            try {

                val usuarioActual =
                    Sesion.usuario ?: return@launch

                var urlFoto =
                    usuarioActual.fotoPerfil

                if (fotoUriSeleccionada != null) {

                    val bitmap =
                        MediaStore.Images.Media.getBitmap(
                            contentResolver,
                            fotoUriSeleccionada
                        )

                    val stream =
                        ByteArrayOutputStream()

                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        85,
                        stream
                    )

                    val bytes =
                        stream.toByteArray()

                    val nombreArchivo =
                        "perfil_${usuarioActual.id}.jpg"

                    val nuevaUrl =
                        UsuarioManager.subirFotoPerfil(
                            nombreArchivo,
                            bytes
                        )

                    if (nuevaUrl == null) {

                        Toast.makeText(
                            this@EditarPerfil,
                            "No se pudo subir la foto.",
                            Toast.LENGTH_SHORT
                        ).show()

                        btnGuardarCambios.isEnabled = true
                        return@launch

                    }

                    urlFoto = nuevaUrl

                }

                val usuarioActualizado =
                    usuarioActual.copy(

                        nombre = nombre,
                        apellido = apellido,
                        fotoPerfil = urlFoto

                    )

                val actualizado =
                    UsuarioManager.actualizarUsuario(
                        usuarioActualizado
                    )

                if (actualizado) {

                    Sesion.usuario =
                        usuarioActualizado

                    Toast.makeText(
                        this@EditarPerfil,
                        "Perfil actualizado correctamente.",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent =
                        Intent(
                            this@EditarPerfil,
                            Perfil::class.java
                        ).apply {

                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP

                        }

                    startActivity(intent)
                    finish()

                } else {

                    Toast.makeText(
                        this@EditarPerfil,
                        "No fue posible actualizar el perfil.",
                        Toast.LENGTH_SHORT
                    ).show()

                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@EditarPerfil,
                    "Ocurrió un error al actualizar el perfil.",
                    Toast.LENGTH_SHORT
                ).show()

                e.printStackTrace()

            } finally {

                btnGuardarCambios.isEnabled = true

            }

        }

    }

}