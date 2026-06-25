package com.example.data

data class RadioStation(
    val id:          String,
    val name:        String,
    val country:     String,
    val categoryId:  String,
    val streamUrl:   String,
    val fallbackUrl: String = "",
    val logoUrl:     String = "",
    val description: String,
    val isCustom:    Boolean = false
)
