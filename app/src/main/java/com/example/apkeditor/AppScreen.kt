package com.example.apkeditor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private class FolderAgg {
    var count = 0
    var size = 0L
    var deleted = 0
    var changed = false
}

private data class Entry(
    val key: String,
    val label: String,
    val isFolder: Boolean,
    val size: Long,
    val count: Int,
    val status: Status?,
    val changed: Boolean = false,
    val allDeleted: Boolean = false,
)

private fun parentOf(path: String): String =
    path.removeSuffix("/").substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun App(vm: EditorViewModel = viewModel()) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    val openApk = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.open(uri)
    }
    val replacePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = vm.selected
        if (uri != null && target != null) vm.replaceWith(target, uri)
    }
    val addPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.stageAdd(uri)
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val target = vm.selected
        if (uri != null && target != null) vm.exportTo(target, uri)
    }
    val savePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
        if (uri != null) vm.buildTo(uri)
    }

    var query by remember { mutableStateOf("") }
    var showReplaceAll by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var showInstalled by remember { mutableStateOf(false) }
    var currentPath by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(setOf<String>()) }
    var showDeleteChecked by remember { mutableStateOf(false) }

    LaunchedEffect(vm.apkName) {
        currentPath = ""
        checked = emptySet()
        query = ""
    }

    if (showInstalled) {
        InstalledAppPicker(
            onPick = { pkg -> showInstalled = false; vm.openInstalled(pkg) },
            onDismiss = { showInstalled = false },
        )
    }

    BackHandler(enabled = vm.editingName != null) { vm.cancelEdit() }
    BackHandler(enabled = vm.editingName == null && vm.apkName != null) { vm.closeApk() }
    BackHandler(
        enabled = vm.editingName == null && vm.apkName != null &&
            (checked.isNotEmpty() || (currentPath.isNotEmpty() && query.isBlank()))
    ) {
        if (checked.isNotEmpty()) checked = emptySet() else currentPath = parentOf(currentPath)
    }

    val editing = vm.editingName
    if (editing != null) {
        TextEditorScreen(vm, editing, snackbar)
        return
    }

    if (vm.apkName == null) {
        HomeScreen(
            snackbar = snackbar,
            busy = vm.busy,
            onPickApk = { openApk.launch(arrayOf("*/*")) },
            onPickInstalled = { showInstalled = true },
        )
        return
    }

    val allRows = vm.rows("")
    val rows = vm.rows(query)

    val entries: List<Entry> = if (query.isNotBlank()) {
        rows.map { Entry(it.name, it.name, false, it.size, 1, it.status) }
    } else {
        val folderMap = linkedMapOf<String, FolderAgg>()
        val files = mutableListOf<Entry>()
        for (r in rows) {
            if (!r.name.startsWith(currentPath)) continue
            val rest = r.name.substring(currentPath.length)
            val slash = rest.indexOf('/')
            if (slash >= 0) {
                val key = currentPath + rest.substring(0, slash + 1)
                val agg = folderMap.getOrPut(key) { FolderAgg() }
                agg.count++
                agg.size += r.size
                if (r.status == Status.DELETED) agg.deleted++
                if (r.status != Status.ORIGINAL) agg.changed = true
            } else {
                files += Entry(r.name, rest, false, r.size, 1, r.status)
            }
        }
        val folders = folderMap.map { (k, a) ->
            Entry(
                key = k,
                label = k.removeSuffix("/").substringAfterLast('/'),
                isFolder = true,
                size = a.size,
                count = a.count,
                status = null,
                changed = a.changed,
                allDeleted = a.deleted == a.count,
            )
        }.sortedBy { it.label.lowercase() }
        folders + files.sortedBy { it.label.lowercase() }
    }

    fun namesFor(keys: Set<String>, accept: (Status) -> Boolean): List<String> =
        allRows.filter { r ->
            accept(r.status) &&
                keys.any { k -> if (k.endsWith("/")) r.name.startsWith(k) else r.name == k }
        }.map { it.name }

    val selecting = checked.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.list_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        )
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (selecting) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        title = { Text("${checked.size} dipilih") },
                        navigationIcon = {
                            TextButton(onClick = { checked = emptySet() }) { Text("Batal") }
                        },
                    )
                } else {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        title = {
                            Text(vm.apkName ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        navigationIcon = { TextButton(onClick = { vm.closeApk() }) { Text("Tutup") } },
                        actions = {
                            TextButton(onClick = { addPicker.launch(arrayOf("*/*")) }) { Text("Tambah") }
                            TextButton(onClick = { showReplaceAll = true }) { Text("Ganti teks") }
                            TextButton(
                                onClick = { savePicker.launch("edited.apk") },
                                enabled = !vm.busy,
                            ) { Text("Simpan") }
                        },
                    )
                }
            },
            bottomBar = {
                if (selecting) {
                    BottomAppBar(containerColor = Color.Black.copy(alpha = 0.6f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { checked = entries.map { it.key }.toSet() }) {
                                Text("Pilih semua")
                            }
                            TextButton(onClick = {
                                namesFor(checked) { it != Status.ORIGINAL }.forEach { vm.restore(it) }
                                checked = emptySet()
                            }) { Text("Pulihkan") }
                            TextButton(
                                onClick = { showDeleteChecked = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                            ) { Text("Hapus") }
                        }
                    }
                }
            },
        ) { pad ->
            Column(modifier = Modifier.fillMaxSize().padding(pad)) {
                if (vm.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Cari file") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Text(
                    "${vm.changeCount} perubahan menunggu disimpan",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
                Text(
                    if (query.isNotBlank()) "Hasil pencarian" else "📂 /" + currentPath,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
                HorizontalDivider()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (currentPath.isNotEmpty() && query.isBlank()) {
                        item(key = "__up__") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentPath = parentOf(currentPath) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Text("⬆  .. (naik satu folder)")
                            }
                            HorizontalDivider()
                        }
                    }
                    items(entries, key = { it.key }) { e ->
                        val isChecked = e.key in checked
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isChecked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .combinedClickable(
                                    onClick = {
                                        when {
                                            selecting -> {
                                                checked = if (isChecked) checked - e.key else checked + e.key
                                            }
                                            e.isFolder -> {
                                                currentPath = e.key
                                            }
                                            else -> {
                                                vm.selected = e.key
                                                showActions = true
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        checked = if (isChecked) checked - e.key else checked + e.key
                                    },
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            if (selecting) {
                                Checkbox(checked = isChecked, onCheckedChange = null)
                                Spacer(Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    (if (e.isFolder) "📁 " else "") + e.label,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val subtitle: String
                                val color: Color
                                if (e.isFolder) {
                                    subtitle = "${e.count} file • ${formatSize(e.size)}" +
                                        when {
                                            e.allDeleted -> " • akan dihapus"
                                            e.changed -> " • ada perubahan"
                                            else -> ""
                                        }
                                    color = when {
                                        e.allDeleted -> MaterialTheme.colorScheme.error
                                        e.changed -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                } else {
                                    val label = when (e.status) {
                                        Status.MODIFIED -> " • diubah"
                                        Status.NEW -> " • baru"
                                        Status.DELETED -> " • akan dihapus"
                                        else -> ""
                                    }
                                    subtitle = formatSize(e.size) + label
                                    color = when (e.status) {
                                        Status.DELETED -> MaterialTheme.colorScheme.error
                                        Status.ORIGINAL -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                }
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = color,
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showDeleteChecked) {
        val targets = namesFor(checked) { it != Status.DELETED }
        DeleteCheckedDialog(
            fileCount = targets.size,
            itemCount = checked.size,
            onConfirm = {
                targets.forEach { vm.delete(it) }
                checked = emptySet()
                showDeleteChecked = false
            },
            onDismiss = { showDeleteChecked = false },
        )
    }

    val sel = vm.selected
    if (showActions && sel != null) {
        val isDeleted = vm.rows("").firstOrNull { it.name == sel }?.status == Status.DELETED
        ActionsDialog(
            name = sel,
            isDeleted = isDeleted,
            onDismiss = { showActions = false },
            onEdit = { showActions = false; vm.startEdit(sel) },
            onReplace = { showActions = false; replacePicker.launch(arrayOf("*/*")) },
            onExport = { showActions = false; exportPicker.launch(sel.substringAfterLast('/')) },
            onDelete = { showActions = false; vm.delete(sel) },
            onRestore = { showActions = false; vm.restore(sel) },
        )
    }

    val draft = vm.addDraft
    if (draft != null) {
        AddFileDialog(
            initialPath = draft.second,
            onConfirm = { path -> vm.confirmAdd(path) },
            onDismiss = { vm.addDraft = null },
        )
    }

    if (showReplaceAll) {
        ReplaceAllDialog(
            onConfirm = { find, repl ->
                showReplaceAll = false
                vm.replaceAll(find, repl)
            },
            onDismiss = { showReplaceAll = false },
        )
    }
}
