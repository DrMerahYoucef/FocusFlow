package com.focusisland.ui.screen.exams

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focusisland.FocusFlowApplication
import com.focusisland.data.db.entity.ExamEntity
import com.focusisland.widget.ExamCountdownWidgetReceiver
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExamsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FocusFlowApplication.instance.examRepository

    val exams: StateFlow<List<ExamEntity>> = repository.getAllExams()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addExam(name: String, subject: String, examDateEpoch: Long, color: Int) {
        viewModelScope.launch {
            repository.insert(
                ExamEntity(
                    name = name,
                    subject = subject,
                    examDate = examDateEpoch,
                    color = color
                )
            )
            ExamCountdownWidgetReceiver.triggerWidgetUpdate(getApplication())
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.delete(exam)
            ExamCountdownWidgetReceiver.triggerWidgetUpdate(getApplication())
        }
    }
}
