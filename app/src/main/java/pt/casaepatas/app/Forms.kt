package pt.casaepatas.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private fun Repeat.short() = when (this) {
    Repeat.NONE -> "Uma vez"
    Repeat.DAILY -> "Diária"
    Repeat.WEEKLY -> "Semanal"
}

@Composable
fun NewTaskForm(store: AppStore, onClose: () -> Unit) {
    val st = store.state
    val ctx = LocalContext.current
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(Kind.HOME) }
    var time by remember { mutableStateOf<String?>(null) }
    var repeat by remember { mutableStateOf(Repeat.NONE) }
    var weekday by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) }
    var mode by remember { mutableStateOf(AssignMode.AUTO) }
    var manualId by remember { mutableIntStateOf(st.meId) }

    val suggested = suggestAssignee(st.people, st.tasks)
    val rotated = st.people[st.rotateIdx % st.people.size]
    val tm = time

    Column(Modifier.fillMaxSize()) {
        TopBar("Nova tarefa", onClose)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Nome da tarefa") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Kind.entries.forEach { k ->
                    FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label()) })
                }
            }

            SectionTitle("Lembrete")
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tm == null) {
                    OutlinedButton(onClick = { pickTime(ctx, "09:00") { time = it } }) { Text("Definir hora") }
                } else {
                    OutlinedButton(onClick = { pickTime(ctx, tm) { time = it } }) { Text("às $tm") }
                    TextButton(onClick = { time = null }) { Text("Sem lembrete") }
                }
            }
            if (tm != null && !Notifications.notificationsEnabled(ctx)) {
                Text("As notificações estão desativadas. Ativa-as em Definições.", color = Muted, fontSize = 13.sp)
            }

            SectionTitle("Repete")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Repeat.entries.forEach { r ->
                    FilterChip(selected = repeat == r, onClick = { repeat = r }, label = { Text(r.short()) })
                }
            }
            if (repeat == Repeat.WEEKLY) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeekdayLabels.forEach { (d, l) ->
                        FilterChip(selected = weekday == d, onClick = { weekday = d }, label = { Text(l) })
                    }
                }
            }

            SectionTitle("Quem faz?")
            val options = listOf(
                Triple(AssignMode.AUTO, "Automático", "A app escolhe quem tem menos tarefas por fazer"),
                Triple(AssignMode.ROTATE, "À vez", "Cada pessoa faz por ordem"),
                Triple(AssignMode.MANUAL, "Escolher eu", "Defino já quem faz"),
            )
            options.forEach { (m, t, s) ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (mode == m) MaterialTheme.colorScheme.primaryContainer else Color.White,
                    border = BorderStroke(if (mode == m) 2.dp else 1.dp, if (mode == m) MaterialTheme.colorScheme.primary else Line),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(20.dp)).clickable { mode = m },
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = mode == m, onClick = { mode = m })
                        Column(Modifier.weight(1f)) {
                            Text(t, fontWeight = FontWeight.Bold)
                            Text(s, color = Muted, fontSize = 13.sp)
                        }
                    }
                }
            }

            when (mode) {
                AssignMode.AUTO, AssignMode.ROTATE -> {
                    val who = if (mode == AssignMode.AUTO) suggested else rotated
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            PersonAvatar(who, 48.dp)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    if (mode == AssignMode.AUTO) "Sugestão da app" else "Próxima vez",
                                    color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
                                )
                                Text("${who.name} faz esta tarefa", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                if (mode == AssignMode.AUTO) {
                                    val counts = st.people.joinToString(" · ") { p ->
                                        "${p.name} ${st.tasks.count { it.assigneeId == p.id && !it.isDone() }}"
                                    }
                                    Text("Por fazer: $counts", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                AssignMode.MANUAL -> {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        st.people.forEach { p ->
                            FilterChip(selected = manualId == p.id, onClick = { manualId = p.id }, label = { Text(p.name) })
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Button(
                onClick = {
                    val assignee = when (mode) {
                        AssignMode.AUTO -> suggested.id
                        AssignMode.ROTATE -> rotated.id
                        AssignMode.MANUAL -> manualId
                    }
                    store.addTask(
                        Task(0, title.trim(), kind, assignee, mode != AssignMode.MANUAL, time, repeat, weekday, null),
                        rotate = mode == AssignMode.ROTATE,
                    )
                    onClose()
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Criar tarefa", fontSize = 16.sp) }
        }
    }
}

@Composable
fun NewPetForm(store: AppStore, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var photo by remember { mutableStateOf<String?>(null) }
    var type by remember { mutableStateOf(PetType.CAT) }
    var other by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var group by remember { mutableStateOf(true) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photo = copyPhoto(ctx, uri)
    }
    val preview = Pet(0, name.ifBlank { "?" }, type, other, photo)
    val groupLabel = when (type) {
        PetType.CAT -> "Come à mesma hora que os outros gatos"
        PetType.DOG -> "Come à mesma hora que os outros cães"
        PetType.OTHER -> "Come à mesma hora que os outros do mesmo tipo"
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Novo animal", onClose)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PetAvatar(preview, 120.dp) }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                OutlinedButton(onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(if (photo == null) "Escolher foto" else "Mudar foto") }
                if (photo != null) TextButton(onClick = { photo = null }) { Text("Remover") }
            }

            SectionTitle("Tipo de animal")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(PetType.CAT to "Gato", PetType.DOG to "Cão", PetType.OTHER to "Outro").forEach { (t, l) ->
                    val on = type == t
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (on) MaterialTheme.colorScheme.primaryContainer else Color.White,
                        border = BorderStroke(if (on) 2.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else Line),
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable { type = t },
                    ) {
                        Column(Modifier.padding(vertical = 14.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            Text(l, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (type == PetType.OTHER) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = other, onValueChange = { other = it },
                    label = { Text("Que animal é? (ex.: coelho)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))
            AppCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(groupLabel, fontWeight = FontWeight.SemiBold)
                        Text("Um só toque regista a refeição de todos", color = Muted, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = group, onCheckedChange = { group = it })
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Button(
                onClick = {
                    store.addPet(Pet(0, name.trim(), type, other.trim(), photo, group, null))
                    onClose()
                },
                enabled = name.isNotBlank() && (type != PetType.OTHER || other.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Adicionar animal", fontSize = 16.sp) }
        }
    }
}
