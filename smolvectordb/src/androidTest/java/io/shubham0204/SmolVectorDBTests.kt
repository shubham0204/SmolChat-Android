package io.shubham0204

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.shubham0204.smolvectordb.SmolVectorDB
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmolVectorDBTests {

    private lateinit var db: SmolVectorDB

    @Before
    fun setUp() {
        db = SmolVectorDB()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndNearestNeighbor() {
        val embeddings =
            arrayOf(
                floatArrayOf(1.0f, 0.0f, 0.0f),
                floatArrayOf(0.0f, 1.0f, 0.0f),
                floatArrayOf(0.0f, 0.0f, 1.0f),
            )
        val texts = arrayOf("one", "two", "three")

        for (i in embeddings.indices) {
            db.insertRecord(texts[i], embeddings[i])
        }

        val query = floatArrayOf(0.8f, 0.1f, 0.1f)
        val results = db.nearestNeighbor(query, 1)

        assertEquals(1, results.size)
        assertEquals("one", results[0])
    }
}
