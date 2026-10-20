package pt.casaepatas.app

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class Kind { HOME, PET }
enum class Repeat { NONE, DAILY, WEEKLY }
enum class PetType { CAT, DOG, OTHER }
enum class AssignMode { AUTO, ROTATE, MANUAL }

data class Person(val id: Int, val name: String, val color: Long)

data class Pet(
    val id: Int,
    val name: String,
    val type: PetType,
    val otherType: String = "",
    val photoPath: String? = null,
    val feedGroup: Boolean = true,
    val fedDate: String? = null,
)

data class Task(
    val id: Int,
    val title: String,
    val kind: Kind,
    val assigneeId: Int,
    val auto: Boolean,
    val time: String?,
    val repeat: Repeat,
    /** Calendar.DAY_OF_WEEK (1 = domingo ... 7 = sábado); só conta quando repeat == WEEKLY. */
    val weekday: Int,
    val doneDate: String?,
)

data class LitterBox(val id: Int, val name: String, val cleanedDate: String?)

data class AppSettings(
    val primary: Int = 0,
    val accent: Int = 0,
    val feedReminder: Boolean = false,
    val feedTime: String = "19:00",
    val litterReminder: Boolean = false,
    val litterTime: String = "20:00",
)

data class AppState(
    val people: List<Person>,
    val meId: Int,
    val pets: List<Pet>,
    val tasks: List<Task>,
    val boxes: List<LitterBox>,
    val settings: AppSettings,
    val nextId: Int,
    val rotateIdx: Int,
)

val PersonColors = listOf(0xFF1E6B58, 0xFF234A7E, 0xFF7B3F66, 0xFFB4531A, 0xFF4A6B1E)

fun defaultState() = AppState(
    people = listOf(Person(1, "Eu", PersonColors[0])),
    meId = 1,
    pets = emptyList(),
    tasks = emptyList(),
    boxes = emptyList(),
    settings = AppSettings(),
    nextId = 2,
    rotateIdx = 0,
)

// ---------- datas ----------

private fun dayFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

fun todayStr(now: Long = System.currentTimeMillis()): String = dayFormat().format(Date(now))

fun weekKey(dateStr: String): String {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.minimalDaysInFirstWeek = 4
    cal.time = dayFormat().parse(dateStr) ?: Date()
    return "${cal.weekYear}-${cal.get(Calendar.WEEK_OF_YEAR)}"
}

// ---------- regras ----------

fun Task.isDone(now: Long = System.currentTimeMillis()): Boolean {
    val d = doneDate
    return when (repeat) {
        Repeat.NONE -> d != null
        Repeat.DAILY -> d == todayStr(now)
        Repeat.WEEKLY -> d != null && weekKey(d) == weekKey(todayStr(now))
    }
}

fun Task.dueToday(now: Long = System.currentTimeMillis()): Boolean = when (repeat) {
    Repeat.WEEKLY -> weekday == Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_WEEK)
    else -> true
}

/** Pessoa com menos tarefas por fazer; em caso de empate, a que foi criada primeiro. */
fun suggestAssignee(people: List<Person>, tasks: List<Task>, now: Long = System.currentTimeMillis()): Person =
    people.minWith(
        compareBy<Person>(
            { p -> tasks.count { it.assigneeId == p.id && !it.isDone(now) } },
            { p -> p.id },
        )
    )

/** Próximo instante (ms) a que o lembrete deve tocar, estritamente depois de [after]. */
fun nextTrigger(time: String, repeat: Repeat, weekday: Int, after: Long): Long? {
    val parts = time.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val cal = Calendar.getInstance()
    cal.timeInMillis = after
    cal.set(Calendar.HOUR_OF_DAY, h)
    cal.set(Calendar.MINUTE, m)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    when (repeat) {
        Repeat.NONE -> if (cal.timeInMillis <= after) return null
        Repeat.DAILY -> if (cal.timeInMillis <= after) cal.add(Calendar.DAY_OF_MONTH, 1)
        Repeat.WEEKLY -> {
            var guard = 0
            while ((cal.get(Calendar.DAY_OF_WEEK) != weekday || cal.timeInMillis <= after) && guard < 15) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
                guard++
            }
        }
    }
    return cal.timeInMillis
}

fun Pet.groupKey(): String = if (type == PetType.OTHER) "O:" + otherType.trim().lowercase() else type.name

