package pt.casaepatas.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun FamiliaScreen(store: AppStore) {
    val st = store.state
    var newName by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("Família", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        AppCard(Modifier.fillMaxWidth()) {
            st.people.forEachIndexed { i, p ->
                if (i > 0) HorizontalDivider(color = Line)
                val mine = st.tasks.filter { it.assigneeId == p.id }
                val doneToday = mine.count { it.dueToday() && it.isDone() }
                val pending = mine.count { !it.isDone() }
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    PersonAvatar(p, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name + if (p.id == st.meId) " (tu)" else "", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("$doneToday feitas hoje · $pending por fazer", color = Muted, fontSize = 13.sp)
                    }
                    if (p.id != st.meId) TextButton(onClick = { store.setMe(p.id) }) { Text("Sou eu") }
                    if (st.people.size > 1) {
                        IconButton(onClick = { store.removePerson(p.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remover ${p.name}")
                        }
                    }
                }
            }
        }
        SectionTitle("Adicionar pessoa")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName, onValueChange = { newName = it },
                label = { Text("Nome") }, singleLine = true, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { store.addPerson(newName); newName = "" }, enabled = newName.isNotBlank()) { Text("Adicionar") }
        }
        Spacer(Modifier.height(16.dp))
        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Partilha entre telemóveis", fontWeight = FontWeight.Bold)
                Text(
                    "Nesta versão de teste os membros ficam guardados só neste telemóvel. " +
                        "A sincronização online com outras pessoas ainda não está incluída.",
                    color = Muted, fontSize = 13.sp,
                )
            }
        }
        Spacer(Modifier.height(88.dp))
    }
}

@Composable
fun DefinicoesScreen(store: AppStore, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val st = store.state
    val s = st.settings
    var tick by remember { mutableIntStateOf(0) }

    val activity = ctx as? ComponentActivity
    DisposableEffect(activity) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        activity?.lifecycle?.addObserver(obs)
        onDispose { activity?.lifecycle?.removeObserver(obs) }
    }
    val notifOn = remember(tick) { Notifications.notificationsEnabled(ctx) }
    val exactOn = remember(tick) { Notifications.canExact(ctx) }
    val scheduled = remember(tick, st) { Notifications.scheduledCount(ctx) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    Column(Modifier.fillMaxSize()) {
        TopBar("Definições", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {

            SectionTitle("Cor principal")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Primaries.forEachIndexed { i, (name, c) ->
                    Swatch(Color(c), s.primary == i, "Cor principal $name", Color.White) {
                        store.setSettings { it.copy(primary = i) }
                    }
                }
            }
            SectionTitle("Cor de destaque")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Accents.forEachIndexed { i, (name, c) ->
                    Swatch(Color(c), s.accent == i, "Cor de destaque $name", Ink) {
                        store.setSettings { it.copy(accent = i) }
                    }
                }
            }

            SectionTitle("Notificações")
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    StatusRow("Notificações", notifOn, "Ativadas", "Desativadas")
                    if (!notifOn) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (Build.VERSION.SDK_INT >= 33) {
                                Button(onClick = { askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                    Text("Permitir")
                                }
                            }
                            OutlinedButton(onClick = {
                                ctx.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                                )
                            }) { Text("Abrir definições do sistema") }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= 31) {
                        HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 12.dp))
                        StatusRow("Alarmes exatos", exactOn, "Permitidos", "Não permitidos")
                        if (!exactOn) {
                            Text(
                                "Sem esta permissão os lembretes podem atrasar alguns minutos.",
                                color = Muted, fontSize = 13.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                ctx.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                                )
                            }) { Text("Permitir alarmes exatos") }
                        }
                    }
                    HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 12.dp))
                    Text("Lembretes agendados: $scheduled", color = Muted, fontSize = 13.sp)
                }
            }

            SectionTitle("Testar")
            Button(
                onClick = {
                    Notifications.show(ctx, Notifications.TEST_ID, "Teste de notificação", "Se estás a ver isto, as notificações funcionam.")
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enviar notificação agora") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    Notifications.scheduleTest(ctx, 60)
                    Toast.makeText(ctx, "Fecha a app: o lembrete chega dentro de 1 minuto.", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Testar lembrete agendado (1 minuto)") }
            Text(
                "O segundo teste usa o mesmo caminho dos lembretes reais (alarme do sistema). " +
                    "Faz-o com a app fechada.",
                color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp),
            )

            SectionTitle("Lembretes automáticos")
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    ReminderRow(
                        "Hora de dar de comer", s.feedReminder, s.feedTime,
                        onToggle = { v -> store.setSettings { it.copy(feedReminder = v) } },
                        onTime = { t -> store.setSettings { it.copy(feedTime = t) } },
                    )
                    HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 12.dp))
                    ReminderRow(
                        "Limpar as areias", s.litterReminder, s.litterTime,
                        onToggle = { v -> store.setSettings { it.copy(litterReminder = v) } },
                        onTime = { t -> store.setSettings { it.copy(litterTime = t) } },
                    )
                    Text(
                        "Só avisam se ainda houver animais por alimentar ou caixas por limpar nesse dia.",
                        color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            Text(
                "Se os lembretes falharem com a app fechada, em Definições do Android > Apps > Casa e Patas > " +
                    "Bateria, escolhe «Sem restrições».",
                color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, description: String, tick: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(color).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Default.Check, contentDescription = "$description (escolhida)", tint = tick)
        else Spacer(Modifier.size(1.dp))
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean, yes: String, no: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Pill(
            if (ok) yes else no,
            if (ok) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFBE1DB),
            if (ok) MaterialTheme.colorScheme.primary else Color(0xFF8F2A18),
        )
    }
}

@Composable
private fun ReminderRow(title: String, on: Boolean, time: String, onToggle: (Boolean) -> Unit, onTime: (String) -> Unit) {
    val ctx = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { pickTime(ctx, time, onTime) }) { Text("às $time") }
        }
        Switch(checked = on, onCheckedChange = onToggle)
    }
}
