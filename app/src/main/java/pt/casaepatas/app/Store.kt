package pt.casaepatas.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Estado da app + gravação no telemóvel + (re)agendamento dos lembretes. */
class AppStore(private val ctx: Context) {

    var state by mutableStateOf(read(ctx))
        private set

    private fun update(block: (AppState) -> AppState) {
        state = block(state)
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, StateJson.encode(state)).apply()
        Notifications.scheduleAll(ctx, state)
    }

    // ---- família ----
    fun addPerson(name: String) = update { s ->
        val color = PersonColors[s.people.size % PersonColors.size]
        s.copy(people = s.people + Person(s.nextId, name.trim(), color), nextId = s.nextId + 1)
    }

    fun removePerson(id: Int) = update { s ->
        if (s.people.size <= 1) {
            s
        } else {
            val rest = s.people.filter { it.id != id }
            s.copy(
                people = rest,
                meId = if (s.meId == id) rest.first().id else s.meId,
                tasks = s.tasks.map { if (it.assigneeId == id) it.copy(assigneeId = rest.first().id) else it },
            )
        }
    }

    fun setMe(id: Int) = update { it.copy(meId = id) }

    // ---- tarefas ----
    fun addTask(t: Task, rotate: Boolean) = update { s ->
        s.copy(
            tasks = s.tasks + t.copy(id = s.nextId),
            nextId = s.nextId + 1,
            rotateIdx = if (rotate) s.rotateIdx + 1 else s.rotateIdx,
        )
    }

    fun toggleTask(id: Int) = update { s ->
        s.copy(tasks = s.tasks.map {
            if (it.id == id) it.copy(doneDate = if (it.isDone()) null else todayStr()) else it
        })
    }

    fun deleteTask(id: Int) = update { s -> s.copy(tasks = s.tasks.filter { it.id != id }) }

    // ---- animais ----
    fun addPet(p: Pet) = update { s ->
        s.copy(pets = s.pets + p.copy(id = s.nextId), nextId = s.nextId + 1)
    }

    fun updatePet(id: Int, f: (Pet) -> Pet) = update { s ->
        s.copy(pets = s.pets.map { if (it.id == id) f(it) else it })
    }

    fun deletePet(id: Int) = update { s -> s.copy(pets = s.pets.filter { it.id != id }) }

    /** Marca (ou desmarca) vários animais como alimentados hoje de uma só vez. */
    fun setFed(ids: Collection<Int>, fed: Boolean) = update { s ->
        s.copy(pets = s.pets.map { if (it.id in ids) it.copy(fedDate = if (fed) todayStr() else null) else it })
    }

    // ---- caixas de areia ----
    fun addBox() = update { s ->
        if (s.boxes.size >= 4) s else s.copy(
            boxes = s.boxes + LitterBox(s.nextId, "Caixa ${s.boxes.size + 1}", null),
            nextId = s.nextId + 1,
        )
    }

    fun removeBox() = update { s -> s.copy(boxes = s.boxes.dropLast(1)) }

    fun toggleBox(id: Int) = update { s ->
        s.copy(boxes = s.boxes.map {
            if (it.id == id) it.copy(cleanedDate = if (it.cleanedDate == todayStr()) null else todayStr()) else it
        })
    }

    fun setAllBoxes(clean: Boolean) = update { s ->
        s.copy(boxes = s.boxes.map { it.copy(cleanedDate = if (clean) todayStr() else null) })
    }

    // ---- definições ----
    fun setSettings(f: (AppSettings) -> AppSettings) = update { it.copy(settings = f(it.settings)) }

    companion object {
        private const val PREFS = "casa_e_patas"
        private const val KEY = "state"

        fun read(ctx: Context): AppState =
            StateJson.decode(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))
    }
}
