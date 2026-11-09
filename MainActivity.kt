package com.example.discipline30

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

private const val PREFS = "discipline30_data"
private const val CHANNEL = "discipline_reminders"
private val GOLD = Color.rgb(226, 178, 67)
private val BG = Color.rgb(19, 21, 26)
private val PANEL = Color.rgb(31, 35, 44)
private val MUTED = Color.rgb(145, 153, 169)

data class Habit(var name: String, var checks: MutableSet<Int> = mutableSetOf(), var reminder: String = "")

class MainActivity : ComponentActivity() {
    private lateinit var root: LinearLayout
    private var habits = mutableListOf<Habit>()
    private var notes = ""
    private val days = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadData(); createChannel()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 91)
        buildUi()
    }

    private fun defaultHabits() = mutableListOf(
        Habit("Fajr at masjid"), Habit("Cold shower"), Habit("No social media"), Habit("Daily Quran"),
        Habit("Daily gym"), Habit("Work & uni"), Habit("Clean food"), Habit("No party"),
        Habit("Family time"), Habit("Deen / dhikr"), Habit("7.5 hrs sleep")
    )

    private fun loadData() {
        val p = getSharedPreferences(PREFS, MODE_PRIVATE)
        notes = p.getString("notes", "") ?: ""
        val raw = p.getString("habits", null)
        if (raw == null) { habits = defaultHabits(); return }
        try {
            val arr = JSONArray(raw); habits.clear()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i); val set = mutableSetOf<Int>(); val c = o.optJSONArray("checks") ?: JSONArray()
                for (j in 0 until c.length()) set.add(c.getInt(j))
                habits.add(Habit(o.optString("name"), set, o.optString("reminder")))
            }
        } catch (_: Exception) { habits = defaultHabits() }
    }

    private fun saveData() {
        val arr = JSONArray()
        habits.forEach { h ->
            val o = JSONObject(); o.put("name", h.name); o.put("reminder", h.reminder)
            val c = JSONArray(); h.checks.sorted().forEach { c.put(it) }; o.put("checks", c); arr.put(o)
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("habits", arr.toString()).putString("notes", notes).apply()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG); setPadding(12, 16, 12, 12) }
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        val title = TextView(this).apply { text = "30-DAY DISCIPLINE ROUTINE"; setTextColor(Color.WHITE); textSize = 24f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, 8, 0, 2) }
        root.addView(title)
        root.addView(TextView(this).apply { text = "Alhamdulillah  •  tick each habit daily  •  consistency over perfection"; setTextColor(GOLD); textSize = 13f; gravity = Gravity.CENTER; setPadding(0, 0, 0, 18) })

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, 4, 0, 10) }
        actions.addView(actionButton("＋ Add habit") { addHabit() })
        actions.addView(actionButton("✎ Edit habits") { editHabits() })
        actions.addView(actionButton("⏰ Reminders") { chooseReminderHabit() })
        root.addView(actions)

        val progress = TextView(this).apply { tag = "progress"; setTextColor(GOLD); textSize = 14f; gravity = Gravity.CENTER; setPadding(0, 5, 0, 12) }
        root.addView(progress)
        val horizontal = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = true }
        val table = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        horizontal.addView(table); root.addView(horizontal)

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(PANEL) }
        header.addView(cell("HABIT", 150, true, Gravity.CENTER_VERTICAL))
        for (d in 1..days) header.addView(cell(d.toString(), 38, true, Gravity.CENTER))
        table.addView(header)

        habits.forEachIndexed { index, h ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(if (index % 2 == 0) PANEL else BG) }
            val name = TextView(this).apply {
                text = h.name; setTextColor(Color.rgb(235, 236, 240)); textSize = 14f; gravity = Gravity.CENTER_VERTICAL
                setPadding(10, 8, 6, 8); minHeight = 44
                setOnClickListener { editHabit(index) }; setOnLongClickListener { reminderDialog(index); true }
            }
            row.addView(name, LinearLayout.LayoutParams(150, LinearLayout.LayoutParams.WRAP_CONTENT))
            for (day in 1..days) {
                val cb = CheckBox(this).apply {
                    buttonTintList = android.content.res.ColorStateList.valueOf(if (h.checks.contains(day)) GOLD else MUTED)
                    isChecked = h.checks.contains(day); gravity = Gravity.CENTER; setPadding(0, 0, 0, 0)
                    setOnCheckedChangeListener { _, checked -> if (checked) h.checks.add(day) else h.checks.remove(day); saveData(); updateProgress() }
                }
                val wrap = FrameLayout(this).apply { addView(cb, FrameLayout.LayoutParams(38, 44, Gravity.CENTER)) }
                row.addView(wrap, LinearLayout.LayoutParams(38, 44))
            }
            table.addView(row)
        }

        val week = TextView(this).apply { text = "WEEKLY CHECK-IN     W1 □     W2 □     W3 □     W4 □     FINAL □"; setTextColor(Color.LTGRAY); textSize = 13f; setPadding(8, 18, 8, 10) }
        root.addView(week)
        root.addView(TextView(this).apply { text = "NOTES"; setTextColor(GOLD); textSize = 13f; setPadding(8, 6, 8, 4) })
        val note = EditText(this).apply { setTextColor(Color.WHITE); setHintTextColor(MUTED); hint = "Write reflections, wins, and lessons..."; setText(notes); minLines = 3; gravity = Gravity.TOP; setBackgroundColor(PANEL); setPadding(10, 8, 10, 8); textSize = 14f }
        note.addTextChangedListener(SimpleWatcher { notes = it; saveData() }); root.addView(note)
        root.addView(TextView(this).apply { text = "Fajr  •  Quran  •  Strength  •  Family  •  Deen"; setTextColor(GOLD); gravity = Gravity.CENTER; setPadding(0, 16, 0, 8) })
        updateProgress()
    }

    private fun updateProgress() {
        val total = habits.size * days; val done = habits.sumOf { it.checks.size }; val pct = if (total == 0) 0 else done * 100 / total
        root.findViewWithTag<TextView>("progress")?.text = "OVERALL PROGRESS   $pct%   •   $done / $total check-ins"
    }

    private fun cell(text: String, width: Int, gold: Boolean, gravity: Int) = TextView(this).apply {
        this.text = text; setTextColor(if (gold) GOLD else Color.WHITE); textSize = 12f; typeface = if (gold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        this.gravity = gravity; setPadding(2, 10, 2, 10); layoutParams = LinearLayout.LayoutParams(width, 42)
    }

    private fun actionButton(label: String, click: () -> Unit) = Button(this).apply {
        text = label; textSize = 10f; setTextColor(GOLD); setBackgroundColor(PANEL); setOnClickListener { click() }
        layoutParams = LinearLayout.LayoutParams(0, 44, 1f).apply { setMargins(3, 0, 3, 0) }
    }

    private fun addHabit() {
        val input = EditText(this).apply { hint = "Habit name"; setTextColor(Color.WHITE) }
        AlertDialog.Builder(this).setTitle("Add a habit").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Add") { _, _ ->
            val name = input.text.toString().trim(); if (name.isNotEmpty()) { habits.add(Habit(name)); saveData(); buildUi() }
        }.show()
    }

    private fun editHabit(i: Int) {
        val input = EditText(this).apply { setText(habits[i].name); setTextColor(Color.WHITE) }
        AlertDialog.Builder(this).setTitle("Edit habit").setView(input).setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ -> cancelReminder(i); habits.removeAt(i); saveData(); buildUi() }
            .setPositiveButton("Save") { _, _ -> habits[i].name = input.text.toString().trim().ifEmpty { habits[i].name }; saveData(); buildUi() }.show()
    }

    private fun editHabits() {
        if (habits.isEmpty()) { addHabit(); return }
        val names = habits.map { it.name }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Choose a habit to edit or delete").setItems(names) { _, which -> editHabit(which) }.setNegativeButton("Close", null).show()
    }

    private fun chooseReminderHabit() {
        if (habits.isEmpty()) return
        AlertDialog.Builder(this).setTitle("Set a daily reminder").setItems(habits.map { it.name }.toTypedArray()) { _, which -> reminderDialog(which) }.show()
    }

    private fun reminderDialog(i: Int) {
        val h = habits[i]; val cal = Calendar.getInstance(); val old = h.reminder.split(":")
        val hour = old.getOrNull(0)?.toIntOrNull() ?: 20; val minute = old.getOrNull(1)?.toIntOrNull() ?: 0
        val picker = TimePicker(this).apply { setIs24HourView(true); this.hour = hour; this.minute = minute }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 8, 20, 0) }
        box.addView(TextView(this).apply { text = "Daily reminder for: ${h.name}"; setTextColor(Color.WHITE); textSize = 16f })
        box.addView(picker)
        AlertDialog.Builder(this).setTitle("Reminder time").setView(box)
            .setNeutralButton(if (h.reminder.isEmpty()) "No reminder" else "Turn off") { _, _ -> cancelReminder(i); h.reminder = ""; saveData(); Toast.makeText(this, "Reminder turned off", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Cancel", null).setPositiveButton("Set reminder") { _, _ ->
                h.reminder = "%02d:%02d".format(picker.hour, picker.minute); saveData(); scheduleReminder(i)
                Toast.makeText(this, "Daily reminder set for ${h.reminder}", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun scheduleReminder(i: Int) {
        val h = habits[i]; if (h.reminder.isEmpty()) return
        val parts = h.reminder.split(":"); val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, parts[0].toInt()); set(Calendar.MINUTE, parts[1].toInt()); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        val pi = reminderPendingIntent(i, h.name)
        if (Build.VERSION.SDK_INT >= 23) {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) { startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)); return }
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } else am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }

    private fun cancelReminder(i: Int) { val am = getSystemService(ALARM_SERVICE) as AlarmManager; am.cancel(reminderPendingIntent(i, habits.getOrNull(i)?.name ?: "Habit")) }
    private fun reminderPendingIntent(i: Int, name: String): PendingIntent {
        val intent = Intent(this, ReminderReceiver::class.java).putExtra("name", name).putExtra("id", i)
        return PendingIntent.getBroadcast(this, i + 100, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) { val channel = NotificationChannel(CHANNEL, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT); (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel) }
    }
}

class ReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Your habit"; val id = intent.getIntExtra("id", 0)
        val notification = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Discipline 30").setContentText("Time for: $name").setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true).build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) manager.notify(id + 1, notification)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); val arr = JSONArray(p.getString("habits", "[]"))
        if (id in 0 until arr.length()) {
            val o = arr.getJSONObject(id); val parts = o.optString("reminder").split(":")
            if (parts.size == 2) {
                val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, parts[0].toInt()); set(Calendar.MINUTE, parts[1].toInt()); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.DAY_OF_YEAR, 1) }
                val next = Intent(context, ReminderReceiver::class.java).putExtra("name", name).putExtra("id", id)
                val pi = PendingIntent.getBroadcast(context, id + 100, next, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi) else am.setExact(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
            }
        }
    }
}

class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); val arr = JSONArray(p.getString("habits", "[]")); val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i); val name = o.optString("name"); val time = o.optString("reminder"); val parts = time.split(":")
            if (parts.size != 2) continue
            val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, parts[0].toInt()); set(Calendar.MINUTE, parts[1].toInt()); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
            val pi = PendingIntent.getBroadcast(context, i + 100, Intent(context, ReminderReceiver::class.java).putExtra("name", name).putExtra("id", i), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi) else am.setExact(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
        }
    }
}

class SimpleWatcher(private val changed: (String) -> Unit) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed(s?.toString() ?: "") }
    override fun afterTextChanged(s: Editable?) {}
}
