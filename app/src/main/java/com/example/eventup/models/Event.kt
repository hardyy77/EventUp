package com.example.eventup.models

import android.os.Parcel
import android.os.Parcelable

data class Event(
    var id: String = "", // Ensure id is a var and has a default value
    val name: String = "",
    val location: String = "",
    val date: String = "",
    val genres: String = "",
    val description: String = "",
    var interest: Int = 0,
    var isFavorite: Boolean = false // Dodane pole
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readByte() != 0.toByte() // Czytanie pola isFavorite z Parcel
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(location)
        parcel.writeString(date)
        parcel.writeString(genres)
        parcel.writeString(description)
        parcel.writeInt(interest)
        parcel.writeByte(if (isFavorite) 1 else 0) // Zapisywanie pola isFavorite do Parcel
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Event> {
        override fun createFromParcel(parcel: Parcel): Event {
            return Event(parcel)
        }

        override fun newArray(size: Int): Array<Event?> {
            return arrayOfNulls(size)
        }
    }
}
