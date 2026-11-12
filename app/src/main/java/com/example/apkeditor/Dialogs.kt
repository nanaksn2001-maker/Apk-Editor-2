package com.example.apkeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DeleteCheckedDialog(
    fileCount: Int,
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus yang dipilih?") },
        text = {
            Text(
                "$fileCount file dari $itemCount item yang dipilih akan dihapus " +
                    "dari APK saat disimpan. Kamu masih bisa memulihkannya sebelum menyimpan."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Hapus", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun ActionsDialog(
    name: String,
    isDeleted: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onReplace: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
        title = { Text(name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 16.sp) },
        text = {
            Column {
                if (!isDeleted) {
                    TextButton(onClick = onEdit) { Text("Edit sebagai teks") }
                    TextButton(onClick = onReplace) { Text("Ganti file") }
                    TextButton(onClick = onExport) { Text("Ekstrak") }
                    TextButton(onClick = onDelete) { Text("Hapus") }
                }
                TextButton(onClick = onRestore) { Text("Batalkan perubahan file ini") }
            }
        },
    )
}

@Composable
fun AddFileDialog(
    initialPath: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var path by remember(initialPath) { mutableStateOf(initialPath) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Letakkan di dalam APK") },
        text = {
            OutlinedTextField(
                value = path,
                onValueChange = { path = it },
                label = { Text("Path file di APK") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(path) }) { Text("Tambah") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun ReplaceAllDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var find by remember { mutableStateOf("") }
    var repl by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cari & ganti di semua file teks") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = find, onValueChange = { find = it }, label = { Text("Cari") })
                OutlinedTextField(value = repl, onValueChange = { repl = it }, label = { Text("Ganti dengan") })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(find, repl) }) { Text("Ganti semua") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
