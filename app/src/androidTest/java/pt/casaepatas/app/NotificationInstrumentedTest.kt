package pt.casaepatas.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testa as notificações num Android real (emulador): canal, publicação direta, alarme do sistema,
 * o receiver dos lembretes automáticos e a regra "não avisar se já está feito".
 */
@RunWith(AndroidJUnit4::class)
class NotificationInstrumentedTest {

    @get:Rule
    val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val ctx: Context = instrumentation.targetContext
    private val nm: NotificationManager = ctx.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        ctx.getSharedPreferences("casa_e_patas", Context.MODE_PRIVATE).edit().clear().commit()
        ctx.getSharedPreferences("alarms", Context.MODE_PRIVATE).edit().clear().commit()
        Notifications.ensureChannel(ctx)
        nm.cancelAll()
        // permite alarmes exatos para o teste (em produção o utilizador concede em Definições)
        instrumentation.uiAutomation
            .executeShellCommand("appops set ${ctx.packageName} SCHEDULE_EXACT_ALARM allow").close()
    }

    private fun active(id: Int) = nm.activeNotifications.any { it.id == id }

    private fun waitFor(timeoutMs: Long = 20_000, cond: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (cond()) return true
            Thread.sleep(250)
        }
        return cond()
    }

    @Test
    fun channelIsCreatedWithHighImportance() {
        val ch = nm.getNotificationChannel(Notifications.CHANNEL)
        assertTrue(ch != null)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, ch.importance)
    }

    @Test
    fun notificationsAreEnabledForTheApp() {
        assertTrue(Notifications.notificationsEnabled(ctx))
    }

    @Test
    fun directNotificationIsPosted() {
        Notifications.show(ctx, Notifications.TEST_ID, "Teste", "Corpo")
        assertTrue(waitFor { active(Notifications.TEST_ID) })
    }

    @Test
    fun scheduledAlarmFiresAndPostsNotification() {
        Notifications.scheduleTest(ctx, 3)
        assertTrue("o alarme de teste devia ter publicado a notificação", waitFor(60_000) { active(Notifications.TEST_ID) })
    }

    @Test
    fun feedReminderPostsWhenAPetIsHungry() {
        AppStore(ctx).addPet(Pet(0, "Luna", PetType.CAT))
        ctx.sendBroadcast(Intent(ctx, ReminderReceiver::class.java).putExtra("key", "feed"))
        assertTrue(waitFor { active(Notifications.FEED_ID) })
    }

    @Test
    fun feedReminderStaysSilentWhenEveryoneAlreadyAte() {
        val store = AppStore(ctx)
        store.addPet(Pet(0, "Luna", PetType.CAT))
        store.setFed(store.state.pets.map { it.id }, true)
        ctx.sendBroadcast(Intent(ctx, ReminderReceiver::class.java).putExtra("key", "feed"))
        Thread.sleep(4_000)
        assertFalse(active(Notifications.FEED_ID))
    }

    @Test
    fun taskReminderPostsForPendingTaskAndNotForDoneTask() {
        val store = AppStore(ctx)
        store.addTask(Task(0, "Regar plantas", Kind.HOME, store.state.meId, false, "09:00", Repeat.DAILY, 2, null), false)
        val id = store.state.tasks.first().id
        ctx.sendBroadcast(Intent(ctx, ReminderReceiver::class.java).putExtra("key", "task:$id"))
        assertTrue(waitFor { active(10000 + id) })

        nm.cancelAll()
        store.toggleTask(id) // marca como feita hoje
        ctx.sendBroadcast(Intent(ctx, ReminderReceiver::class.java).putExtra("key", "task:$id"))
        Thread.sleep(4_000)
        assertFalse(active(10000 + id))
    }

    @Test
    fun addingATaskWithTimeSchedulesAnAlarm() {
        val store = AppStore(ctx)
        store.addTask(Task(0, "Passear", Kind.PET, store.state.meId, false, "23:59", Repeat.DAILY, 2, null), false)
        assertEquals(1, Notifications.scheduledCount(ctx))
        store.deleteTask(store.state.tasks.first().id)
        assertEquals(0, Notifications.scheduledCount(ctx))
    }
}
