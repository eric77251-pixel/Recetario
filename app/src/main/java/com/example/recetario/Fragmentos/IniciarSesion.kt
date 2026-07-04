package com.example.recetario.Fragmentos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.recetario.Actividades.CambiarContraseña
import com.example.recetario.Actividades.MainActivity
import com.example.recetario.Funciones.Validaciones
import com.example.recetario.R
import com.example.recetario.Funciones.Authentication
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.recetario.Manager.UsuarioManager
import com.example.recetario.Funciones.Sesion


class IniciarSesion : Fragment() {

    private lateinit var txtUsuario: EditText
    private lateinit var txtContraseña: EditText
    private lateinit var btnAcceder: Button
    private lateinit var btnCrearUsuario: Button


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.login,
            container,
            false
        )

        txtUsuario = view.findViewById(R.id.txtUsuario)
        txtContraseña = view.findViewById(R.id.txtContraseña)
        btnAcceder = view.findViewById(R.id.btnAcceder)
        btnCrearUsuario = view.findViewById(R.id.btnCrearUsuario)


        btnAcceder.setOnClickListener {
            iniciarSesion()
        }

        btnCrearUsuario.setOnClickListener {
            (requireActivity() as MainActivity).cambiarFragmento(
                RegistrarUsuario(),
                agregarAlBackStack = true,
                mostrarMenu = false
            )
        }

        return view
    }

    private fun iniciarSesion() {

        val correo = txtUsuario.text.toString().trim()
        val contraseña = txtContraseña.text.toString()

        // Validar correo vacío
        if (Validaciones.campoVacio(correo)) {
            txtUsuario.error = "Ingrese un correo"
            txtUsuario.requestFocus()
            return
        }

        // Validar formato del correo
        if (!Validaciones.correoValido(correo)) {
            txtUsuario.error = "Correo inválido"
            txtUsuario.requestFocus()
            return
        }

        // Validar contraseña
        if (Validaciones.campoVacio(contraseña)) {
            txtContraseña.error = "Ingrese una contraseña"
            txtContraseña.requestFocus()
            return
        }

        // Evita múltiples clics
        btnAcceder.isEnabled = false

        Authentication.iniciarSesion(
            correo,
            contraseña
        ) { exito, mensaje ->

            if (!exito) {

                btnAcceder.isEnabled = true

                Toast.makeText(
                    requireContext(),
                    mensaje ?: "Correo o contraseña incorrectos",
                    Toast.LENGTH_LONG
                ).show()

                return@iniciarSesion
            }

            // Usuario autenticado en Firebase
            val firebaseUser = Authentication.obtenerUsuario()

            if (firebaseUser == null) {

                btnAcceder.isEnabled = true

                Toast.makeText(
                    requireContext(),
                    "No se pudo obtener el usuario autenticado.",
                    Toast.LENGTH_SHORT
                ).show()

                return@iniciarSesion
            }

            // Buscar los datos del usuario en Supabase
            viewLifecycleOwner.lifecycleScope.launch {

                val usuario = UsuarioManager.obtenerUsuario(firebaseUser.uid.toString())

                btnAcceder.isEnabled = true

                if (usuario == null) {

                    Toast.makeText(
                        requireContext(),
                        "No se encontró el usuario en la base de datos.",
                        Toast.LENGTH_LONG
                    ).show()

                    Authentication.cerrarSesion()
                    return@launch
                }

                // Guardar el usuario en memoria
                Sesion.usuario = usuario

                Toast.makeText(
                    requireContext(),
                    "Bienvenido ${usuario.nombre}",
                    Toast.LENGTH_SHORT
                ).show()

                // Ir al fragmento principal
                (requireActivity() as MainActivity).cambiarFragmento(
                    Recetas(),
                    agregarAlBackStack = false,
                    mostrarMenu = true
                )
            }
        }
    }

}