fun Pet.typeLabel(): String = when (type) {
    PetType.CAT -> "Gato"
    PetType.DOG -> "Cão"
    PetType.OTHER -> otherType.ifBlank { "Outro" }
}

// ---------- persistência (JSON) ----------

object StateJson {
    private fun JSONObject.str(k: String): String? = if (isNull(k)) null else getString(k)

    fun encode(s: AppState): String {
        val o = JSONObject()
        o.put("nextId", s.nextId)
        o.put("rotateIdx", s.rotateIdx)
        o.put("meId", s.meId)
        o.put("people", JSONArray().also { a ->
            s.people.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("color", it.color)) }
        })
        o.put("pets", JSONArray().also { a ->
            s.pets.forEach {
                a.put(
                    JSONObject().put("id", it.id).put("name", it.name).put("type", it.type.name)
                        .put("otherType", it.otherType).put("photo", it.photoPath ?: JSONObject.NULL)
                        .put("feedGroup", it.feedGroup).put("fed", it.fedDate ?: JSONObject.NULL)
                )
            }
        })
        o.put("tasks", JSONArray().also { a ->
            s.tasks.forEach {
                a.put(
                    JSONObject().put("id", it.id).put("title", it.title).put("kind", it.kind.name)
                        .put("assignee", it.assigneeId).put("auto", it.auto)
                        .put("time", it.time ?: JSONObject.NULL).put("repeat", it.repeat.name)
                        .put("weekday", it.weekday).put("done", it.doneDate ?: JSONObject.NULL)
                )
            }
        })
        o.put("boxes", JSONArray().also { a ->
            s.boxes.forEach {
                a.put(JSONObject().put("id", it.id).put("name", it.name).put("cleaned", it.cleanedDate ?: JSONObject.NULL))
            }
        })
        val st = s.settings
        o.put(
            "settings",
            JSONObject().put("primary", st.primary).put("accent", st.accent)
                .put("feedReminder", st.feedReminder).put("feedTime", st.feedTime)
                .put("litterReminder", st.litterReminder).put("litterTime", st.litterTime)
        )
        return o.toString()
    }

    fun decode(text: String?): AppState {
        if (text.isNullOrBlank()) return defaultState()
        return try {
            val o = JSONObject(text)
            val people = o.getJSONArray("people").let { a ->
                (0 until a.length()).map { i ->
                    val p = a.getJSONObject(i)
                    Person(p.getInt("id"), p.getString("name"), p.getLong("color"))
                }
            }.ifEmpty { defaultState().people }
            val pets = o.getJSONArray("pets").let { a ->
                (0 until a.length()).map { i ->
                    val p = a.getJSONObject(i)
                    Pet(
                        id = p.getInt("id"), name = p.getString("name"),
                        type = PetType.valueOf(p.getString("type")),
                        otherType = p.optString("otherType", ""), photoPath = p.str("photo"),
                        feedGroup = p.optBoolean("feedGroup", true), fedDate = p.str("fed"),
                    )
                }
            }
            val tasks = o.getJSONArray("tasks").let { a ->
                (0 until a.length()).map { i ->
                    val t = a.getJSONObject(i)
                    Task(
                        id = t.getInt("id"), title = t.getString("title"),
                        kind = Kind.valueOf(t.getString("kind")), assigneeId = t.getInt("assignee"),
                        auto = t.optBoolean("auto", false), time = t.str("time"),
                        repeat = Repeat.valueOf(t.getString("repeat")), weekday = t.optInt("weekday", 2),
                        doneDate = t.str("done"),
                    )
                }
            }
            val boxes = o.getJSONArray("boxes").let { a ->
                (0 until a.length()).map { i ->
                    val b = a.getJSONObject(i)
                    LitterBox(b.getInt("id"), b.getString("name"), b.str("cleaned"))
                }
            }
            val st = o.getJSONObject("settings")
            AppState(
                people = people,
                meId = o.optInt("meId", people.first().id),
                pets = pets, tasks = tasks, boxes = boxes,
                settings = AppSettings(
                    primary = st.optInt("primary", 0), accent = st.optInt("accent", 0),
                    feedReminder = st.optBoolean("feedReminder", false), feedTime = st.optString("feedTime", "19:00"),
                    litterReminder = st.optBoolean("litterReminder", false), litterTime = st.optString("litterTime", "20:00"),
                ),
                nextId = o.optInt("nextId", 100), rotateIdx = o.optInt("rotateIdx", 0),
            )
        } catch (e: Exception) {
            defaultState()
        }
    }
}
