package com.example.ui.screen.radio

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusFlowApplication
import com.example.data.RadioStation
import com.example.data.api.RadioBrowserClient
import com.example.data.db.entity.CategoryEntity
import com.example.data.db.entity.StationEntity
import com.example.data.db.entity.FavouriteStationEntity
import com.example.service.RadioPlayerService
import android.content.ComponentName
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
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
                    
                    // Pre-favourite all the default focus/study channels so they appear in Favorites by default
                    defaultStations.forEach { station ->
                        db.favouriteStationDao().add(FavouriteStationEntity(station.id))
                    }
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

    private var mediaController: MediaController? = null
    private val pendingActions = mutableListOf<(MediaController) -> Unit>()

    private fun initController(context: Context) {
        if (mediaController != null) return
        val sessionToken = SessionToken(
            context,
            ComponentName(context, RadioPlayerService::class.java)
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                mediaController = controller
                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        mediaItem?.mediaMetadata?.let { meta ->
                            val title = meta.title?.toString()
                            if (title != null && _currentStation.value?.name != title) {
                                _currentStation.value = RadioStation(
                                    id = mediaItem.mediaId,
                                    name = title,
                                    country = meta.artist?.toString() ?: "",
                                    categoryId = "",
                                    streamUrl = mediaItem.localConfiguration?.uri?.toString() ?: "",
                                    logoUrl = meta.artworkUri?.toString() ?: "",
                                    description = meta.description?.toString() ?: "",
                                    isCustom = false
                                )
                            }
                        }
                    }
                })
                _isPlaying.value = controller.isPlaying

                val actions = ArrayList(pendingActions)
                pendingActions.clear()
                actions.forEach { it(controller) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, context.mainExecutor)
    }

    private fun executeWhenControllerReady(context: Context, action: (MediaController) -> Unit) {
        initController(context)
        val controller = mediaController
        if (controller != null) {
            action(controller)
        } else {
            pendingActions.add(action)
        }
    }

    fun selectStation(station: RadioStation, context: Context) {
        _currentStation.value = station
        _isPlaying.value = true
        executeWhenControllerReady(context) { controller ->
            val artwork = station.logoUrl.ifEmpty {
                "android.resource://com.focusflow/drawable/ic_radio_default"
            }
            val mediaItem = MediaItem.Builder()
                .setMediaId(station.id)
                .setUri(station.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(station.name)
                        .setArtist(station.country)
                        .setDescription(station.description)
                        .setArtworkUri(Uri.parse(artwork))
                        .build()
                )
                .build()

            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        }
    }

    fun togglePlayback(context: Context) {
        executeWhenControllerReady(context) { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                if (controller.mediaItemCount > 0) {
                    controller.play()
                } else {
                    _currentStation.value?.let { selectStation(it, context) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
        mediaController = null
    }

    fun toggleFavourite(station: RadioStation) {
        viewModelScope.launch {
            val isFav = db.favouriteStationDao().isFavourite(station.id) > 0
            if (isFav) {
                db.favouriteStationDao().remove(station.id)
            } else {
                // Ensure station details are stored so we can show them offline/in local favorites
                db.radioDao().insertStation(
                    StationEntity(
                        id = station.id,
                        name = station.name,
                        country = station.country,
                        categoryId = "STUDY",
                        streamUrl = station.streamUrl,
                        logoUrl = station.logoUrl,
                        description = station.description,
                        isCustom = true
                    )
                )
                db.favouriteStationDao().add(FavouriteStationEntity(station.id))
            }
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

    // Global Discover & Search properties
    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery = _globalSearchQuery.asStateFlow()

    private val _globalSearchResults = MutableStateFlow<List<RadioStation>>(emptyList())
    val globalSearchResults = _globalSearchResults.asStateFlow()

    private val _globalTrendingStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val globalTrendingStations = _globalTrendingStations.asStateFlow()

    private val _isDiscoverSearching = MutableStateFlow(false)
    val isDiscoverSearching = _isDiscoverSearching.asStateFlow()

    private val _isDiscoverTrendingLoading = MutableStateFlow(false)
    val isDiscoverTrendingLoading = _isDiscoverTrendingLoading.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    private val _trendingError = MutableStateFlow<String?>(null)
    val trendingError = _trendingError.asStateFlow()

    fun searchGlobalStations(query: String) {
        _globalSearchQuery.value = query
        if (query.trim().length < 2) {
            _globalSearchResults.value = emptyList()
            _searchError.value = null
            return
        }

        viewModelScope.launch {
            _isDiscoverSearching.value = true
            _searchError.value = null
            try {
                val results = RadioBrowserClient.api.searchStations(name = query, limit = 40)
                _globalSearchResults.value = results.map { response ->
                    RadioStation(
                        id = response.stationuuid,
                        name = response.name.takeIf { it.isNotBlank() } ?: "Unknown Station",
                        country = response.country?.trim()?.takeIf { it.isNotBlank() } ?: "🌍 Global",
                        categoryId = "",
                        streamUrl = response.urlResolved ?: response.url ?: "",
                        logoUrl = response.favicon ?: "",
                        description = response.tags?.trim()?.takeIf { it.isNotBlank() } ?: response.language ?: "Global Stream",
                        isCustom = false
                    )
                }
            } catch (e: Exception) {
                _searchError.value = "Failed to load search results: ${e.localizedMessage ?: "Network error"}"
            } finally {
                _isDiscoverSearching.value = false
            }
        }
    }

    fun loadTrendingStations() {
        if (_globalTrendingStations.value.isNotEmpty()) return // already loaded or loading

        viewModelScope.launch {
            _isDiscoverTrendingLoading.value = true
            _trendingError.value = null
            try {
                val topClicks = RadioBrowserClient.api.getTopClick(limit = 40)
                _globalTrendingStations.value = topClicks.map { response ->
                    RadioStation(
                        id = response.stationuuid,
                        name = response.name.takeIf { it.isNotBlank() } ?: "Unknown Station",
                        country = response.country?.trim()?.takeIf { it.isNotBlank() } ?: "🌍 Global",
                        categoryId = "",
                        streamUrl = response.urlResolved ?: response.url ?: "",
                        logoUrl = response.favicon ?: "",
                        description = response.tags?.trim()?.takeIf { it.isNotBlank() } ?: response.language ?: "Popular Stream",
                        isCustom = false
                    )
                }
            } catch (e: Exception) {
                _trendingError.value = "Failed to load trending stations: ${e.localizedMessage ?: "Network error"}"
            } finally {
                _isDiscoverTrendingLoading.value = false
            }
        }
    }

    fun saveDiscoveredStation(station: RadioStation, categoryId: String) {
        viewModelScope.launch {
            db.radioDao().insertStation(
                StationEntity(
                    id = station.id,
                    name = station.name,
                    country = station.country,
                    categoryId = categoryId,
                    streamUrl = station.streamUrl,
                    logoUrl = station.logoUrl,
                    description = station.description,
                    isCustom = true
                )
            )
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
