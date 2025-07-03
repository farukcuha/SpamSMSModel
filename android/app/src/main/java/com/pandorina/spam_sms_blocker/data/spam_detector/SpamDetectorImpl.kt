package com.pandorina.spam_sms_blocker.data.spam_detector

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

@Singleton
class SpamDetectorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SpamDetector {

    private var interpreter: Interpreter? = null
    private var wordIndex: Map<String, Int> = emptyMap()
    private val maxSequenceLength = 51
    private val oovToken = "<OOV>"
    private val padToken = 0
    private val lock = ReentrantLock()

    @Volatile
    private var isInitialized = false

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext

        lock.withLock {
            if (isInitialized) return@withLock
            try {
                loadModel()
                loadTokenizer()
                isInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun isReady(): Boolean = isInitialized

    override fun detectSpam(message: String): SpamDetectionResult {
        if (!isInitialized) return SpamDetectionResult.NotInitialized

        return lock.withLock {
            try {
                val tokens = tokenizeText(message)
                val inputArray = Array(1) { FloatArray(maxSequenceLength) }
                tokens.forEachIndexed { i, token -> inputArray[0][i] = token.toFloat() }

                val outputArray = Array(1) { FloatArray(1) }
                interpreter?.run(inputArray, outputArray)
                    ?: return SpamDetectionResult.Error("Interpreter is null")

                val probability = outputArray[0][0].coerceIn(0.0f, 1.0f)
                SpamDetectionResult.Success(probability)
            } catch (e: Exception) {
                e.printStackTrace()
                SpamDetectionResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun loadModel() {
        val modelBuffer = loadModelFile()
        interpreter = Interpreter(modelBuffer)
    }

    private fun loadTokenizer() {
        val tokenizerJson = context.assets.open("tokenizer.json").bufferedReader().use { it.readText() }
        val tokenizer = JSONObject(tokenizerJson)
        val config = tokenizer.getJSONObject("config")
        val wordCountsStr = config.getString("word_counts")
        val wordCounts = JSONObject(wordCountsStr)

        val sortedWords = wordCounts.keys().asSequence()
            .map { it to wordCounts.getInt(it) }
            .sortedByDescending { it.second }

        val tempWordIndex = mutableMapOf<String, Int>()
        tempWordIndex[oovToken] = 1
        var index = 2
        for ((word, _) in sortedWords) {
            if (word != oovToken) tempWordIndex[word] = index++
        }

        wordIndex = tempWordIndex
    }

    private fun loadModelFile(): MappedByteBuffer {
        val afd = context.assets.openFd("spam_model.tflite")
        return FileInputStream(afd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY,
            afd.startOffset,
            afd.declaredLength
        )
    }

    private fun cleanText(text: String): String {
        return text.lowercase()
            .replace(Regex("http\\S+"), "")
            .replace(Regex("[^a-zçğıöşü0-9 ]"), "")
            .trim()
    }

    private fun tokenizeText(text: String): IntArray {
        val words = cleanText(text).split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return IntArray(maxSequenceLength) { i ->
            if (i < words.size) {
                wordIndex[words[i]] ?: wordIndex[oovToken] ?: 1
            } else {
                padToken
            }
        }
    }
}
