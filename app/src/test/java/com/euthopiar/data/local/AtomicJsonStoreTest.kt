package com.euthopiar.data.local

import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Serializable
data class TestModel(val id: String, val count: Int)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AtomicJsonStoreTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Test
    fun testDefaultReturnedWhenFileMissing() = runBlocking {
        val file = tempFolder.newFile("missing.json").apply { delete() }
        val store = AtomicJsonStore(
            file = file,
            serializer = TestModel.serializer(),
            default = TestModel("default", 0)
        )

        val result = store.read()
        assertEquals("default", result.id)
        assertEquals(0, result.count)
    }

    @Test
    fun testWriteAndReadRoundtrip() = runBlocking {
        val file = File(tempFolder.root, "test.json")
        val store = AtomicJsonStore(
            file = file,
            serializer = TestModel.serializer(),
            default = TestModel("default", 0)
        )

        store.write(TestModel("saved", 42))
        val readBack = store.read()
        assertEquals("saved", readBack.id)
        assertEquals(42, readBack.count)

        // Second write to test overwrite
        store.write(TestModel("saved2", 84))
        val readBack2 = store.read()
        assertEquals("saved2", readBack2.id)
        assertEquals(84, readBack2.count)
    }

    @Test
    fun testAtomicUpdate() = runBlocking {
        val file = File(tempFolder.root, "update.json")
        val store = AtomicJsonStore(
            file = file,
            serializer = TestModel.serializer(),
            default = TestModel("item", 1)
        )

        store.write(TestModel("item", 10))
        val updated = store.update { it.copy(count = it.count + 5) }
        assertEquals(15, updated.count)

        val readBack = store.read()
        assertEquals(15, readBack.count)
    }

    @Test
    fun testCorruptedFileFallsBackToDefaultWithoutCrashing() = runBlocking {
        val file = tempFolder.newFile("corrupt.json")
        file.writeText("{{{NOT_VALID_JSON@@@")

        val store = AtomicJsonStore(
            file = file,
            serializer = TestModel.serializer(),
            default = TestModel("fallback", 999)
        )

        val result = store.read()
        assertEquals("fallback", result.id)
        assertEquals(999, result.count)
    }
}
