package com.example.ui.screen.radio

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusFlowApplication
import com.example.data.RadioStation
import com.example.data.db.entity.CategoryEntity
import com.example.data.db.entity.StationEntity
import com.example.data.db.entity.FavouriteStationEntity
import com.example.service.RadioPlayerService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FocusFlowApplication.instance.database

    init {
        viewModelScope.launch {
            try {
                // Proactively purge default Algerian database entries from previous runs so they disappear instantly
                db.radioDao().deleteStationsByCategoryId("ALGERIAN")
                db.radioDao().deleteCategoryForce("ALGERIAN")

                if (db.radioDao().getCategoryCount() == 0) {
                    val defaultCategories = listOf(
                        CategoryEntity(id = "STUDY", name = "Study", isCustom = false),
                        CategoryEntity(id = "GLOBAL", name = "Global", isCustom = false)
                    )
                    db.radioDao().insertCategories(defaultCategories)

                    val defaultStations = listOf(
                        StationEntity(
                            id = "lofi_hiphop",
                            name = "Lo-Fi Hip Hop",
                            country = "🌍 Global",
                            categoryId = "STUDY",
                            streamUrl = "https://streams.ilovemusic.de/iloveradio17.mp3",
                            description = "Chill beats to study and relax to",
                            isCustom = false
                        ),
                        StationEntity(
                            id = "study_classical",
                            name = "Classical Focus",
                            country = "🌍 Global",
                            categoryId = "STUDY",
                            streamUrl = "https://live.musopen.org:8085/streamvbr0",
                            description = "Classical music — royalty free",
                            isCustom = false
                        ),
                        StationEntity(
                            id = "cafe_jazz",
                            name = "Jazz Café",
                            country = "🌍 Global",
                            categoryId = "STUDY",
                            streamUrl = "https://streams.ilovemusic.de/iloveradio29.mp3",
                            description = "Smooth jazz for deep work",
                            isCustom = false
                        ),
                        StationEntity(
                            id = "ambient_space",
                            name = "Ambient Space",
                            country = "🌍 Global",
                            categoryId = "STUDY",
                            streamUrl = "https://ice1.somafm.com/deepspaceone-128-mp3",
                            description = "Deep space ambient for long sessions",
                            isCustom = false
                        ),
                        StationEntity(
                            id = "brown_noise",
                            name = "Brown Noise",
                            country = "🌍 Global",
                            categoryId = "STUDY",
                            streamUrl = "https://ice1.somafm.com/darkzone-128-mp3",
                            description = "Noise masking for maximum focus",
                            isCustom = false
                        ),
                        StationEntity(
                            id = "piano_study",
                            name = "Piano Study",
                            country = "🌍 Global",
                            categoryId = "STUDY",
                            streamUrl = "https://streams.ilovemusic.de/iloveradio2.mp3",
                            description = "Solo piano — no lyrics, pure focus",
                            isCustom = false
                        ),
                        StationEntity(
                            id = "bbc_radio4",
                            name = "BBC Radio 4",
                            country = "🇬🇧 UK",
                            categoryId = "GLOBAL",
                            streamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_fourfm",
                            description = "Talk radio — news and culture",
                            isCustom = false
                        ),
                        StationEntity(
                            id = "france_inter",
                            name = "France Inter",
                            country = "🇫🇷 France",
                            categoryId = "GLOBAL",
                            streamUrl = "https://icecast.radiofrance.fr/franceinter-hifi.aac",
                            description = "French public radio",
                            isCustom = false
                        ),
                        StationEntity(
                            id = "monte_carlo",
                            name = "Radio Monte Carlo",
                            country = "🇫🇷 France",
                            categoryId = "GLOBAL",
                            streamUrl = "https://icy.unitedradio.it/RMC.mp3",
                            description = "French pop and hits",
                            isCustom = false
                        ),
                        StationEntity(
                            id = "soma_groove",
                            name = "SomaFM Groove Salad",
                            country = "🌍 Global",
                            categoryId = "GLOBAL",
                            streamUrl = "https://ice1.somafm.com/groovesalad-128-mp3",
                            description = "A nicely chilled plate of ambient",
                            isCustom = false
                        )
                    )
                    db.radioDao().insertStations(defaultStations)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val favouriteIds: StateFlow<Set<String>> =
        db.favouriteStationDao().getAllFavourites()
            .map { it.toSet() }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> =
        db.radioDao().getAllCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayedStations: StateFlow<List<RadioStation>> =
        combine(
            db.radioDao().getAllStations(),
            favouriteIds,
            _selectedCategoryId
        ) { stations, favs, catId ->
            val mapped = stations.map { it.toRadioStation() }
            val filtered = if (catId == null) mapped else mapped.filter { it.categoryId == catId }
            filtered.sortedByDescending { it.id in favs }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun setCategory(catId: String?) {
        _selectedCategoryId.value = catId
    }

    // Dynamic execution actions
    fun addCustomCategory(name: String) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            db.radioDao().insertCategory(CategoryEntity(id = id, name = name, isCustom = true))
        }
    }

    fun removeCategory(id: String) {
        viewModelScope.launch {
            db.radioDao().deleteCategoryAndStations(id)
            if (_selectedCategoryId.value == id) {
                _selectedCategoryId.value = null
            }
        }
    }

    fun addCustomStation(name: String, url: String, categoryId: String) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            db.radioDao().insertStation(
                StationEntity(
                    id = id,
                    name = name,
                    country = "Custom Stream",
                    categoryId = categoryId,
                    streamUrl = url,
                    description = "Custom user-added stream",
                    isCustom = true
                )
            )
        }
    }

    fun removeStation(id: String) {
        viewModelScope.launch {
            db.radioDao().deleteCustomStation(id)
        }
    }
}

// Extension to map DB entity to UI model
fun StationEntity.toRadioStation(): RadioStation {
    return RadioStation(
        id = id,
        name = name,
        country = country,
        categoryId = categoryId,
        streamUrl = streamUrl,
        fallbackUrl = fallbackUrl,
        logoUrl = logoUrl,
        description = description,
        isCustom = isCustom
    )
}
