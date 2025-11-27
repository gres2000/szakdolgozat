import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import com.taskraze.myapplication.viewmodel.calendar.CalendarViewModel

class CalendarViewModelFactory(
    private val authViewModel: AuthViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(authViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
