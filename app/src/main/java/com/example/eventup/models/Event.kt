package com.example.eventup.models

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.PropertyName

data class Event(
    var id: String = "", // Ensure id is a var and has a default value
    val name: String = "",
    val location: String = "",
    val date: String = "",
    val genres: String = "",
    val description: String = "",
    var interest: Int = 0,
    @get:PropertyName("isFavorite") @set:PropertyName("isFavorite") var isFavorite: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(location)
        parcel.writeString(date)
        parcel.writeString(genres)
        parcel.writeString(description)
        parcel.writeInt(interest)
        parcel.writeByte(if (isFavorite) 1 else 0)
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

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "location" to location,
            "date" to date,
            "genres" to genres,
            "description" to description,
            "interest" to interest,
            "isFavorite" to isFavorite
        )
    }
}
