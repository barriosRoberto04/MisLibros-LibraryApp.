package com.example.mislibros.ui.screens.login

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.mislibros.R

@Composable
fun RegisterScreen(
    onBackToLoginClick: () -> Unit,
    onRegisterSubmit: (Map<String, String>, Bitmap) -> Unit
) {
    val context = LocalContext.current
    val imageLoadError = stringResource(id = R.string.image_load_error)
    val cameraPermissionDenied = stringResource(id = R.string.camera_permission_denied)
    val addPhotoWarning = stringResource(id = R.string.add_photo_warning)
    var fotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var mostrarDialogoOrigen by remember { mutableStateOf(false) }

    //  Lanzador para tomar foto con la cámara
    val camaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            fotoBitmap = bitmap
        }
    }

    // ️ Lanzador para seleccionar foto desde la galería
    val galeriaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                fotoBitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
            } catch (e: Exception) {
                Toast.makeText(context, imageLoadError, Toast.LENGTH_SHORT).show()
            }
        }
    }

    //  Lanzador para solicitar permiso de la cámara
    val permisoCamaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { esAceptado ->
        if (esAceptado) {
            camaraLauncher.launch()
        } else {
            Toast.makeText(context, cameraPermissionDenied, Toast.LENGTH_SHORT).show()
        }
    }

    // Cuadro de diálogo para elegir origen de la foto
    if (mostrarDialogoOrigen) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoOrigen = false },
            title = { Text(text = stringResource(id = R.string.select_photo_title)) },
            text = { Text(text = stringResource(id = R.string.select_photo_body)) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoOrigen = false
                    permisoCamaraLauncher.launch(android.Manifest.permission.CAMERA)
                }) {
                    Text(stringResource(id = R.string.camera_option))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoOrigen = false
                    galeriaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text(stringResource(id = R.string.gallery_option))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) { innerPadding ->
        RegisterContent(
            onRegisterClick = { dataMap ->
                val bitmap = fotoBitmap
                if (bitmap != null) {
                    onRegisterSubmit(dataMap, bitmap)
                } else {
                    Toast.makeText(context, addPhotoWarning, Toast.LENGTH_SHORT).show()
                }
            },
            onCancelClick = { onBackToLoginClick() },
            fotoBitmap = fotoBitmap,
            onFotoClick = { mostrarDialogoOrigen = true }, //  Activa el selector
            modifier = Modifier.padding(innerPadding)
        )
    }
}