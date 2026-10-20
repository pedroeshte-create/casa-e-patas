package pt.casaepatas.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifications {
    const val CHANNEL = "lembretes"
    const val TEST_ID = 9001
    const val FEED_ID = 9002
    const val LITTER_ID = 9003

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Lembretes", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Tarefas, comida e caixas de areia"
                }
            )
        }
    }

    fun notificationsEnabled(ctx: Context): Boolean = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun canExact(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun show(ctx: Context, id: Int, title: String, text: String) {
        ensureChannel(ctx)
        if (!notificationsEnabled(ctx)) return
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (e: SecurityException) {
            // sem permissão de notificações: nada a fazer
        }
    }

    // ---------- agendamento ----------

    private fun pendingFor(ctx: Context, key: String): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, key.hashCode(),
            Intent(ctx, ReminderReceiver::class.java).putExtra("key", key),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun setAlarm(ctx: Context, at: Long, key: String) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingFor(ctx, key)
        try {
            if (canExact(ctx)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    /** Agenda (ou cancela) todos os lembretes de acordo com o estado atual. */
    fun scheduleAll(ctx: Context, st: AppState, now: Long = System.currentTimeMillis()) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val prefs = ctx.getSharedPreferences("alarms", Context.MODE_PRIVATE)
        val wanted = mutableMapOf<String, Long>()

        for (t in st.tasks) {
            val time = t.time ?: continue
            if (t.repeat == Repeat.NONE && t.isDone(now)) continue
            nextTrigger(time, t.repeat, t.weekday, now + 1000)?.let { wanted["task:${t.id}"] = it }
        }
        if (st.settings.feedReminder && st.pets.isNotEmpty()) {
            nextTrigger(st.settings.feedTime, Repeat.DAILY, 0, now + 1000)?.let { wanted["feed"] = it }
        }
        if (st.settings.litterReminder && st.boxes.isNotEmpty()) {
            nextTrigger(st.settings.litterTime, Repeat.DAILY, 0, now + 1000)?.let { wanted["litter"] = it }
        }

        val old = prefs.getStringSet("keys", emptySet()) ?: emptySet()
        for (k in old - wanted.keys) am.cancel(pendingFor(ctx, k))
        for ((k, at) in wanted) setAlarm(ctx, at, k)
        prefs.edit().putStringSet("keys", wanted.keys.toSet()).apply()
    }

    fun scheduledCount(ctx: Context): Int =
        ctx.getSharedPreferences("alarms", Context.MODE_PRIVATE).getStringSet("keys", emptySet())?.size ?: 0

    /** Agenda um lembrete de teste para daqui a [seconds] segundos pelo mesmo caminho dos lembretes reais. */
    fun scheduleTest(ctx: Context, seconds: Int) {
        setAlarm(ctx, System.currentTimeMillis() + seconds * 1000L, "test")
    }

    // ---------- disparo ----------

    /** Chamado pelo ReminderReceiver quando um alarme toca. */
    fun fire(ctx: Context, st: AppState, key: String) {
        val today = todayStr()
        when {
            key == "test" -> show(
                ctx, TEST_ID, "Teste de lembrete",
                "Os lembretes estão a funcionar neste telemóvel.",
            )

            key == "feed" -> {
                val hungry = st.pets.filter { it.fedDate != today }
                if (hungry.isNotEmpty()) {
                    show(ctx, FEED_ID, "Hora de dar de comer", hungry.joinToString(", ") { it.name })
                }
            }

            key == "litter" -> {
                val dirty = st.boxes.filter { it.cleanedDate != today }
                if (dirty.isNotEmpty()) {
                    show(ctx, LITTER_ID, "Limpar as areias", dirty.joinToString(", ") { it.name })
                }
            }

            key.startsWith("task:") -> {
                val id = key.removePrefix("task:").toIntOrNull() ?: return
                val t = st.tasks.firstOrNull { it.id == id } ?: return
                if (!t.isDone()) {
                    val who = st.people.firstOrNull { it.id == t.assigneeId }?.name ?: ""
                    show(ctx, 10000 + id, t.title, if (who.isEmpty()) "Tarefa por fazer" else "Responsável: $who")
                }
            }
        }
    }
}
