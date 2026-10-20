package pt.casaepatas.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun litterRelevant(pets: List<Pet>, boxes: List<LitterBox>): Boolean =
    pets.any { it.type == PetType.CAT } || boxes.isNotEmpty()

/** Um cartão por grupo (gatos, cães, ...): um toque regista a refeição de todos. */
@Composable
fun FeedCards(store: AppStore, pets: List<Pet>) {
    val today = todayStr()
    val groups = pets.filter { it.feedGroup }.groupBy { it.groupKey() }
    groups.forEach { (_, list) ->
        val allFed = list.all { it.fedDate == today }
        val first = list.first()
        val title = when {
            list.size == 1 -> "Alimentar ${first.name}"
            first.type == PetType.CAT -> "Alimentar os gatos"
            first.type == PetType.DOG -> "Alimentar os cães"
            else -> "Alimentar: ${first.otherType.ifBlank { "outros" }}"
        }
        AppCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                PetAvatar(first, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        list.joinToString(", ") { it.name }, color = Muted, fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (allFed) {
                    FilledTonalButton(onClick = { store.setFed(list.map { it.id }, false) }) { Text("Feito") }
                } else {
                    Button(onClick = { store.setFed(list.map { it.id }, true) }) {
                        Text(if (list.size == 1) "Dar ração" else "Dar a todos (${list.size})")
                    }
                }
            }
        }
    }
}

@Composable
fun LitterCard(store: AppStore) {
    val st = store.state
    val today = todayStr()
    val boxes = st.boxes
    val cleaned = boxes.count { it.cleanedDate == today }
    val all = boxes.isNotEmpty() && cleaned == boxes.size
    AppCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Limpar as areias", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            boxes.isEmpty() -> "Ainda sem caixas de areia"
                            all -> "Todas as ${boxes.size} caixas limpas"
                            else -> "$cleaned de ${boxes.size} caixas limpas"
                        },
                        color = Muted, fontSize = 13.sp,
                    )
                }
                if (boxes.isNotEmpty()) {
                    if (all) {
                        FilledTonalButton(onClick = { store.setAllBoxes(false) }) { Text("Feito") }
                    } else {
                        Button(onClick = { store.setAllBoxes(true) }) { Text("Limpei todas (${boxes.size})") }
                    }
                }
            }
            if (boxes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    boxes.forEach { b ->
                        val d = b.cleanedDate == today
                        if (d) {
                            FilledTonalButton(onClick = { store.toggleBox(b.id) }, modifier = Modifier.weight(1f)) {
                                Text("${b.name} ✓", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } else {
                            OutlinedButton(onClick = { store.toggleBox(b.id) }, modifier = Modifier.weight(1f)) {
                                Text(b.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            Row {
                TextButton(onClick = { store.addBox() }, enabled = boxes.size < 4) { Text("+ Caixa") }
                if (boxes.isNotEmpty()) {
                    TextButton(onClick = { store.removeBox() }) { Text("− Caixa") }
                }
            }
        }
    }
}

@Composable
fun AnimaisScreen(store: AppStore, wide: Boolean, onOpenPet: (Int) -> Unit, onNew: () -> Unit) {
    val st = store.state
    var filter by rememberSaveable { mutableStateOf("ALL") }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }

    val chips = buildList {
        add("ALL" to "Todos ${st.pets.size}")
        val cats = st.pets.count { it.type == PetType.CAT }
        val dogs = st.pets.count { it.type == PetType.DOG }
        val others = st.pets.count { it.type == PetType.OTHER }
        if (cats > 0) add("CAT" to "Gatos $cats")
        if (dogs > 0) add("DOG" to "Cães $dogs")
        if (others > 0) add("OTHER" to "Outros $others")
    }
    val effective = if (chips.any { it.first == filter }) filter else "ALL"
    val shown = st.pets.filter { effective == "ALL" || it.type.name == effective }

    val list: @Composable (Modifier) -> Unit = { mod ->
        Column(mod.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Animais", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (st.pets.isEmpty()) {
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Ainda sem animais", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Adiciona o primeiro com foto e tipo (gato, cão ou outro).", color = Muted)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onNew) { Text("Adicionar animal") }
                    }
                }
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { (k, l) ->
                        FilterChip(selected = effective == k, onClick = { filter = k }, label = { Text(l) })
                    }
                }
                SectionTitle("Cuidados de todos de uma vez")
                FeedCards(store, shown)
                if ((effective == "ALL" || effective == "CAT") && litterRelevant(shown, st.boxes)) {
                    LitterCard(store)
                }
                SectionTitle("Os meus animais")
                AppCard(Modifier.fillMaxWidth()) {
                    shown.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(color = Line)
                        PetRow(store, p, wide && selected == p.id) { if (wide) selected = p.id else onOpenPet(p.id) }
                    }
                }
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
                    Text("Escolhe um animal na lista para ver o perfil.", color = Muted)
                } else {
                    PetDetail(store, id, Modifier) { selected = null }
                }
            }
        }
    } else {
        list(Modifier.fillMaxSize())
    }
}

