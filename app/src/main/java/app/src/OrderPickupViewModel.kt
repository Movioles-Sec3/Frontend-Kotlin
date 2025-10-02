package app.src

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

data class PickupUi(
    val code: String,
    val title: String,
    val subtitle: String
)

class OrderPickupViewModel : ViewModel() {

    private val _ui = MutableLiveData<PickupUi>()
    val ui: LiveData<PickupUi> = _ui

    fun load(initialCode: String?, title: String?, subtitle: String?) {
        viewModelScope.launch(Dispatchers.Default) {
            val code = initialCode?.takeIf { it.isNotBlank() } ?: generateCode()
            _ui.postValue(
                PickupUi(
                    code = code,
                    title = title ?: "Your order is ready!",
                    subtitle = subtitle ?: "Go to the pickup point and show your code."
                )
            )
        }
    }

    private fun generateCode(): String {
        val letters = (1..3).map { ('A'..'Z').random() }.joinToString("")
        val digits = Random.nextInt(1000, 9999).toString()
        return "$letters-$digits"
    }
}
