package com.example.util

import java.util.Calendar
import kotlin.math.*

object SunriseSunsetCalculator {
    /**
     * Determines whether it is currently daytime at the given latitude and longitude.
     */
    fun isDaytime(latitude: Double, longitude: Double): Boolean {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        
        // Convert latitude to radians
        val latRad = Math.toRadians(latitude)
        
        // Calculate the Sun's declination
        // Declination = 23.45 * sin(2 * pi * (284 + dayOfYear) / 365)
        val declination = Math.toRadians(23.45 * sin(2.0 * Math.PI * (284 + dayOfYear) / 365.0))
        
        // Hour angle (H) where zenith is 90.833 degrees (official sunrise/sunset)
        // cos(H) = (cos(90.833) - sin(lat) * sin(dec)) / (cos(lat) * cos(dec))
        // cos(90.833) is approx -0.01454
        val cosDenominator = cos(latRad) * cos(declination)
        if (cosDenominator == 0.0) {
            return true // Fallback to daytime
        }
        val cosH = (-0.01454 - sin(latRad) * sin(declination)) / cosDenominator
        
        if (cosH >= 1.0) {
            // Sun never rises (polar night)
            return false
        } else if (cosH <= -1.0) {
            // Sun never sets (polar day)
            return true
        }
        
        val h = Math.toDegrees(acos(cosH)) // half day length in degrees
        
        // Solar time for solar noon (approximate)
        // B = 360/365 * (d - 81)
        // EoT = 9.87 * sin(2B) - 7.53 * cos(B) - 1.5 * sin(B)
        val b = Math.toRadians((360.0 / 365.0) * (dayOfYear - 81))
        val equationOfTime = 9.87 * sin(2.0 * b) - 7.53 * cos(b) - 1.5 * sin(b) // in minutes
        
        // Local standard meridian longitude = timezoneOffsetInHours * 15
        val tzOffset = calendar.timeZone.getOffset(calendar.timeInMillis) / (1000.0 * 60.0 * 60.0)
        val lstM = tzOffset * 15.0
        
        // Longitude correction (in minutes)
        val longCorr = 4.0 * (longitude - lstM)
        
        // Solar Noon in local time (hours from midnight)
        val solarNoon = 12.0 - (longCorr + equationOfTime) / 60.0
        
        // Sunrise and sunset in local time hours
        val sunriseHour = solarNoon - (h / 15.0)
        val sunsetHour = solarNoon + (h / 15.0)
        
        // Current local hour of the day as decimal
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY) + 
                          calendar.get(Calendar.MINUTE) / 60.0 + 
                          calendar.get(Calendar.SECOND) / 3600.0
        
        return currentHour in sunriseHour..sunsetHour
    }
}
