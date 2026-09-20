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

        assertEquals(219, bounds.size)
        assertEquals(7f, bounds.minOf { it.left }, 0f)
        assertEquals(1246f, bounds.maxOf { it.bottom }, 0f)
        assertTrue(bounds.all { it.left >= 0f && it.top >= 0f && it.right <= 688f && it.bottom <= 1536f })
    }

    @Test
    fun windowOrderIsAFullShuffleAndDoesNotUseSequentialIndexes() {
        val order = BuildingBackgroundRenderer.shuffledWindowOrder(1234L)

        assertEquals((0 until BuildingBackgroundRenderer.windowBounds.size).toSet(), order.toSet())
        assertNotEquals((0 until 55).toList(), order)
    }
}