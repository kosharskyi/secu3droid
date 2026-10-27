package org.secu3.android.utils

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import org.secu3.android.ui.logger.SecuLogsViewModel

class FileHelperSafCleanupTest {
    private lateinit var root: File
    private lateinit var context: Context
    private lateinit var helper: FileHelper
    private lateinit var provider: FakeDocumentsProvider
    private lateinit var treeUri: Uri

    @Before
    fun setUp() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        root = File(base.cacheDir, "saf-cleanup-${System.nanoTime()}").apply { mkdirs() }
        provider = FakeDocumentsProvider(File(root, "provider-output"))
        val resolver = ContentResolver.wrap(provider)
        context = object : ContextWrapper(base) {
            override fun getFilesDir() = File(root, "files").also { it.mkdirs() }
            override fun getCacheDir() = File(root, "cache").also { it.mkdirs() }
            override fun getContentResolver() = resolver
        }
        helper = FileHelper(context)
        treeUri = DocumentsContract.buildTreeDocumentUri(AUTHORITY, "primary:TestLogs")
    }

    @After
    fun tearDown() {
        if (::root.isInitialized) root.deleteRecursively()
    }

    @Test
    fun customTreeExportKeepsSuccessfullyWrittenDocumentAndUsesCsvMime() {
        val source = File(root, "engine.CSV").apply { writeText("rpm;map\n1000;42\n") }

        val destination = helper.saveLog(source, LogExportDestination.CustomTree(treeUri.toString()))

        assertEquals("TestLogs", destination)
        assertEquals("text/csv", provider.createdMimeType)
        assertEquals("engine.CSV", provider.createdDisplayName)
        assertEquals("rpm;map\n1000;42\n", provider.outputFile.readText())
        assertEquals(0, provider.deleteCalls)
    }

    @Test
    fun shareIntentUsesExportMimeForSelectedFile() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val realHelper = FileHelper(base)
        val csv = File.createTempFile("share-mime-", ".CSV", realHelper.logsDir).apply { writeText("csv-data") }
        val s3l = File.createTempFile("share-mime-", ".S3L", realHelper.logsDir).apply { writeBytes(byteArrayOf(1, 2, 3)) }
        var logger: SecuLogger? = null
        var prefsFixture: IsolatedUserPrefs? = null
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val fixture = IsolatedUserPrefs(base, "share-mime-${System.nanoTime()}")
                prefsFixture = fixture
                val prefs = fixture.prefs
                val testLogger = SecuLogger(prefs, realHelper)
                logger = testLogger
                val viewModel = SecuLogsViewModel(realHelper, prefs, SavedStateHandle(), testLogger, org.secu3.android.ui.messages.UiMessageQueue())
                val viewModelStore = ViewModelStore().apply { put("logs-share-test", viewModel) }
                try {
                    val csvIntent = viewModel.getShareIntent(csv)
                    val csvUri = csvIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    assertEquals(Intent.ACTION_SEND, csvIntent.action)
                    assertEquals("text/csv", csvIntent.type)
                    assertEquals(csv.name, csvUri?.lastPathSegment)
                    assertEquals("csv-data", base.contentResolver.openInputStream(csvUri!!)?.use { it.bufferedReader().readText() })
                    assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, csvIntent.flags)

                    val s3lIntent = viewModel.getShareIntent(s3l)
                    val s3lUri = s3lIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    assertEquals("application/octet-stream", s3lIntent.type)
                    assertEquals(s3l.name, s3lUri?.lastPathSegment)
                    assertEquals(byteArrayOf(1, 2, 3).toList(), base.contentResolver.openInputStream(s3lUri!!)?.use { it.readBytes().toList() })
                    assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, s3lIntent.flags)
                } finally {
                    viewModelStore.clear()
                }
            }
        } finally {
            runBlocking {
                logger?.shutdownForTests()
                prefsFixture?.close()
            }
            csv.delete()
            s3l.delete()
        }
    }

    @Test
    fun outputOpenFailureDeletesCreatedDocument() {
        val source = File(root, "engine.s3l").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        provider.failOpen = true

        val error = runCatching { helper.saveLog(source, LogExportDestination.CustomTree(treeUri.toString())) }.exceptionOrNull()

        assertTrue(error is FileNotFoundException)
        assertEquals("application/octet-stream", provider.createdMimeType)
        assertEquals(1, provider.deleteCalls)
        assertEquals(provider.createdDocumentUri, provider.deletedDocumentUri)
        assertFalse(provider.outputFile.exists())
    }

    @Test
    fun sourceReadFailureDeletesCreatedDocument() {
        val source = File(root, "missing.csv")

        val error = runCatching { helper.saveLog(source, LogExportDestination.CustomTree(treeUri.toString())) }.exceptionOrNull()

        assertTrue(error is FileNotFoundException)
        assertTrue(error?.message.orEmpty().contains("missing.csv"))
        assertEquals(1, provider.deleteCalls)
        assertEquals(provider.createdDocumentUri, provider.deletedDocumentUri)
        assertFalse(provider.outputFile.exists())
    }

    @Test
    fun cleanupFailureIsSuppressedWithoutReplacingOriginalWriteFailure() {
        val source = File(root, "engine.csv").apply { writeText("data") }
        provider.failOpen = true
        provider.failDelete = true

        val error = runCatching { helper.saveLog(source, LogExportDestination.CustomTree(treeUri.toString())) }.exceptionOrNull()

        assertSame(provider.openFailure, error)
        assertEquals(provider.createdDocumentUri, provider.deletedDocumentUri)
        assertEquals(1, error?.suppressed?.size)
        assertEquals("cleanup failed", error?.suppressed?.single()?.message)
    }

    private class FakeDocumentsProvider(val outputFile: File) : ContentProvider() {
        var failOpen = false
        var failDelete = false
        var deleteCalls = 0
        var createdMimeType: String? = null
        var createdDisplayName: String? = null
        var createdDocumentUri: Uri? = null
        var deletedDocumentUri: Uri? = null
        val openFailure = FileNotFoundException("output open failed")

        override fun onCreate() = true

        override fun call(authority: String, method: String, arg: String?, extras: Bundle?): Bundle? = when (method) {
            METHOD_CREATE_DOCUMENT -> {
                createdMimeType = extras?.getString(DocumentsContract.Document.COLUMN_MIME_TYPE)
                createdDisplayName = extras?.getString(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                createdDocumentUri = Uri.parse("content://$AUTHORITY/document/new")
                Bundle().apply { putParcelable(EXTRA_URI, createdDocumentUri) }
            }
            METHOD_DELETE_DOCUMENT -> {
                deleteCalls++
                deletedDocumentUri = extras?.getParcelable(EXTRA_URI)
                if (failDelete) throw IllegalStateException("cleanup failed")
                outputFile.delete()
                Bundle()
            }
            else -> error("Unexpected documents operation: $method")
        }

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            if (failOpen) throw openFailure
            outputFile.parentFile?.mkdirs()
            return ParcelFileDescriptor.open(
                outputFile,
                ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_WRITE_ONLY,
            )
        }

        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor =
            MatrixCursor(projection ?: emptyArray())

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    }

    private companion object {
        const val AUTHORITY = "org.secu3.android.test.documents"
        const val METHOD_CREATE_DOCUMENT = "android:createDocument"
        const val METHOD_DELETE_DOCUMENT = "android:deleteDocument"
        const val EXTRA_URI = "uri"
    }
}
