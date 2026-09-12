package com.sharn.handpan.data.local

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.sharn.handpan.model.DifficultyLevel
import com.sharn.handpan.model.HandpanPattern
import com.sharn.handpan.model.NoteEvent
import com.sharn.handpan.model.PatternCategory
import com.sharn.handpan.model.Subdivision
import com.sharn.handpan.model.TimeSignature
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object PatternShareHelper {

    private const val CURRENT_SCHEMA_VERSION = 1
    private const val CURRENT_APP_VERSION = "1.0.0"

    /**
     * Serializes a HandpanPattern into a clean versioned JSON string.
     */
    fun patternToJson(pattern: HandpanPattern): String {
        val root = JSONObject()
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("appVersion", CURRENT_APP_VERSION)
        root.put("format", "HandpanPattern_v1")
        root.put("id", pattern.id)
        root.put("title", pattern.title)
        root.put("description", pattern.description)
        root.put("bpm", pattern.bpm)
        root.put("timeSignature", "${pattern.timeSignature.numerator}/${pattern.timeSignature.denominator}")
        root.put("bars", pattern.bars)
        root.put("difficulty", pattern.difficulty.name)
        root.put("category", pattern.category.name)
        root.put("subdivision", pattern.recommendedSubdivision.name)

        val eventsArray = JSONArray()
        for (event in pattern.events) {
            val ev = JSONObject()
            ev.put("note", event.noteNumber)
            ev.put("beat", event.beatPosition)
            ev.put("duration", event.duration)
            ev.put("velocity", event.velocity.toDouble())
            ev.put("accent", event.accent)
            ev.put("rest", event.isRest)
            if (event.hand != null) ev.put("hand", event.hand)
            eventsArray.put(ev)
        }
        root.put("events", eventsArray)

        return root.toString(2)
    }

    /**
     * Parses and strictly validates JSON string back into a HandpanPattern.
     * Rejects corrupted or invalid data with clear, user-friendly error messages.
     */
    fun jsonToPattern(jsonStr: String): Result<HandpanPattern> {
        val trimmed = jsonStr.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("متن وارد شده خالی است."))
        }

        return try {
            val root = JSONObject(trimmed)

            // Schema / Format check
            val format = root.optString("format", "")
            val schemaVersion = root.optInt("schemaVersion", 1)
            if (schemaVersion > CURRENT_SCHEMA_VERSION) {
                return Result.failure(IllegalArgumentException("نسخه فرمت الگو ($schemaVersion) از نسخه برنامه جدیدتر است."))
            }

            // Title validation
            if (!root.has("title")) {
                return Result.failure(IllegalArgumentException("فیلد عنوان الگو (title) در داده‌ها یافت نشد."))
            }
            val title = root.getString("title").trim()
            if (title.isEmpty()) {
                return Result.failure(IllegalArgumentException("عنوان الگو نمی‌تواند خالی باشد."))
            }
            val description = root.optString("description", "وارد شده از فایل اشتراکی")

            // BPM validation
            if (!root.has("bpm")) {
                return Result.failure(IllegalArgumentException("فیلد سرعت (bpm) در الگو موجود نیست."))
            }
            val bpm = root.getInt("bpm")
            if (bpm !in 30..240) {
                return Result.failure(IllegalArgumentException("سرعت (BPM) باید بین ۳۰ تا ۲۴۰ باشد (مقدار دریافتی: $bpm)."))
            }

            // Bars validation
            if (!root.has("bars")) {
                return Result.failure(IllegalArgumentException("فیلد تعداد میزان (bars) در الگو موجود نیست."))
            }
            val bars = root.getInt("bars")
            if (bars !in 1..16) {
                return Result.failure(IllegalArgumentException("تعداد میزان‌ها باید بین ۱ تا ۱۶ باشد (مقدار دریافتی: $bars)."))
            }

            // Time signature validation
            val tsStr = root.optString("timeSignature", "4/4")
            val tsParts = tsStr.split("/")
            val num = tsParts.getOrNull(0)?.toIntOrNull() ?: 4
            val den = tsParts.getOrNull(1)?.toIntOrNull() ?: 4
            if (num !in 1..16 || den !in listOf(2, 4, 8, 16)) {
                return Result.failure(IllegalArgumentException("کسر میزان نامعتبر است: $tsStr"))
            }
            val timeSignature = TimeSignature(num, den)

            // Difficulty, Category, Subdivision
            val difficultyStr = root.optString("difficulty", "BEGINNER")
            val difficulty = try { DifficultyLevel.valueOf(difficultyStr) } catch (_: Exception) { DifficultyLevel.BEGINNER }

            val categoryStr = root.optString("category", "CUSTOM")
            val category = try { PatternCategory.valueOf(categoryStr) } catch (_: Exception) { PatternCategory.CUSTOM }

            val subStr = root.optString("subdivision", "EIGHTH")
            val subdivision = try { Subdivision.valueOf(subStr) } catch (_: Exception) { Subdivision.EIGHTH }

            // Events validation
            if (!root.has("events")) {
                return Result.failure(IllegalArgumentException("آرایه رویدادها و نت‌های موسیقی (events) یافت نشد."))
            }
            val eventsArray = root.getJSONArray("events")
            if (eventsArray.length() == 0) {
                return Result.failure(IllegalArgumentException("الگو هیچ نتی برای اجرا ندارد."))
            }

            val maxTotalBeats = (bars * timeSignature.beatsPerBar).toDouble()
            val events = mutableListOf<NoteEvent>()

            for (i in 0 until eventsArray.length()) {
                val ev = eventsArray.getJSONObject(i)
                val note = ev.optInt("note", 0)
                val isRest = ev.optBoolean("rest", false)
                val beat = ev.optDouble("beat", -1.0)
                val duration = ev.optDouble("duration", 1.0)
                val velocity = ev.optDouble("velocity", 0.85).toFloat()
                val accent = ev.optBoolean("accent", false)
                val hand = if (ev.has("hand")) ev.getString("hand") else null

                if (!isRest && note !in 0..9) {
                    return Result.failure(IllegalArgumentException("نت نامعتبر در ضرب $i: نت $note باید بین ۰ تا ۹ یا سکوت باشد."))
                }
                if (beat < 0.0 || beat >= maxTotalBeats + 0.01) {
                    return Result.failure(IllegalArgumentException("موقعیت زمانی نت نامعتبر است: beat=$beat خارج از محدوده مجاز (۰ تا $maxTotalBeats) است."))
                }
                if (duration <= 0.0) {
                    return Result.failure(IllegalArgumentException("کشش زمانی نت باید مثبت باشد: duration=$duration"))
                }
                if (velocity !in 0.0f..1.0f) {
                    return Result.failure(IllegalArgumentException("شدت ضربه نامعتبر است: velocity=$velocity"))
                }

                events.add(
                    NoteEvent(
                        noteNumber = note,
                        beatPosition = beat,
                        duration = duration,
                        velocity = velocity,
                        accent = accent,
                        isRest = isRest,
                        hand = hand
                    )
                )
            }

            val pattern = HandpanPattern(
                id = "imported_" + UUID.randomUUID().toString().take(8),
                title = title,
                description = description,
                bpm = bpm,
                timeSignature = timeSignature,
                bars = bars,
                events = events,
                difficulty = difficulty,
                category = category,
                isCustom = true,
                recommendedSubdivision = subdivision
            )
            Result.success(pattern)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("قالب‌بندی فایل JSON نامعتبر یا مخدوش است: ${e.message}", e))
        }
    }

    /**
     * Copies pattern JSON to clipboard.
     */
    fun copyToClipboard(context: Context, pattern: HandpanPattern) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val json = patternToJson(pattern)
        val clip = ClipData.newPlainText("Handpan Pattern ${pattern.title}", json)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "الگوی «${pattern.title}» در کلیپ‌بورد کپی شد", Toast.LENGTH_SHORT).show()
    }

    /**
     * Shares pattern JSON via system share sheet (SMS, Telegram, WhatsApp, Email, etc.).
     */
    fun sharePattern(context: Context, pattern: HandpanPattern) {
        val json = patternToJson(pattern)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "الگوی هنگ‌درام: ${pattern.title}")
            putExtra(
                Intent.EXTRA_TEXT,
                "🎶 الگوی آموزشی هنگ‌درام: ${pattern.title}\n" +
                "⏱️ تمپو: ${pattern.bpm} BPM | میزان: ${pattern.timeSignature.numerator}/${pattern.timeSignature.denominator}\n\n" +
                json
            )
        }
        val chooser = Intent.createChooser(shareIntent, "اشتراک‌گذاری الگوی «${pattern.title}»")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Gets clipboard text if it contains pattern JSON.
     */
    fun getClipboardText(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()
            return text
        }
        return null
    }
}
