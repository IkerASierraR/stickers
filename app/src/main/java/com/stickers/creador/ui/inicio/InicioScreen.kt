package com.stickers.creador.ui.inicio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stickers.creador.data.CarpetaResumen
import com.stickers.creador.data.StickerRepositorio
import com.stickers.creador.ui.coloresBarra
import com.stickers.creador.ui.componentes.CarpetaCard
import com.stickers.creador.ui.componentes.EstadoVacio
import com.stickers.creador.ui.contenedor
import com.stickers.creador.ui.theme.Primario
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class InicioViewModel(repo: StickerRepositorio) : ViewModel() {
    /** null = cargando. */
    val carpetas: StateFlow<List<CarpetaResumen>?> = repo.observarCarpetas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    onAbrirCarpeta: (Long) -> Unit,
    onNuevaCarpeta: () -> Unit,
) {
    val repo = contenedor().repositorio
    val vm: InicioViewModel = viewModel { InicioViewModel(repo) }
    val carpetas by vm.carpetas.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis stickers") }, colors = coloresBarra()) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevaCarpeta,
                icon = { Icon(Icons.Rounded.CreateNewFolder, null) },
                text = { Text("Nueva carpeta") },
                containerColor = Primario,
                contentColor = Color.White,
            )
        },
    ) { padding ->
        val lista = carpetas
        when {
            lista == null -> Box(Modifier.fillMaxSize().padding(padding))
            lista.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EstadoVacio("Crea tu primera carpeta de stickers")
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                items(lista, key = { it.carpeta.id }) { r ->
                    CarpetaCard(r, onClick = { onAbrirCarpeta(r.carpeta.id) })
                }
            }
        }
    }
}
