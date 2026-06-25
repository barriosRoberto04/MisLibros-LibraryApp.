package com.example.mislibros.ui.screens.admin

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mislibros.R
import com.example.mislibros.model.BookModel
import com.example.mislibros.ui.components.PremiumTextField
import com.example.mislibros.ui.components.LibraryScaffold
import com.example.mislibros.ui.components.PremiumDropdownField
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.AmberOrange
import com.example.mislibros.ui.theme.YaleBlue
import com.example.mislibros.viewmodel.AuthViewModel

@Composable
fun AdminRegisterBookScreen(
    bookToEdit: BookModel? = null,
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onRegisterSubmit: (Map<String, String>, Bitmap?, Bitmap?) -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val userProfile = authViewModel.currentUserProfile
    val logoutSuccessText = stringResource(id = R.string.logout_success)

    // CARGA DE TEXTOS DESDE STRINGS.XML
    val titleRegister = stringResource(id = R.string.book_title_register)
    val titleEdit = stringResource(id = R.string.book_title_edit)
    val photoDialogTitle = stringResource(id = R.string.book_photo_title)
    val photoDialogBody = stringResource(id = R.string.book_photo_body)
    val cameraOption = stringResource(id = R.string.camera_option)
    val galleryOption = stringResource(id = R.string.gallery_option)
    val imageLoadError = stringResource(id = R.string.image_load_error)
    val cameraPermissionDenied = stringResource(id = R.string.camera_permission_denied)
    val fillAllFieldsWarning = stringResource(id = R.string.fill_all_fields_warning)
    val errorPhoto = stringResource(id = R.string.book_error_photo)
    val errorCategory = stringResource(id = R.string.book_error_category)
    val errorStock = stringResource(id = R.string.book_error_stock)
    val cancelBtnText = stringResource(id = R.string.cancel_button)

    // ESTADOS DE IMAGEN
    var fotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var authorFotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isPickingAuthorFoto by remember { mutableStateOf(false) }
    var mostrarDialogoOrigen by remember { mutableStateOf(false) }

    val camaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> 
        if (bitmap != null) {
            if (isPickingAuthorFoto) {
                authorFotoBitmap = bitmap
            } else {
                fotoBitmap = bitmap
            }
        }
    }

    val galeriaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                if (isPickingAuthorFoto) {
                    authorFotoBitmap = bitmap
                } else {
                    fotoBitmap = bitmap
                }
            } catch (e: Exception) {
                Toast.makeText(context, imageLoadError, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permisoCamaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { esAceptado ->
        if (esAceptado) camaraLauncher.launch(null)
        else Toast.makeText(context, cameraPermissionDenied, Toast.LENGTH_SHORT).show()
    }

    // ESTADOS DEL FORMULARIO
    val isEditMode = bookToEdit != null
    var title by remember { mutableStateOf(bookToEdit?.title ?: "") }
    var author by remember { mutableStateOf(bookToEdit?.author ?: "") }
    var publisher by remember { mutableStateOf(bookToEdit?.publisher ?: "") }
    var year by remember { mutableStateOf(bookToEdit?.year ?: "") }
    var edition by remember { mutableStateOf(bookToEdit?.edition ?: "") }
    var category by remember { mutableStateOf(bookToEdit?.category ?: "-------") }
    var language by remember { mutableStateOf(bookToEdit?.language ?: "") }
    var pages by remember { mutableStateOf(bookToEdit?.pages ?: "") }
    var stock by remember { mutableStateOf(bookToEdit?.stock ?: "") }
    var review by remember { mutableStateOf(bookToEdit?.review ?: "") }
    var authorBio by remember { mutableStateOf(bookToEdit?.authorBio ?: "") }

    val opcionesCategorias = listOf(
        "-------", "Novela", "Ciencia", "Historia", "Fantasía", "Educación",
        "Ciencia Ficción", "Misterio", "Terror", "Suspenso", "Romántica"
    )

    // Detectar teclado para ocultar barra inferior
    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    // Diálogo de selección de imagen
    if (mostrarDialogoOrigen) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoOrigen = false },
            title = { Text(text = photoDialogTitle) },
            text = { Text(text = photoDialogBody) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoOrigen = false
                    permisoCamaraLauncher.launch(android.Manifest.permission.CAMERA)
                }) { Text(cameraOption) }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoOrigen = false
                    galeriaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(galleryOption) }
            }
        )
    }

    LibraryScaffold(
        authViewModel = authViewModel,
        title = if (isEditMode) titleEdit else titleRegister,
        isAdmin = true,
        onHomeClick = onHomeClick,
        onProfileClick = onProfileClick,
        onLogoutSuccess = onLogoutSuccess,
        onBackClick = onBackClick
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                        // PORTADA DEL LIBRO (DENTRO DE LA CARD)
                        Box(
                            modifier = Modifier
                                .size(width = 110.dp, height = 150.dp)
                                .background(YaleBlue.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
                                .border(2.dp, YaleBlue, shape = RoundedCornerShape(12.dp))
                                .clickable { 
                                    isPickingAuthorFoto = false
                                    mostrarDialogoOrigen = true 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (fotoBitmap != null) {
                                Image(
                                    bitmap = fotoBitmap!!.asImageBitmap(),
                                    contentDescription = stringResource(id = R.string.book_photo_label),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                )
                            } else if (isEditMode && !bookToEdit?.imageUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = bookToEdit!!.imageUrl,
                                    contentDescription = stringResource(id = R.string.book_photo_label),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = YaleBlue, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = stringResource(id = R.string.book_photo_label), fontSize = 11.sp, color = YaleBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        SectionTitle(text = stringResource(id = R.string.book_section_main))

                        PremiumTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = stringResource(id = R.string.book_label_title),
                            icon = Icons.AutoMirrored.Filled.LibraryBooks,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        PremiumTextField(
                            value = author,
                            onValueChange = { author = it },
                            label = stringResource(id = R.string.book_label_author),
                            icon = Icons.Default.Person,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        PremiumTextField(
                            value = publisher,
                            onValueChange = { publisher = it },
                            label = stringResource(id = R.string.book_label_publisher),
                            icon = Icons.Default.Business,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        SectionTitle(text = stringResource(id = R.string.book_section_classification))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumTextField(
                                    value = year,
                                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) year = it },
                                    label = stringResource(id = R.string.book_label_year),
                                    icon = Icons.Default.CalendarToday,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumTextField(
                                    value = edition,
                                    onValueChange = { edition = it },
                                    label = stringResource(id = R.string.book_label_edition),
                                    icon = Icons.Default.Layers,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        PremiumDropdownField(
                            value = category,
                            label = stringResource(id = R.string.book_label_category),
                            icon = Icons.Default.Category,
                            options = opcionesCategorias,
                            onOptionSelected = { category = it }
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        PremiumTextField(
                            value = language,
                            onValueChange = { language = it },
                            label = stringResource(id = R.string.book_label_language),
                            icon = Icons.Default.Language,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumTextField(
                                    value = pages,
                                    onValueChange = { if (it.all { char -> char.isDigit() }) pages = it },
                                    label = stringResource(id = R.string.book_label_pages),
                                    icon = Icons.Default.AutoStories,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumTextField(
                                    value = stock,
                                    onValueChange = { if (it.all { char -> char.isDigit() }) stock = it },
                                    label = stringResource(id = R.string.book_label_stock),
                                    icon = Icons.Default.Inventory,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        SectionTitle(text = "Reseña del Libro")

                        PremiumTextField(
                            value = review,
                            onValueChange = { review = it },
                            label = "Reseña del libro",
                            icon = null,
                            singleLine = false,
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SectionTitle(text = "Información del Autor")

                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(YaleBlue.copy(alpha = 0.1f), shape = CircleShape)
                                .border(2.dp, YaleBlue, shape = CircleShape)
                                .clickable { 
                                    isPickingAuthorFoto = true
                                    mostrarDialogoOrigen = true 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (authorFotoBitmap != null) {
                                Image(
                                    bitmap = authorFotoBitmap!!.asImageBitmap(),
                                    contentDescription = "Foto del Autor",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else if (isEditMode && !bookToEdit?.authorImageUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = bookToEdit!!.authorImageUrl,
                                    contentDescription = "Foto del Autor",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = YaleBlue, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Foto Autor", fontSize = 10.sp, color = YaleBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        PremiumTextField(
                            value = authorBio,
                            onValueChange = { authorBio = it },
                            label = "Bibliografía",
                            icon = null,
                            singleLine = false,
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // BOTÓN REGISTRAR / GUARDAR
                        Button(
                            onClick = {
                                if (title.trim().isEmpty() || author.trim().isEmpty() || publisher.trim().isEmpty() ||
                                    year.trim().isEmpty() || edition.trim().isEmpty() || language.trim().isEmpty() || pages.trim().isEmpty() || stock.trim().isEmpty()) {
                                    Toast.makeText(context, fillAllFieldsWarning, Toast.LENGTH_SHORT).show()
                                } else if (category == "-------") {
                                    Toast.makeText(context, errorCategory, Toast.LENGTH_SHORT).show()
                                } else if ((pages.toIntOrNull() ?: 0) <= 0) {
                                    Toast.makeText(context, "El número de páginas debe ser mayor a 0", Toast.LENGTH_SHORT).show()
                                } else if ((stock.toIntOrNull() ?: 0) <= 0) {
                                    Toast.makeText(context, errorStock, Toast.LENGTH_SHORT).show()
                                } else if (review.trim().isEmpty() || authorBio.trim().isEmpty()) {
                                    Toast.makeText(context, "Por favor, llene la reseña y la bibliografía", Toast.LENGTH_SHORT).show()
                                } else if (!isEditMode && fotoBitmap == null) {
                                    Toast.makeText(context, errorPhoto, Toast.LENGTH_SHORT).show()
                                } else if (!isEditMode && authorFotoBitmap == null) {
                                    Toast.makeText(context, "Por favor, seleccione la foto del autor", Toast.LENGTH_SHORT).show()
                                } else {
                                    val dataMap = mapOf(
                                        "title" to title,
                                        "author" to author,
                                        "publisher" to publisher,
                                        "year" to year,
                                        "edition" to edition,
                                        "category" to category,
                                        "language" to language,
                                        "pages" to pages,
                                        "stock" to stock,
                                        "review" to review,
                                        "authorBio" to authorBio
                                    )
                                    onRegisterSubmit(dataMap, fotoBitmap, authorFotoBitmap)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEditMode) AirForceBlue else YaleBlue
                            ),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(
                                text = if (isEditMode) stringResource(id = R.string.book_button_save) else stringResource(id = R.string.book_button_register),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // BOTÓN CANCELAR
                        Button(
                            onClick = {
                                title = ""
                                author = ""
                                publisher = ""
                                year = ""
                                edition = ""
                                category = "-------"
                                language = ""
                                pages = ""
                                stock = ""
                                review = ""
                                authorBio = ""
                                fotoBitmap = null
                                authorFotoBitmap = null
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(text = cancelBtnText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 16.dp)) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = YaleBlue)
        Spacer(modifier = Modifier.height(2.dp).fillMaxWidth().background(YaleBlue.copy(alpha = 0.1f)))
    }
}