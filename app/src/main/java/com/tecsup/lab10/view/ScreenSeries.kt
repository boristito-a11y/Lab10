package com.tecsup.lab10.view

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.tecsup.lab10.data.SerieApiService
import com.tecsup.lab10.data.SerieModel
import kotlinx.coroutines.launch

// Listado de series: GET /serie
@Composable
fun ContenidoSeriesListado(navController: NavHostController, servicio: SerieApiService) {
    val listaSeries = remember { mutableStateListOf<SerieModel>() }
    var mensaje by remember { mutableStateOf("Cargando series...") }

    LaunchedEffect(Unit) {
        try {
            val listado = servicio.selectSeries()
            Log.d("SERIES", "GET serie -> ${listado.size} series")
            listaSeries.clear()
            listaSeries.addAll(listado)
            mensaje = if (listado.isEmpty()) "No hay series registradas" else ""
        } catch (e: Exception) {
            Log.e("SERIES", "Error al listar", e)
            mensaje = "No se pudo conectar con la API: ${e.message}"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillParentMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "ID", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.1f))
                Text(text = "SERIE", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
                Text(text = "Acción", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.2f))
            }
        }
        if (mensaje.isNotEmpty()) {
            item { Text(text = mensaje, modifier = Modifier.padding(8.dp)) }
        }
        items(listaSeries) { item ->
            Row(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .fillParentMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${item.id}", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.1f))
                Text(text = item.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
                IconButton(
                    onClick = { navController.navigate("serieVer/${item.id}") },
                    modifier = Modifier.weight(0.1f)
                ) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Editar")
                }
                IconButton(
                    onClick = { navController.navigate("serieDel/${item.id}") },
                    modifier = Modifier.weight(0.1f)
                ) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Eliminar")
                }
            }
        }
    }
}

// Crear (POST /serie) o editar (GET y PUT /serie/{id})
@Composable
fun ContenidoSerieEditar(navController: NavHostController, servicio: SerieApiService, pid: Int = 0) {
    val id = pid
    var name by remember { mutableStateOf("") }
    var releaseDate by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    if (id != 0) {
        LaunchedEffect(id) {
            try {
                val respuesta = servicio.selectSerie(id.toString())
                Log.d("SERIES", "GET serie/$id -> ${respuesta.code()}")
                respuesta.body()?.let { serie ->
                    name = serie.name
                    releaseDate = serie.release_date
                    rating = serie.rating.toString()
                    category = serie.category
                }
            } catch (e: Exception) {
                error = "No se pudo cargar la serie: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = id.toString(),
            onValueChange = { },
            label = { Text("ID (solo lectura)") },
            readOnly = true,
            singleLine = true
        )
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true
        )
        TextField(
            value = releaseDate,
            onValueChange = { releaseDate = it },
            label = { Text("Release Date (AAAA-MM-DD)") },
            singleLine = true
        )
        TextField(
            value = rating,
            onValueChange = { rating = it },
            label = { Text("Rating (número)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        TextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category: horror, comedy, action o drama") },
            singleLine = true
        )
        if (error.isNotEmpty()) {
            Text(text = error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
        }
        Button(
            onClick = {
                val objSerie = SerieModel(id, name, releaseDate, rating.toIntOrNull() ?: 0, category)
                scope.launch {
                    try {
                        val respuesta = if (id == 0) {
                            servicio.insertSerie(objSerie)
                        } else {
                            servicio.updateSerie(id.toString(), objSerie)
                        }
                        Log.d("SERIES", "${if (id == 0) "POST" else "PUT"} -> ${respuesta.code()} ${respuesta.body()}")
                        if (respuesta.isSuccessful) {
                            navController.navigate("series") {
                                popUpTo("series") { inclusive = true }
                            }
                        } else {
                            error = "La API respondió ${respuesta.code()}: ${respuesta.errorBody()?.string()}"
                        }
                    } catch (e: Exception) {
                        error = "Error de conexión: ${e.message}"
                    }
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Grabar", fontSize = 16.sp)
        }
    }
}

// Eliminar: DELETE /serie/{id}
@Composable
fun ContenidoSerieEliminar(navController: NavHostController, servicio: SerieApiService, id: Int) {
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { navController.popBackStack() },
        title = { Text(text = "Confirmar Eliminación") },
        text = { Text(if (error.isEmpty()) "¿Está seguro de eliminar la serie con ID $id?" else error) },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val respuesta = servicio.deleteSerie(id.toString())
                            Log.d("SERIES", "DELETE serie/$id -> ${respuesta.code()}")
                            if (respuesta.isSuccessful) {
                                navController.navigate("series") {
                                    popUpTo("series") { inclusive = true }
                                }
                            } else {
                                error = "La API respondió ${respuesta.code()}"
                            }
                        } catch (e: Exception) {
                            error = "Error de conexión: ${e.message}"
                        }
                    }
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            Button(onClick = { navController.popBackStack() }) {
                Text("Cancelar")
            }
        }
    )
}