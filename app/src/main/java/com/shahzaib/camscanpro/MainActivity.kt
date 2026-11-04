package com.shahzaib.camscanpro

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val pages = mutableStateListOf<Uri>()
    private var latestPdf: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ScannerScreen() }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    @Composable
    private fun ScannerScreen() {
        var busy by remember { mutableStateOf(false) }
        var extractedText by remember { mutableStateOf("") }

        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val uri = saveBitmapToCache(bitmap)
                pages.add(uri)
            }
        }
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            pages.addAll(uris)
        }

        MaterialTheme {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Cam Scan Pro", fontSize = 22.sp) },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Your documents, scanned simply", style = MaterialTheme.typography.titleMedium)
                        Text("Add pages, create a PDF, or extract text with OCR.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.AddAPhoto, null); Spacer(Modifier.width(6.dp)); Text("Scan")
                            }
                            OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(6.dp)); Text("Import")
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${pages.size} page(s) selected", style = MaterialTheme.typography.labelLarge)
                    }
                    itemsIndexed(pages) { index, uri ->
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                                val bitmap = remember(uri) { decodeBitmap(uri) }
                                if (bitmap != null) {
                                    Image(bitmap.asImageBitmap(), null, modifier = Modifier.size(76.dp, 90.dp))
                                } else {
                                    Icon(Icons.Default.Description, null, modifier = Modifier.size(76.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Page ${index + 1}", style = MaterialTheme.typography.titleMedium)
                                    Text("Ready to include in PDF", style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = { pages.remove(uri) }) { Text("Remove") }
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                if (pages.isEmpty()) toast("Please scan or import at least one page.")
                                else {
                                    busy = true
                                    try {
                                        latestPdf = createPdf()
                                        toast("PDF saved in Downloads/CamScanPro")
                                    } catch (e: Exception) { toast("PDF could not be created: ${e.message}") }
                                    busy = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null); Spacer(Modifier.width(8.dp)); Text("Create PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                if (pages.isEmpty()) toast("Please add a page first.")
                                else {
                                    busy = true
                                    val first = decodeBitmap(pages.first())
                                    if (first == null) { busy = false; toast("Could not read image.") }
                                    else {
                                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                        recognizer.process(InputImage.fromBitmap(first, 0))
                                            .addOnSuccessListener { result ->
                                                extractedText = result.text.ifBlank { "No text detected." }
                                                busy = false
                                            }
                                            .addOnFailureListener { e -> busy = false; toast("OCR failed: ${e.message}") }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Description, null); Spacer(Modifier.width(8.dp)); Text("Extract text (OCR)")
                        }
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (extractedText.isNotBlank()) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Extracted text", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(6.dp))
                                    Text(extractedText)
                                    TextButton(onClick = {
                                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, extractedText)
                                        startActivity(Intent.createChooser(send, "Share extracted text"))
                                    }) { Icon(Icons.Default.Share, null); Text("Share text") }
                                }
                            }
                        }
                        OutlinedButton(onClick = { pages.clear(); extractedText = ""; latestPdf = null }, modifier = Modifier.fillMaxWidth()) {
                            Text("Clear all pages")
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Privacy: images are processed on this device. OCR uses Google ML Kit.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val file = File(cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        return Uri.fromFile(file)
    }

    private fun decodeBitmap(uri: Uri): Bitmap? = try {
        if (uri.scheme == "file") BitmapFactory.decodeFile(uri.path)
        else contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) { null }

    private fun createPdf(): File {
        val document = PdfDocument()
        pages.forEachIndexed { index, uri ->
            val bitmap = decodeBitmap(uri) ?: return@forEachIndexed
            val maxW = 595
            val maxH = 842
            val scale = minOf(maxW.toFloat() / bitmap.width, maxH.toFloat() / bitmap.height)
            val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
            val pageInfo = PdfDocument.PageInfo.Builder(maxW, maxH, index + 1).create()
            val page = document.startPage(pageInfo)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val left = (maxW - width) / 2f
            val top = (maxH - height) / 2f
            page.canvas.drawColor(Color.WHITE)
            page.canvas.drawBitmap(Bitmap.createScaledBitmap(bitmap, width, height, true), left, top, paint)
            document.finishPage(page)
        }
        val dir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "CamScanPro")
        if (!dir.exists()) dir.mkdirs()
        val name = "CamScanPro_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        val file = File(dir, name)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        // Also publish a copy to Downloads where available.
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CamScanPro")
            }
            val outUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (outUri != null) contentResolver.openOutputStream(outUri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
        }
        return file
    }
}
