package com.example.recetario.Modelos

import android.os.Parcel
import android.os.Parcelable

data class Receta(
 var id: String = "",
 var usuario: String = "",
 var nombre: String = "",
 var descripcion: String = "",
 var imagenUrl: String = "",
 var proceso: List<String> = emptyList(),
 var ingredientes: List<String> = emptyList()
) : Parcelable {

 constructor(parcel: Parcel) : this(
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  parcel.readString() ?: "",
  parcel.createStringArrayList() ?: emptyList(),
  parcel.createStringArrayList() ?: emptyList()
 )

 override fun writeToParcel(parcel: Parcel, flags: Int) {
  parcel.writeString(id)
  parcel.writeString(usuario)
  parcel.writeString(nombre)
  parcel.writeString(descripcion)
  parcel.writeString(imagenUrl)
  parcel.writeStringList(proceso)
  parcel.writeStringList(ingredientes)
 }

 override fun describeContents(): Int {
  return 0
 }

 companion object CREATOR : Parcelable.Creator<Receta> {
  override fun createFromParcel(parcel: Parcel): Receta {
   return Receta(parcel)
  }

  override fun newArray(size: Int): Array<Receta?> {
   return arrayOfNulls(size)
  }
 }
}