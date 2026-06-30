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


class IniciarSesion : Fragment() {

    private lateinit var txtUsuario: EditText
    private lateinit var txtContraseña: EditText
    private lateinit var btnAcceder: Button
    private lateinit var btnCrearUsuario: Button
    private lateinit var textCambiarContraseña: TextView

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
        textCambiarContraseña = view.findViewById(R.id.textCambiarcontraseña)

        btnAcceder.setOnClickListener {
            iniciarSesion()
        }

        // Corregido: Uso correcto del contexto en un Fragment
        textCambiarContraseña.setOnClickListener {
            val intent = Intent(requireContext(), CambiarContraseña::class.java)
            startActivity(intent)
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

            // Vuelve a habilitar el botón
            btnAcceder.isEnabled = true

            if (exito) {

                Toast.makeText(
                    requireContext(),
                    "Inicio de sesión exitoso",
                    Toast.LENGTH_SHORT
                ).show()

                // Ir al Fragment de Recetas
                (requireActivity() as MainActivity).cambiarFragmento(
                    Recetas(),
                    agregarAlBackStack = false,
                    mostrarMenu = true
                )

            } else {

                Toast.makeText(
                    requireContext(),
                    mensaje ?: "Correo o contraseña incorrectos",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}