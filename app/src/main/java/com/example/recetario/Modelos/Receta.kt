package com.example.recetario.Modelos

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Receta(

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
 var fechaCreacion: String = ""

) : Parcelable {

 constructor(parcel: Parcel) : this(
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  parcel.readString() ?: ""
 )

 override fun writeToParcel(parcel: Parcel, flags: Int) {
  parcel.writeString(id)
  parcel.writeString(usuarioId)
  parcel.writeString(nombreUsuario)
  parcel.writeString(nombre)
  parcel.writeString(descripcion)
  parcel.writeString(imagenUrl)
  parcel.writeString(fechaCreacion)
 }

 override fun describeContents(): Int = 0

 companion object CREATOR : Parcelable.Creator<Receta> {

  override fun createFromParcel(parcel: Parcel): Receta {
   return Receta(parcel)
  }

  override fun newArray(size: Int): Array<Receta?> {
   return arrayOfNulls(size)
  }
 }
}