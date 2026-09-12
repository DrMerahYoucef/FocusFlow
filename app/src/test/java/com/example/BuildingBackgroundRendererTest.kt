package com.example

import com.example.ui.components.BuildingBackgroundRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildingBackgroundRendererTest {

    @Test
    fun windowGridCoversEveryRowAndColumnInsideSourceImage() {
        val bounds = BuildingBackgroundRenderer.windowBounds

        assertEquals(55, bounds.size)
        assertEquals(158f, bounds.minOf { it.top }, 0f)
        assertEquals(581f, bounds.maxOf { it.bottom }, 0f)
        assertTrue(bounds.all { it.left >= 0f && it.top >= 0f && it.right <= 335f && it.bottom <= 745f })
    }

    @Test
    fun windowOrderIsAFullShuffleAndDoesNotUseSequentialIndexes() {
        val order = BuildingBackgroundRenderer.shuffledWindowOrder(1234L)

        assertEquals((0 until 55).toSet(), order.toSet())
        assertNotEquals((0 until 55).toList(), order)
    }
}