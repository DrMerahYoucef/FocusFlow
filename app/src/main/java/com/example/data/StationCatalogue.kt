package com.example.data

object StationCatalogue {

    val stations = listOf(

        // ── ALGERIAN RADIO ──────────────────────────────────────────
        RadioStation(
            id        = "dz_chaine3",
            name      = "Chaîne 3",
            country   = "🇩🇿 Algeria",
            category  = Category.ALGERIAN,
            streamUrl = "https://stream.radios-algerie.dz/chaine3",
            fallbackUrl = "https://mediaserver.radios-algerie.dz/chaine3/mp3",
            logoUrl   = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7f/Cha%C3%AEne_3_logo.svg/200px-Cha%C3%AEne_3_logo.svg.png",
            description = "French-language Algerian national radio"
        ),
        RadioStation(
            id        = "dz_radioalger",
            name      = "Radio Alger Internationale",
            country   = "🇩🇿 Algeria",
            category  = Category.ALGERIAN,
            streamUrl = "https://mediaserver.radios-algerie.dz/alger-international/mp3",
            logoUrl   = "",
            description = "International Algerian broadcast"
        ),
        RadioStation(
            id        = "dz_jil_fm",
            name      = "Jil FM",
            country   = "🇩🇿 Algeria",
            category  = Category.ALGERIAN,
            streamUrl = "https://mediaserver.radios-algerie.dz/jilfm/mp3",
            logoUrl   = "",
            description = "Popular Algerian music radio"
        ),
        RadioStation(
            id        = "dz_quran",
            name      = "Radio Coran",
            country   = "🇩🇿 Algeria",
            category  = Category.ALGERIAN,
            streamUrl = "https://mediaserver.radios-algerie.dz/coran/mp3",
            logoUrl   = "",
            description = "Algerian Holy Quran radio"
        ),

        // ── STUDY WITH ME ────────────────────────────────────────────
        RadioStation(
            id        = "lofi_hiphop",
            name      = "Lo-Fi Hip Hop",
            country   = "🌍 Global",
            category  = Category.STUDY,
            streamUrl = "https://streams.ilovemusic.de/iloveradio17.mp3",
            logoUrl   = "",
            description = "Chill beats to study and relax to"
        ),
        RadioStation(
            id        = "study_classical",
            name      = "Classical Focus",
            country   = "🌍 Global",
            category  = Category.STUDY,
            streamUrl = "https://live.musopen.org:8085/streamvbr0",
            logoUrl   = "",
            description = "Classical music — royalty free"
        ),
        RadioStation(
            id        = "cafe_jazz",
            name      = "Jazz Café",
            country   = "🌍 Global",
            category  = Category.STUDY,
            streamUrl = "https://streams.ilovemusic.de/iloveradio29.mp3",
            logoUrl   = "",
            description = "Smooth jazz for deep work"
        ),
        RadioStation(
            id        = "ambient_space",
            name      = "Ambient Space",
            country   = "🌍 Global",
            category  = Category.STUDY,
            streamUrl = "https://ice1.somafm.com/deepspaceone-128-mp3",
            logoUrl   = "",
            description = "Deep space ambient for long sessions"
        ),
        RadioStation(
            id        = "brown_noise",
            name      = "Brown Noise",
            country   = "🌍 Global",
            category  = Category.STUDY,
            streamUrl = "https://ice1.somafm.com/darkzone-128-mp3",
            logoUrl   = "",
            description = "Noise masking for maximum focus"
        ),
        RadioStation(
            id        = "piano_study",
            name      = "Piano Study",
            country   = "🌍 Global",
            category  = Category.STUDY,
            streamUrl = "https://streams.ilovemusic.de/iloveradio2.mp3",
            logoUrl   = "",
            description = "Solo piano — no lyrics, pure focus"
        ),

        // ── GLOBAL RADIO ─────────────────────────────────────────────
        RadioStation(
            id        = "bbc_radio4",
            name      = "BBC Radio 4",
            country   = "🇬🇧 UK",
            category  = Category.GLOBAL,
            streamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_fourfm",
            logoUrl   = "",
            description = "Talk radio — news and culture"
        ),
        RadioStation(
            id        = "france_inter",
            name      = "France Inter",
            country   = "🇫🇷 France",
            category  = Category.GLOBAL,
            streamUrl = "https://icecast.radiofrance.fr/franceinter-hifi.aac",
            logoUrl   = "",
            description = "French public radio"
        ),
        RadioStation(
            id        = "monte_carlo",
            name      = "Radio Monte Carlo",
            country   = "🇫🇷 France",
            category  = Category.GLOBAL,
            streamUrl = "https://icy.unitedradio.it/RMC.mp3",
            logoUrl   = "",
            description = "French pop and hits"
        ),
        RadioStation(
            id        = "soma_groove",
            name      = "SomaFM Groove Salad",
            country   = "🌍 Global",
            category  = Category.GLOBAL,
            streamUrl = "https://ice1.somafm.com/groovesalad-128-mp3",
            logoUrl   = "",
            description = "A nicely chilled plate of ambient"
        )
    )

    enum class Category { ALGERIAN, STUDY, GLOBAL }
}

data class RadioStation(
    val id:          String,
    val name:        String,
    val country:     String,
    val category:    StationCatalogue.Category,
    val streamUrl:   String,
    val fallbackUrl: String = "",
    val logoUrl:     String,
    val description: String
)
