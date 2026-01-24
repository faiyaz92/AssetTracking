package com.example.assettracking.util

/**
 * Utility class for generating hierarchical location codes
 * Pattern: LOC-A, LOC-B, ..., LOC-Z, LOC-AA, LOC-AB, ..., LOC-AZ, LOC-BA, etc.
 * For child locations: LOC-(parentcode)-A, LOC-(parentcode)-B, etc.
 */
object LocationCodeGenerator {

    private const val PREFIX = "LOC-"

    /**
     * Generates the next location code for a super parent location
     * @param existingCodes List of existing location codes at the same level
     * @return Next available location code
     */
    fun generateNextSuperParentCode(existingCodes: List<String>): String {
        if (existingCodes.isEmpty()) {
            return "${PREFIX}A"
        }

        // Extract the code parts after "LOC-"
        val codeParts = existingCodes.map { it.removePrefix(PREFIX) }

        // Find the highest code and increment it (length-aware so "AA" > "Z")
        val highestCode = codeParts.maxWithOrNull(codeComparator) ?: "A"
        val nextCode = incrementCode(highestCode)

        return "$PREFIX$nextCode"
    }

    /**
     * Generates the next location code for a child location
     * @param parentCode The parent's location code
     * @param existingChildCodes List of existing child location codes for this parent
     * @return Next available child location code
     */
    fun generateNextChildCode(parentCode: String, existingChildCodes: List<String>): String {
        if (existingChildCodes.isEmpty()) {
            return "$parentCode-A"
        }

        // Extract the child code parts (after parent code and dash)
        val parentPrefix = "$parentCode-"
        val childCodeParts = existingChildCodes
            .filter { it.startsWith(parentPrefix) }
            .map { it.removePrefix(parentPrefix) }

        // Find the highest child code and increment it (length-aware so "AA" > "Z")
        val highestChildCode = childCodeParts.maxWithOrNull(codeComparator) ?: "A"
        val nextChildCode = incrementCode(highestChildCode)

        return "$parentCode-$nextChildCode"
    }

    /**
     * Increments a code string (A -> B, Z -> AA, AZ -> BA, etc.)
     */
    private fun incrementCode(code: String): String {
        val chars = code.toCharArray()
        var carry = true

        // Start from the end and increment
        for (i in chars.indices.reversed()) {
            if (!carry) break

            if (chars[i] < 'Z') {
                chars[i] = chars[i] + 1
                carry = false
            } else {
                chars[i] = 'A'
                carry = true
            }
        }

        // If we still have carry after processing all characters, add a new 'A'
        val result = String(chars)
        return if (carry) "A$result" else result
    }

    // Compare by length first, then lexicographically, so AA > Z and AAA > ZZ
    private val codeComparator = compareBy<String> { it.length }.thenBy { it }

    /**
     * Extracts the parent code from a child location code
     * @param childCode Full child location code (e.g., "LOC-A-B")
     * @return Parent code (e.g., "LOC-A") or null if not a child code
     */
    fun getParentCode(childCode: String): String? {
        val parts = childCode.split("-")
        return if (parts.size >= 3 && parts[0] == "LOC") {
            "${parts[0]}-${parts[1]}"
        } else {
            null
        }
    }

    /**
     * Checks if a code represents a super parent location (no child suffix)
     */
    fun isSuperParentCode(code: String): Boolean {
        val parts = code.split("-")
        return parts.size == 2 && parts[0] == "LOC" && parts[1].length == 1
    }

    /**
     * Checks if a code represents a child location
     */
    fun isChildCode(code: String): Boolean {
        val parts = code.split("-")
        return parts.size >= 3 && parts[0] == "LOC"
    }
}