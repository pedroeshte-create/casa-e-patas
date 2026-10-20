package pt.casaepatas.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LogicTest {

    /** Cria um instante local. Mês 1-12. */
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        Calendar.getInstance().apply { clear(); set(y, mo - 1, d, h, mi, 0) }.timeInMillis

    private fun task(
        id: Int = 1, assignee: Int = 1, repeat: Repeat = Repeat.NONE, done: String? = null, weekday: Int = 2,
    ) = Task(id, "t$id", Kind.HOME, assignee, false, null, repeat, weekday, done)

    // 2026-10-20 é uma terça-feira
    private val tue10h = at(2026, 10, 20, 10, 0)

    @Test
    fun dailyReminderLaterToday() {
        val t = nextTrigger("19:00", Repeat.DAILY, 0, tue10h)
        assertEquals(at(2026, 10, 20, 19, 0), t)
    }

    @Test
    fun dailyReminderAlreadyPassedGoesToTomorrow() {
        val t = nextTrigger("08:00", Repeat.DAILY, 0, tue10h)
        assertEquals(at(2026, 10, 21, 8, 0), t)
    }

    @Test
    fun dailyReminderIsStrictlyAfterNow() {
        val t = nextTrigger("10:00", Repeat.DAILY, 0, tue10h)
        assertEquals(at(2026, 10, 21, 10, 0), t)
    }

    @Test
    fun weeklyReminderFindsNextFriday() {
        val t = nextTrigger("09:30", Repeat.WEEKLY, Calendar.FRIDAY, tue10h)
        assertEquals(at(2026, 10, 23, 9, 30), t)
    }

    @Test
    fun weeklyReminderSameWeekdayAfterTimeGoesNextWeek() {
        val t = nextTrigger("08:00", Repeat.WEEKLY, Calendar.TUESDAY, tue10h)
        assertEquals(at(2026, 10, 27, 8, 0), t)
    }

    @Test
    fun oneOffReminderInThePastIsDropped() {
        assertNull(nextTrigger("08:00", Repeat.NONE, 0, tue10h))
        assertNotNull(nextTrigger("19:00", Repeat.NONE, 0, tue10h))
    }

    @Test
    fun badTimeIsIgnored() {
        assertNull(nextTrigger("xx", Repeat.DAILY, 0, tue10h))
    }

    @Test
    fun dailyTaskIsDoneOnlyToday() {
        assertTrue(task(repeat = Repeat.DAILY, done = "2026-10-20").isDone(tue10h))
        assertFalse(task(repeat = Repeat.DAILY, done = "2026-10-19").isDone(tue10h))
        assertFalse(task(repeat = Repeat.DAILY, done = null).isDone(tue10h))
    }

    @Test
    fun weeklyTaskIsDoneForTheWholeWeek() {
        // segunda 19 e terça 20 estão na mesma semana; domingo 18 é da semana anterior
        assertTrue(task(repeat = Repeat.WEEKLY, done = "2026-10-19").isDone(tue10h))
        assertFalse(task(repeat = Repeat.WEEKLY, done = "2026-10-18").isDone(tue10h))
    }

    @Test
    fun oneOffTaskStaysDone() {
        assertTrue(task(repeat = Repeat.NONE, done = "2020-01-01").isDone(tue10h))
    }

    @Test
    fun weeklyTaskIsDueOnlyOnItsWeekday() {
        assertTrue(task(repeat = Repeat.WEEKLY, weekday = Calendar.TUESDAY).dueToday(tue10h))
        assertFalse(task(repeat = Repeat.WEEKLY, weekday = Calendar.FRIDAY).dueToday(tue10h))
    }

    @Test
    fun autoAssignPicksPersonWithFewestPendingTasks() {
        val people = listOf(Person(1, "Ana", 0), Person(2, "Rui", 0), Person(3, "Marta", 0))
        val tasks = listOf(
            task(1, assignee = 1), task(2, assignee = 1), task(3, assignee = 1),
            task(4, assignee = 2),
            task(5, assignee = 3), task(6, assignee = 3),
        )
        assertEquals("Rui", suggestAssignee(people, tasks, tue10h).name)
    }

    @Test
    fun autoAssignIgnoresDoneTasksAndBreaksTiesByOrder() {
        val people = listOf(Person(1, "Ana", 0), Person(2, "Rui", 0))
        val tasks = listOf(
            task(1, assignee = 1, done = "2020-01-01"), task(2, assignee = 1, done = "2020-01-01"),
            task(3, assignee = 2),
        )
        assertEquals("Ana", suggestAssignee(people, tasks, tue10h).name)
        assertEquals("Ana", suggestAssignee(people, emptyList(), tue10h).name)
    }

    @Test
    fun stateSurvivesJsonRoundTrip() {
        val s = AppState(
            people = listOf(Person(1, "Ana", 0xFF1E6B58), Person(5, "Rui", 0xFF234A7E)),
            meId = 5,
            pets = listOf(
                Pet(2, "Luna", PetType.CAT, "", "/x/luna.jpg", true, "2026-10-20"),
                Pet(3, "Coelho Bino", PetType.OTHER, "coelho", null, false, null),
            ),
            tasks = listOf(
                Task(4, "Aspirar", Kind.HOME, 5, true, "18:30", Repeat.WEEKLY, Calendar.THURSDAY, null),
                Task(6, "Regar", Kind.HOME, 1, false, null, Repeat.DAILY, 2, "2026-10-20"),
            ),
            boxes = listOf(LitterBox(7, "Caixa 1", "2026-10-20"), LitterBox(8, "Caixa 2", null)),
            settings = AppSettings(2, 1, true, "19:30", true, "21:00"),
            nextId = 9, rotateIdx = 3,
        )
        assertEquals(s, StateJson.decode(StateJson.encode(s)))
    }

    @Test
    fun brokenJsonFallsBackToDefaultState() {
        assertEquals(defaultState(), StateJson.decode("{ isto não é json"))
        assertEquals(defaultState(), StateJson.decode(null))
    }

    @Test
    fun petsAreGroupedByType() {
        val cat1 = Pet(1, "Luna", PetType.CAT)
        val cat2 = Pet(2, "Nico", PetType.CAT)
        val rabbit = Pet(3, "Bino", PetType.OTHER, " Coelho ")
        assertEquals(cat1.groupKey(), cat2.groupKey())
        assertFalse(cat1.groupKey() == rabbit.groupKey())
        assertEquals("Coelho", rabbit.typeLabel())
    }
}
