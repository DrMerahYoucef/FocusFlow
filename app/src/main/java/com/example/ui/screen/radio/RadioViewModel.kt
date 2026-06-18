package com.example.ui.screen.radio

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusFlowApplication
import com.example.data.RadioStation
import com.example.data.StationCatalogue
import com.example.data.db.entity.FavouriteStationEntity
import com.example.service.RadioPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FocusFlowApplication.instance.database

    val favouriteIds: StateFlow<Set<String>> =
        db.favouriteStationDao().getAllFavourites()
            .map { it.toSet() }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    private val _selectedCategory = MutableStateFlow<StationCatalogue.Category?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    val displayedStations: StateFlow<List<RadioStation>> =
        combine(favouriteIds, _selectedCategory) { favs, cat ->
            val all = StationCatalogue.stations
            val filtered = if (cat == null) all else all.filter { it.category == cat }
            filtered.sortedByDescending { it.id in favs }
        }.stateIn(viewModelScope, SharingStarted.Lazily, StationCatalogue.stations)

    private val _currentStation = MutableStateFlow<RadioStation?>(null)
    val currentStation = _currentStation.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    fun selectStation(station: RadioStation, context: Context) {
        _currentStation.value = station
        _isPlaying.value = true
        RadioPlayerService.play(context, station)
    }

    fun togglePlayback(context: Context) {
        if (_isPlaying.value) {
            RadioPlayerService.stop(context)
            _isPlaying.value = false
        } else {
            _currentStation.value?.let { selectStation(it, context) }
        }
    }

    fun toggleFavourite(stationId: String) {
        viewModelScope.launch {
            val isFav = db.favouriteStationDao().isFavourite(stationId) > 0
            if (isFav) db.favouriteStationDao().remove(stationId)
            else db.favouriteStationDao().add(FavouriteStationEntity(stationId))
        }
    }

    fun setCategory(cat: StationCatalogue.Category?) {
        _selectedCategory.value = cat
    }
}
