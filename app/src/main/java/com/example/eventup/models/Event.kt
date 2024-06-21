package com.example.eventup.models

import android.os.Parcel
import android.os.Parcelable

data class Event(
    var id: Int = 0, // Identyfikator wydarzenia jako Int
    val name: String = "", // Nazwa wydarzenia jako String
    val location: String = "", // Lokalizacja wydarzenia jako String
    val date: String = "", // Data wydarzenia jako String
    val genres: String = "", // Gatunki związane z wydarzeniem jako String
    val description: String = "", // Opis wydarzenia jako String
    var interest: Int = 0, // Liczba osób zainteresowanych wydarzeniem jako Int
    var isFavorite: Boolean = false, // Flaga oznaczająca, czy wydarzenie jest ulubione
    val addedDate: String = "" // Data dodania wydarzenia jako String
) : Parcelable {

    // Konstruktor odczytujący dane z obiektu Parcel
    constructor(parcel: Parcel) : this(
        parcel.readInt(), // Odczytanie wartości id jako Int
        parcel.readString() ?: "", // Odczytanie wartości name lub pusty string, jeśli null
        parcel.readString() ?: "", // Odczytanie wartości location lub pusty string, jeśli null
        parcel.readString() ?: "", // Odczytanie wartości date lub pusty string, jeśli null
        parcel.readString() ?: "", // Odczytanie wartości genres lub pusty string, jeśli null
        parcel.readString() ?: "", // Odczytanie wartości description lub pusty string, jeśli null
        parcel.readInt(), // Odczytanie wartości interest jako Int
        parcel.readByte() != 0.toByte(), // Odczytanie wartości isFavorite jako Boolean
        parcel.readString() ?: "" // Odczytanie wartości addedDate lub pusty string, jeśli null
    )

    // Funkcja zapisująca dane do obiektu Parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id) // Zapisanie wartości id jako Int
        parcel.writeString(name) // Zapisanie wartości name jako String
        parcel.writeString(location) // Zapisanie wartości location jako String
        parcel.writeString(date) // Zapisanie wartości date jako String
        parcel.writeString(genres) // Zapisanie wartości genres jako String
        parcel.writeString(description) // Zapisanie wartości description jako String
        parcel.writeInt(interest) // Zapisanie wartości interest jako Int
        parcel.writeByte(if (isFavorite) 1 else 0) // Zapisanie wartości isFavorite jako Byte
        parcel.writeString(addedDate) // Zapisanie wartości addedDate jako String
    }

    // Funkcja zwracająca opis zawartości Parcel
    override fun describeContents(): Int {
        return 0
    }

    // Obiekt towarzyszący implementujący interfejs Parcelable.Creator dla klasy Event
    companion object CREATOR : Parcelable.Creator<Event> {
        // Tworzy obiekt Event na podstawie Parcel
        override fun createFromParcel(parcel: Parcel): Event {
            return Event(parcel)
        }

        // Tworzy tablicę obiektów Event o podanym rozmiarze
        override fun newArray(size: Int): Array<Event?> {
            return arrayOfNulls(size)
        }
    }
}
