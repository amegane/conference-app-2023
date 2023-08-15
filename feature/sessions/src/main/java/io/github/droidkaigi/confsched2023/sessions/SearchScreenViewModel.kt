package io.github.droidkaigi.confsched2023.sessions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.droidkaigi.confsched2023.designsystem.strings.AppStrings
import io.github.droidkaigi.confsched2023.model.DroidKaigi2023Day
import io.github.droidkaigi.confsched2023.model.SessionsRepository
import io.github.droidkaigi.confsched2023.model.Timetable
import io.github.droidkaigi.confsched2023.model.TimetableCategory
import io.github.droidkaigi.confsched2023.model.TimetableLanguage
import io.github.droidkaigi.confsched2023.model.TimetableSessionType
import io.github.droidkaigi.confsched2023.sessions.component.SearchFilterUiState
import io.github.droidkaigi.confsched2023.ui.UserMessageStateHolder
import io.github.droidkaigi.confsched2023.ui.buildUiState
import io.github.droidkaigi.confsched2023.ui.handleErrorAndRetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

const val SEARCH_QUERY = "searchQuery"

@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    sessionsRepository: SessionsRepository,
    userMessageStateHolder: UserMessageStateHolder,
) : ViewModel() {
    private val searchQuery = savedStateHandle.getStateFlow(SEARCH_QUERY, "")

    private val sessionsStateFlow: StateFlow<Timetable> = sessionsRepository
        .getTimetableStream()
        .handleErrorAndRetry(
            AppStrings.Retry,
            userMessageStateHolder,
        )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Timetable(),
        )

    private val searchFilterUiState: MutableStateFlow<SearchFilterUiState> = MutableStateFlow(
        SearchFilterUiState(),
    )

    val uiState: StateFlow<SearchScreenUiState> = buildUiState(
        searchQuery,
        searchFilterUiState,
        sessionsStateFlow,
    ) { searchQuery, searchFilterUiState, sessions ->
        SearchScreenUiState(
            searchQuery = searchQuery,
            searchFilterUiState = searchFilterUiState.copy(
                categories = sessions.categories,
                languages = sessions.languages,
                sessionTypes = sessions.sessionTypes,
            ),
        )
    }

    fun onSearchQueryChanged(searchQuery: String) {
        savedStateHandle[SEARCH_QUERY] = searchQuery
    }

    fun onDaySelected(day: DroidKaigi2023Day, isSelected: Boolean) {
        val selectedDays = searchFilterUiState.value.selectedDays.toMutableList()
        searchFilterUiState.value = searchFilterUiState.value.copy(
            selectedDays = selectedDays.apply {
                if (isSelected) {
                    add(day)
                } else {
                    remove(day)
                }
            }.sortedBy(DroidKaigi2023Day::start),
        )
    }

    fun onCategoriesSelected(category: TimetableCategory, isSelected: Boolean) {
        val selectedCategories = searchFilterUiState.value.selectedCategories.toMutableList()
        searchFilterUiState.value = searchFilterUiState.value.copy(
            selectedCategories = selectedCategories.apply {
                if (isSelected) {
                    add(category)
                } else {
                    remove(category)
                }
            },
        )
    }

    fun onSessionTypesSelected(sessionType: TimetableSessionType, isSelected: Boolean) {
        val selectedSessionTypes = searchFilterUiState.value.selectedSessionTypes.toMutableList()
        searchFilterUiState.value = searchFilterUiState.value.copy(
            selectedSessionTypes = selectedSessionTypes.apply {
                if (isSelected) {
                    add(sessionType)
                } else {
                    remove(sessionType)
                }
            },
        )
    }

    fun onLanguagesSelected(language: TimetableLanguage, isSelected: Boolean) {
        val selectedLanguages = searchFilterUiState.value.selectedLanguages.toMutableList()
        searchFilterUiState.value = searchFilterUiState.value.copy(
            selectedLanguages = selectedLanguages.apply {
                if (isSelected) {
                    add(language)
                } else {
                    remove(language)
                }
            },
        )
    }
}
