package com.example.recetario.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Recipe(

 var id: String = "",

 @SerialName("usuario_id")
 var usuarioId: String = "",

 @SerialName("nombre_usuario")
 var nombreUsuario: String = "",

 var nombre: String = "",

 var descripcion: String = "",

 @SerialName("imagen_url")
 var imagenUrl: String = "",

 @SerialName("fecha_creacion")
 var fechaCreacion: String = "",

 // 1. NUEVO: Agregamos el estado con valor "publicado" por defecto
 var estado: String = "publicado"

) : Parcelable {

 constructor(parcel: Parcel) : this(
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  // 2. NUEVO: Leemos el estado al desempaquetar (si es nulo, será "publicado")
  parcel.readString() ?: "publicado"
 )

 override fun writeToParcel(parcel: Parcel, flags: Int) {
  parcel.writeString(id)
  parcel.writeString(usuarioId)
  parcel.writeString(nombreUsuario)
  parcel.writeString(nombre)
  parcel.writeString(descripcion)
  parcel.writeString(imagenUrl)
  parcel.writeString(fechaCreacion)
  // 3. NUEVO: Guardamos el estado al empaquetar
  parcel.writeString(estado)
 }

 override fun describeContents(): Int = 0

 companion object CREATOR : Parcelable.Creator<Recipe> {

  override fun createFromParcel(parcel: Parcel): Recipe {
   return Recipe(parcel)
  }

  override fun newArray(size: Int): Array<Recipe?> {
   return arrayOfNulls(size)
  }
 }
}