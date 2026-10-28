package com.tecsup.lab10.view

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.tecsup.lab10.data.PostApiService
import com.tecsup.lab10.data.PostModel
import kotlinx.coroutines.launch

// JSONPlaceholder responde como una API real pero no guarda los cambios,
// por eso cada respuesta se aplica a la lista local listaPosts.

// Listado: GET /posts
@Composable
fun ContenidoPostsListado(
    navController: NavHostController,
    servicio: PostApiService,
    listaPosts: SnapshotStateList<PostModel>
) {
    var mensaje by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (listaPosts.isEmpty()) {
            mensaje = "Cargando posts..."
            try {
                val listado = servicio.selectPosts()
                Log.d("POSTS", "GET posts -> ${listado.size} posts")
                listaPosts.addAll(listado.take(20))
                mensaje = ""
            } catch (e: Exception) {
                Log.e("POSTS", "Error al listar", e)
                mensaje = "No se pudo conectar con JSONPlaceholder: ${e.message}"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        item {
            Text(
                text = "Posts de jsonplaceholder.typicode.com",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
        }
        if (mensaje.isNotEmpty()) {
            item { Text(text = mensaje, modifier = Modifier.padding(8.dp)) }
        }
        items(listaPosts) { post ->
            Row(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .fillParentMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "${post.id}", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.12f))
                Text(
                    text = post.title,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.68f)
                )
                IconButton(
                    onClick = { navController.navigate("postVer/${post.id}") },
                    modifier = Modifier.weight(0.1f)
                ) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Editar")
                }
                IconButton(
                    onClick = { navController.navigate("postDel/${post.id}") },
                    modifier = Modifier.weight(0.1f)
                ) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Eliminar")
                }
            }
        }
    }
}

// Crear (POST /posts) o editar (PUT /posts/{id})
@Composable
fun ContenidoPostEditar(
    navController: NavHostController,
    servicio: PostApiService,
    listaPosts: SnapshotStateList<PostModel>,
    pid: Int = 0
) {
    val postActual = listaPosts.find { it.id == pid }
    var title by remember { mutableStateOf(postActual?.title ?: "") }
    var body by remember { mutableStateOf(postActual?.body ?: "") }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = if (pid == 0) "nuevo" else pid.toString(),
            onValueChange = { },
            label = { Text("ID (solo lectura)") },
            readOnly = true,
            singleLine = true
        )
        TextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título") },
            singleLine = true
        )
        TextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Contenido") },
            minLines = 3
        )
        if (error.isNotEmpty()) {
            Text(text = error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
        }
        Button(
            onClick = {
                val post = PostModel(pid, postActual?.userId ?: 1, title, body)
                scope.launch {
                    try {
                        if (pid == 0) {
                            val respuesta = servicio.insertPost(post)
                            Log.d("POSTS", "POST posts -> ${respuesta.code()} ${respuesta.body()}")
                            val creado = respuesta.body()
                            if (respuesta.isSuccessful && creado != null) {
                                // JSONPlaceholder siempre devuelve id 101; le damos un id único dentro de la lista
                                val nuevoId = (listaPosts.maxOfOrNull { it.id } ?: 0) + 1
                                listaPosts.add(0, creado.copy(id = nuevoId))
                                navController.popBackStack()
                            } else {
                                error = "La API respondió ${respuesta.code()}"
                            }
                        } else {
                            val respuesta = servicio.updatePost(pid, post)
                            Log.d("POSTS", "PUT posts/$pid -> ${respuesta.code()} ${respuesta.body()}")
                            val actualizado = respuesta.body()
                            if (respuesta.isSuccessful && actualizado != null) {
                                val indice = listaPosts.indexOfFirst { it.id == pid }
                                if (indice >= 0) listaPosts[indice] = actualizado
                                navController.popBackStack()
                            } else {
                                error = "La API respondió ${respuesta.code()}"
                            }
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

// Eliminar: DELETE /posts/{id}
@Composable
fun ContenidoPostEliminar(
    navController: NavHostController,
    servicio: PostApiService,
    listaPosts: SnapshotStateList<PostModel>,
    id: Int
) {
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { navController.popBackStack() },
        title = { Text(text = "Confirmar Eliminación") },
        text = { Text(if (error.isEmpty()) "¿Está seguro de eliminar el post con ID $id?" else error) },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val respuesta = servicio.deletePost(id)
                            Log.d("POSTS", "DELETE posts/$id -> ${respuesta.code()}")
                            if (respuesta.isSuccessful) {
                                listaPosts.removeAll { it.id == id }
                                navController.popBackStack()
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