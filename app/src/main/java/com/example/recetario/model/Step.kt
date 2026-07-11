package com.example.recetario.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

@Serializable
data class Step(
    var id: String = "",
    var recetaId: String = "",
    var numero: Int = 0,
    var descripcion: String = "",
    var tiempoSegundos: Int = 0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(recetaId)
        parcel.writeInt(numero)
        parcel.writeString(descripcion)
        parcel.writeInt(tiempoSegundos)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Step> {
        override fun createFromParcel(parcel: Parcel): Step = Step(parcel)
        override fun newArray(size: Int): Array<Step?> = arrayOfNulls(size)
    }
}