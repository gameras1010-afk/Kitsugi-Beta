package com.kitsugi.animelist

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testStaffFetch() = kotlinx.coroutines.runBlocking {
        val client = com.kitsugi.animelist.data.remote.KitsugiStaffClient()
        val result = client.fetchStaffDetail("jikan", 43814)
        println("Fetched staff: ${result?.name}")
        println("Character roles count: ${result?.characterRoles?.size}")
        result?.characterRoles?.take(5)?.forEach { role ->
            println(" - Char: ${role.characterName}, Media: ${role.mediaTitle}")
        }
        assertNotNull(result)
        assertTrue(result!!.characterRoles.isNotEmpty())
    }
}