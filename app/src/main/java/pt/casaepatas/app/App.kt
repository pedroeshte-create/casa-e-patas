package pt.casaepatas.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

enum class MainTab(val label: String, val icon: ImageVector) {
    HOJE("Hoje", Icons.Default.Home),
    TAREFAS("Tarefas", Icons.Default.CheckCircle),
    ANIMAIS("Animais", Icons.Default.Pets),
    FAMILIA("Família", Icons.Default.People),
}

sealed interface Overlay {
    data object NewTask : Overlay
    data object NewPet : Overlay
    data object Definicoes : Overlay
    data class OpenTask(val id: Int) : Overlay
    data class OpenPet(val id: Int) : Overlay
}

/**
 * Layout adaptativo: abaixo de 600 dp de largura (Pixel 7, Fold fechado) usa barra em baixo e
 * uma coluna; a partir de 600 dp (Fold 7 aberto) usa barra lateral e dois painéis lado a lado.
 */
@Composable
fun AppRoot(store: AppStore) {
    AppTheme(store.state.settings) {
        var tab by rememberSaveable { mutableStateOf(MainTab.HOJE) }
        var overlay by remember { mutableStateOf<Overlay?>(null) }
        val wide = LocalConfiguration.current.screenWidthDp >= 600
        BackHandler(enabled = overlay != null) { overlay = null }

        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (wide) {
                NavigationRail(containerColor = Color.White) {
                    MainTab.entries.forEach { t ->
                        NavigationRailItem(
                            selected = overlay == null && tab == t,
                            onClick = { overlay = null; tab = t },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    NavigationRailItem(
                        selected = overlay == Overlay.Definicoes,
                        onClick = { overlay = Overlay.Definicoes },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Definições") },
                        label = { Text("Definições") },
                    )
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (val o = overlay) {
                        Overlay.NewTask -> NewTaskForm(store) { overlay = null }
                        Overlay.NewPet -> NewPetForm(store) { overlay = null }
                        Overlay.Definicoes -> DefinicoesScreen(store) { overlay = null }
                        is Overlay.OpenTask -> Column(Modifier.fillMaxSize()) {
                            TopBar("Tarefa", { overlay = null })
                            TaskDetail(
                                store, o.id,
                                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                            ) { overlay = null }
                        }

                        is Overlay.OpenPet -> Column(Modifier.fillMaxSize()) {
                            TopBar("Animal", { overlay = null })
                            PetDetail(
                                store, o.id,
                                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                            ) { overlay = null }
                        }

                        null -> {
                            when (tab) {
                                MainTab.HOJE -> HojeScreen(
                                    store, wide,
                                    onOpenTask = { overlay = Overlay.OpenTask(it) },
                                    onOpenSettings = { overlay = Overlay.Definicoes },
                                )

                                MainTab.TAREFAS -> TarefasScreen(store, wide) { overlay = Overlay.OpenTask(it) }
                                MainTab.ANIMAIS -> AnimaisScreen(
                                    store, wide,
                                    onOpenPet = { overlay = Overlay.OpenPet(it) },
                                    onNew = { overlay = Overlay.NewPet },
                                )

                                MainTab.FAMILIA -> FamiliaScreen(store)
                            }
                            if (tab != MainTab.FAMILIA) {
                                FloatingActionButton(
                                    onClick = { overlay = if (tab == MainTab.ANIMAIS) Overlay.NewPet else Overlay.NewTask },
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = Ink,
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                                ) { Icon(Icons.Default.Add, contentDescription = "Adicionar") }
                            }
                        }
                    }
                }
                if (!wide) {
                    NavigationBar(containerColor = Color.White) {
                        MainTab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = overlay == null && tab == t,
                                onClick = { overlay = null; tab = t },
                                icon = { Icon(t.icon, contentDescription = t.label) },
                                label = { Text(t.label) },
                            )
                        }
                    }
                }
            }
        }
    }
}
