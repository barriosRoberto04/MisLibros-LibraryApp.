package com.example.mislibros.ui.screens.user

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mislibros.R
import com.example.mislibros.model.UserModel
import com.example.mislibros.ui.components.LibraryScaffold
import com.example.mislibros.ui.components.PremiumTextField
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.YaleBlue
import com.example.mislibros.viewmodel.AuthViewModel

@Composable
fun UserProfileScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit = {},
    onLogoutSuccess: () -> Unit = {},
    isMandatory: Boolean = false
) {
    val context = LocalContext.current
    val userProfile = authViewModel.currentUserProfile
    val isLoading = authViewModel.isLoading
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    // Si isMandatory es true, se muestra una alerta indicando que el perfil esta incompleto.
    LaunchedEffect(isMandatory) {
        if (isMandatory) {
            Toast.makeText(context, "Llena todos los campos para acceder al menú principal", Toast.LENGTH_LONG).show()
        }
    }

    // CARGA DE TEXTOS DESDE STRINGS.XML
    val imageLoadError = stringResource(id = R.string.image_load_error)
    val cameraPermissionDenied = stringResource(id = R.string.camera_permission_denied)
    val onlyLettersWarningFormat = stringResource(id = R.string.only_letters_warning)
    val fillAllFieldsWarning = stringResource(id = R.string.fill_all_fields_warning)
    val zipCodeLengthWarning = stringResource(id = R.string.zip_code_length_warning)
    val phoneLengthWarning = stringResource(id = R.string.phone_length_warning)

    // ESTADOS DE IMAGEN Y DIÁLOGO
    var fotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var mostrarDialogoOrigen by remember { mutableStateOf(false) }

    // Lanzador para tomar foto con la cámara
    val camaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            fotoBitmap = bitmap
        }
    }

    // Lanzador para seleccionar foto desde la galería
    val galeriaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                fotoBitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
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

    // Lanzador para solicitar permiso de la cámara
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

    // ESTADOS DE CAMPOS
    var nombre by remember { mutableStateOf(userProfile?.nombre ?: "") }
    var apellidos by remember { mutableStateOf(userProfile?.apellidos ?: "") }
    var telefono by remember { mutableStateOf(formatoTelefono(userProfile?.telefono ?: "")) }
    var calle by remember { mutableStateOf(userProfile?.calle ?: "") }
    var noInterior by remember { mutableStateOf(userProfile?.noInterior ?: "") }
    var noExterior by remember { mutableStateOf(userProfile?.noExterior ?: "") }
    var colonia by remember { mutableStateOf(userProfile?.colonia ?: "") }
    var alcaldia by remember { mutableStateOf(userProfile?.alcaldia ?: "") }
    var codigoPostal by remember { mutableStateOf(userProfile?.codigoPostal ?: "") }

    // Función auxiliar para filtrar solo letras y lanzar Toast si hay números
    val filterOnlyLetters: (String, String) -> String = { input, fieldName ->
        if (input.all { it.isLetter() || it.isWhitespace() }) {
            input
        } else {
            Toast.makeText(context, onlyLettersWarningFormat.format(fieldName), Toast.LENGTH_SHORT).show()
            input.filter { it.isLetter() || it.isWhitespace() }
        }
    }

    // Envolvemos la pantalla en LibraryScaffold. Si el perfil es obligatorio (incompleto),
    // pasamos null a onHomeClick/onBackClick y false a showBottomBar para desactivar la navegacion.
    LibraryScaffold(
        authViewModel = authViewModel,
        title = "Mi Perfil",
        isAdmin = userProfile?.role == "ADMIN",
        onHomeClick = if (isMandatory) null else onHomeClick,
        onProfileClick = { /* ya estamos en perfil */ },
        onLogoutSuccess = onLogoutSuccess,
        onBackClick = if (isMandatory) null else onBackClick,
        showBottomBar = !isMandatory
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
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
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // SECCIÓN: FOTOGRAFÍA EDITABLE (CÍRCULO MÁS GRANDE ~ 130dp)
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .background(YaleBlue.copy(alpha = 0.1f), shape = CircleShape)
                                .clickable { mostrarDialogoOrigen = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (fotoBitmap != null) {
                                Image(
                                    bitmap = fotoBitmap!!.asImageBitmap(),
                                    contentDescription = stringResource(id = R.string.photo_label),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(130.dp)
                                        .clip(CircleShape)
                                )
                            } else if (!userProfile?.imageUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = userProfile!!.imageUrl,
                                    contentDescription = stringResource(id = R.string.photo_label),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(130.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = stringResource(id = R.string.photo_label),
                                        tint = YaleBlue,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(id = R.string.photo_label),
                                        fontSize = 12.sp,
                                        color = YaleBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // SECCIÓN: DATOS PERSONALES
                        SectionTitle(text = stringResource(id = R.string.personal_data_section))

                        PremiumTextField(
                            value = nombre,
                            onValueChange = { nombre = filterOnlyLetters(it, "Nombre") },
                            label = stringResource(id = R.string.first_name_label),
                            icon = Icons.Default.Person,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        PremiumTextField(
                            value = apellidos,
                            onValueChange = { apellidos = filterOnlyLetters(it, "Apellidos") },
                            label = "Apellidos",
                            icon = Icons.Default.Badge,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        PremiumTextField(
                            value = telefono,
                            onValueChange = {
                                val numeros = it.filter { c -> c.isDigit() }
                                if (numeros.length <= 10) {
                                    telefono = if (numeros.length == 10) {
                                        formatoTelefono(numeros)
                                    } else {
                                        numeros
                                    }
                                }
                            },
                            label = stringResource(id = R.string.phone_label),
                            icon = Icons.Default.Phone,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECCIÓN: DIRECCIÓN
                        SectionTitle(text = stringResource(id = R.string.address_section))

                        PremiumTextField(
                            value = calle,
                            onValueChange = { calle = it },
                            label = stringResource(id = R.string.street_label),
                            icon = Icons.Default.Home,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumTextField(
                                    value = noExterior,
                                    onValueChange = { if (it.all { char -> char.isDigit() }) noExterior = it },
                                    label = stringResource(id = R.string.num_exterior_label),
                                    icon = Icons.Default.Numbers,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumTextField(
                                    value = noInterior,
                                    onValueChange = { if (it.all { char -> char.isDigit() }) noInterior = it },
                                    label = stringResource(id = R.string.num_interior_label),
                                    icon = Icons.Default.Numbers,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        PremiumTextField(
                            value = colonia,
                            onValueChange = { colonia = filterOnlyLetters(it, "Colonia") },
                            label = stringResource(id = R.string.neighborhood_label),
                            icon = Icons.Default.LocationCity,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        PremiumTextField(
                            value = alcaldia,
                            onValueChange = { alcaldia = filterOnlyLetters(it, "Alcaldía") },
                            label = stringResource(id = R.string.municipality_label),
                            icon = Icons.Default.Map,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        PremiumTextField(
                            value = codigoPostal,
                            onValueChange = { if (it.length <= 5 && it.all { char -> char.isDigit() }) codigoPostal = it },
                            label = stringResource(id = R.string.zip_code_label),
                            icon = Icons.Default.Pin,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // BOTONES DE ACCIÓN (CANCELAR Y GUARDAR)
                        // BOTONES: GUARDAR / CANCELAR
                        //

                        // GUARDAR
                        Button(
                            onClick = {
                                val telefonoLimpio = telefono.replace(" ", "")

                                // Validaciones básicas
                                if (nombre.isBlank() || apellidos.isBlank() ||
                                    telefonoLimpio.isBlank() || calle.isBlank() || noExterior.isBlank() ||
                                    colonia.isBlank() || alcaldia.isBlank() || codigoPostal.isBlank()
                                ) {
                                    Toast.makeText(context, fillAllFieldsWarning, Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                if (codigoPostal.length != 5) {
                                    Toast.makeText(context, zipCodeLengthWarning, Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                if (telefonoLimpio.length != 10) {
                                    Toast.makeText(context, phoneLengthWarning, Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val updatedUser = userProfile?.copy(
                                    nombre = nombre.trim(),
                                    apellidos = apellidos.trim(),
                                    telefono = telefonoLimpio.trim(),
                                    calle = calle.trim(),
                                    noExterior = noExterior.trim(),
                                    noInterior = noInterior.trim(),
                                    colonia = colonia.trim(),
                                    alcaldia = alcaldia.trim(),
                                    codigoPostal = codigoPostal.trim()
                                ) ?: return@Button

                                authViewModel.updateProfile(updatedUser, context, newPhoto = fotoBitmap) {
                                    // Si era obligatorio, navegar al home después de guardar
                                    onHomeClick()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AirForceBlue,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = !isLoading
                        ) {
                            Text(
                                text = "GUARDAR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Si el perfil es obligatorio (isMandatory = true), no permitimos cancelar el guardado.
                        // En su lugar, el boton "Cancelar" se convierte en "Cerrar Sesion" para salir de la app de forma segura.
                        Button(
                            onClick = {
                                if (isMandatory) {
                                    authViewModel.signOut()
                                    onLogoutSuccess()
                                } else {
                                    nombre = userProfile?.nombre ?: ""
                                    apellidos = userProfile?.apellidos ?: ""
                                    telefono = userProfile?.telefono ?: ""
                                    calle = userProfile?.calle ?: ""
                                    noExterior = userProfile?.noExterior ?: ""
                                    noInterior = userProfile?.noInterior ?: ""
                                    colonia = userProfile?.colonia ?: ""
                                    alcaldia = userProfile?.alcaldia ?: ""
                                    codigoPostal = userProfile?.codigoPostal ?: ""
                                    fotoBitmap = null
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53935),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = if (isMandatory) "CERRAR SESIÓN" else stringResource(id = R.string.cancel_button),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, top = 16.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = YaleBlue
        )
        Spacer(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(YaleBlue.copy(alpha = 0.1f))
        )
    }
}

fun formatoTelefono(numero: String): String {
    return try {
        if (numero.length == 10) {
            java.text.DecimalFormat("00 0000 0000").format(numero.toLong())
        } else {
            numero
        }
    } catch (e: Exception) {
        numero
    }
}
