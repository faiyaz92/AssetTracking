package com.example.assettracking.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationCodeGeneratorTest {

    @Test
    fun testSuperParentCodes() {
        val existing = listOf("LOC-A", "LOC-B", "LOC-Z")
        val next = LocationCodeGenerator.generateNextSuperParentCode(existing)
        assertEquals("LOC-AA", next)
    }

    @Test
    fun testChildCodes() {
        val parentCode = "LOC-AAB-BAC"
        val existingChildren = listOf("LOC-AAB-BAC-A", "LOC-AAB-BAC-B", "LOC-AAB-BAC-Z")
        val next = LocationCodeGenerator.generateNextChildCode(parentCode, existingChildren)
        assertEquals("LOC-AAB-BAC-AA", next)
    }

    @Test
    fun testIncrementCode() {
        // Test the private incrementCode function via reflection or public methods
        val gen = LocationCodeGenerator
        val nextA = gen.javaClass.getDeclaredMethod("incrementCode", String::class.java)
            .apply { isAccessible = true }
            .invoke(gen, "A") as String
        assertEquals("B", nextA)

        val nextZ = gen.javaClass.getDeclaredMethod("incrementCode", String::class.java)
            .apply { isAccessible = true }
            .invoke(gen, "Z") as String
        assertEquals("AA", nextZ)

        val nextAZ = gen.javaClass.getDeclaredMethod("incrementCode", String::class.java)
            .apply { isAccessible = true }
            .invoke(gen, "AZ") as String
        assertEquals("BA", nextAZ)
    }
}