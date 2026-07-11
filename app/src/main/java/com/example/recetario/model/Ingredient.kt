package com.example.recetario.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    var id: String = "",
    var recetaId: String = "",
    var nombre: String = "",
    var cantidad: String = "",
    var unidad: String = "",
    var seccion: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(recetaId)
        parcel.writeString(nombre)
        parcel.writeString(cantidad)
        parcel.writeString(unidad)
        parcel.writeString(seccion)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Ingredient> {
        override fun createFromParcel(parcel: Parcel): Ingredient = Ingredient(parcel)
        override fun newArray(size: Int): Array<Ingredient?> = arrayOfNulls(size)
    }
}