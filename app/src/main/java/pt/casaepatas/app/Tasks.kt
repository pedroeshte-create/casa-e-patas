package pt.casaepatas.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TaskRow(store: AppStore, t: Task, selected: Boolean, onClick: () -> Unit) {
    val done = t.isDone()
    val who = store.state.people.firstOrNull { it.id == t.assigneeId }
    val sub = listOfNotNull(
        t.time,
        if (t.repeat == Repeat.WEEKLY) weekdayName(t.weekday) else null,
        t.repeat.label(),
        t.kind.label(),
    ).joinToString(" · ")
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = done, onCheckedChange = { store.toggleTask(t.id) })
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                t.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = if (done) Muted else Ink,
                textDecoration = if (done) TextDecoration.LineThrough else null,
            )
            Text(sub, color = Muted, fontSize = 13.sp)
        }
        PersonAvatar(who)
    }
}

@Composable
fun TaskList(store: AppStore, tasks: List<Task>, selectedId: Int?, onOpen: (Int) -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        tasks.forEachIndexed { i, t ->
            if (i > 0) HorizontalDivider(color = Line)
            TaskRow(store, t, t.id == selectedId) { onOpen(t.id) }
        }
    }
}

@Composable
fun HojeScreen(store: AppStore, wide: Boolean, onOpenTask: (Int) -> Unit, onOpenSettings: () -> Unit) {
    val st = store.state
    val me = st.people.firstOrNull { it.id == st.meId } ?: st.people.first()
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greet = when {
        hour < 12 -> "Bom dia"
        hour < 20 -> "Boa tarde"
        else -> "Boa noite"
    }
    val date = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-PT")).format(Date())
        .replaceFirstChar { it.uppercase() }
    val today = st.tasks.filter { it.dueToday() }
    val doneN = today.count { it.isDone() }
    val fraction = if (today.isEmpty()) 0f else doneN.toFloat() / today.size

    val tasksBlock: @Composable () -> Unit = {
        SectionTitle("Para fazer hoje")
        if (today.isEmpty()) {
            Text("Sem tarefas para hoje. Toca no + para criar uma.", color = Muted)
        } else {
            TaskList(store, today, null, onOpenTask)
        }
    }
    val petsBlock: @Composable () -> Unit = {
        SectionTitle("Animais hoje")
        if (st.pets.isEmpty()) {
            Text("Ainda sem animais. Adiciona o primeiro no separador Animais.", color = Muted)
        } else {
            FeedCards(store, st.pets)
            if (litterRelevant(st.pets, st.boxes)) {
                LitterCard(store)
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(date, color = Muted, fontSize = 14.sp)
                Text("$greet, ${me.name}", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Definições")
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Progresso de hoje", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "$doneN de ${today.size} feitas", color = Color.White,
                    fontSize = 28.sp, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = Color.Black.copy(alpha = 0.25f),
                )
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f)) { tasksBlock() }
                Column(Modifier.weight(1f)) { petsBlock() }
            }
        } else {
            petsBlock()
            tasksBlock()
        }
        Spacer(Modifier.height(88.dp))
    }
}

@Composable
fun TarefasScreen(store: AppStore, wide: Boolean, onOpenTask: (Int) -> Unit) {
    val st = store.state
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val labels = listOf("Todas", "Minhas", "Casa", "Animais")
    val tasks = st.tasks.filter {
        when (filter) {
            1 -> it.assigneeId == st.meId
            2 -> it.kind == Kind.HOME
            3 -> it.kind == Kind.PET
            else -> true
        }
    }
    val today = tasks.filter { it.dueToday() }
    val later = tasks.filter { !it.dueToday() }

    val list: @Composable (Modifier) -> Unit = { mod ->
        Column(mod.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Tarefas", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEachIndexed { i, l ->
                    FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(l) })
                }
            }
            if (tasks.isEmpty()) {
                SectionTitle("Sem tarefas")
                Text("Toca no + para criar a primeira tarefa.", color = Muted)
            }
            if (today.isNotEmpty()) {
                SectionTitle("Hoje")
                TaskList(store, today, if (wide) selected else null) { if (wide) selected = it else onOpenTask(it) }
            }
            if (later.isNotEmpty()) {
                SectionTitle("Mais tarde")
                TaskList(store, later, if (wide) selected else null) { if (wide) selected = it else onOpenTask(it) }
            }
            Spacer(Modifier.height(88.dp))
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize()) {
            list(Modifier.weight(0.45f).fillMaxHeight())
            Spacer(Modifier.width(1.dp).fillMaxHeight().background(Line))
            Column(Modifier.weight(0.55f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(24.dp)) {
                val id = selected
                if (id == null) {
                    Text("Escolhe uma tarefa na lista para ver os detalhes.", color = Muted)
                } else {
                    TaskDetail(store, id, Modifier) { selected = null }
                }
            }
        }
    } else {
        list(Modifier.fillMaxSize())
    }
}

@Composable
fun TaskDetail(store: AppStore, id: Int, modifier: Modifier, onGone: () -> Unit) {
    val st = store.state
    val t = st.tasks.firstOrNull { it.id == id }
    var confirm by remember { mutableStateOf(false) }
    if (t == null) {
        LaunchedEffect(id) { onGone() }
        return
    }
    val who = st.people.firstOrNull { it.id == t.assigneeId }
    val done = t.isDone()
    Column(modifier) {
        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Pill(
                    t.kind.label(),
                    if (t.kind == Kind.PET) Color(0xFFFDEBCB) else MaterialTheme.colorScheme.primaryContainer,
                    if (t.kind == Kind.PET) Color(0xFF7A4A00) else MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(10.dp))
                Text(t.title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                DetailRow("Responsável") {
                    PersonAvatar(who, 28.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(who?.name ?: "?", fontWeight = FontWeight.SemiBold)
                    if (t.auto) {
                        Spacer(Modifier.width(8.dp))
                        Pill("Automático", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider(color = Line)
                DetailRow("Lembrete") { Text(t.time ?: "Sem lembrete", fontWeight = FontWeight.SemiBold) }
                HorizontalDivider(color = Line)
                DetailRow("Repete") {
                    val extra = if (t.repeat == Repeat.WEEKLY) " (${weekdayName(t.weekday)})" else ""
                    Text(t.repeat.label() + extra, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { store.toggleTask(t.id) }, modifier = Modifier.weight(1f)) {
                Text(if (done) "Desfazer" else "Marcar como feita")
            }
            OutlinedButton(onClick = { confirm = true }) { Text("Eliminar") }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Eliminar tarefa?") },
            text = { Text("«${t.title}» deixa de existir e os lembretes são cancelados.") },
            confirmButton = {
                TextButton(onClick = { confirm = false; store.deleteTask(t.id); onGone() }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun DetailRow(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        content()
    }
}