@Composable
fun PetRow(store: AppStore, p: Pet, selected: Boolean, onClick: () -> Unit) {
    val fed = p.fedDate == todayStr()
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PetAvatar(p, 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("${p.typeLabel()} · ${if (fed) "Alimentado" else "Por alimentar"}", color = Muted, fontSize = 13.sp)
        }
        if (!p.feedGroup) {
            if (fed) {
                FilledTonalButton(onClick = { store.setFed(listOf(p.id), false) }) { Text("Feito") }
            } else {
                Button(onClick = { store.setFed(listOf(p.id), true) }) { Text("Dar ração") }
            }
        }
    }
}

@Composable
fun PetDetail(store: AppStore, id: Int, modifier: Modifier, onGone: () -> Unit) {
    val ctx = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            copyPhoto(ctx, uri)?.let { path -> store.updatePet(id) { it.copy(photoPath = path) } }
        }
    }
    val pet = store.state.pets.firstOrNull { it.id == id }
    var confirm by remember { mutableStateOf(false) }
    if (pet == null) {
        LaunchedEffect(id) { onGone() }
        return
    }
    var name by remember(pet.id) { mutableStateOf(pet.name) }
    val fed = pet.fedDate == todayStr()

    Column(modifier) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PetAvatar(pet, 128.dp) }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            OutlinedButton(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text(if (pet.photoPath == null) "Escolher foto" else "Mudar foto") }
            if (pet.photoPath != null) {
                TextButton(onClick = { store.updatePet(id) { it.copy(photoPath = null) } }) { Text("Remover") }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { v ->
                name = v
                if (v.isNotBlank()) store.updatePet(id) { it.copy(name = v.trim()) }
            },
            label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text("Tipo: ${pet.typeLabel()}", color = Muted)
        Spacer(Modifier.height(12.dp))
        AppCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Come à mesma hora que os outros", fontWeight = FontWeight.SemiBold)
                    Text("Entra no botão «Dar a todos»", color = Muted, fontSize = 13.sp)
                }
                Switch(checked = pet.feedGroup, onCheckedChange = { v -> store.updatePet(id) { it.copy(feedGroup = v) } })
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (fed) {
                FilledTonalButton(onClick = { store.setFed(listOf(id), false) }, modifier = Modifier.weight(1f)) {
                    Text("Alimentado hoje (desfazer)")
                }
            } else {
                Button(onClick = { store.setFed(listOf(id), true) }, modifier = Modifier.weight(1f)) {
                    Text("Dar ração")
                }
            }
            OutlinedButton(onClick = { confirm = true }) { Text("Eliminar") }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Eliminar ${pet.name}?") },
            text = { Text("O perfil e a foto deixam de existir neste telemóvel.") },
            confirmButton = {
                TextButton(onClick = { confirm = false; store.deletePet(id); onGone() }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } },
        )
    }
}
