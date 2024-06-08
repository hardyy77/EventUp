import com.example.eventup.Event
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreUtils {

    private val firestore = FirebaseFirestore.getInstance()

    fun saveEvent(event: Event, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("events")
            .add(event.toMap())
            .addOnSuccessListener { documentReference ->
                onSuccess(documentReference.id)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getEvents(onSuccess: (List<Event>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("events")
            .get()
            .addOnSuccessListener { result ->
                val events = result.map { document ->
                    Event(
                        document.getString("name") ?: "",
                        document.getString("location") ?: "",
                        document.getString("date") ?: "",
                        document.getString("genres") ?: "",
                        document.getString("description") ?: "",
                        document.getLong("interest")?.toInt() ?: 0
                    )
                }
                onSuccess(events)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}
