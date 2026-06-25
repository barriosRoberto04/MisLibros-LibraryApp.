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
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import com.example.mislibros.R
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import com.example.mislibros.model.UserModel
import com.example.mislibros.viewmodel.AuthViewModel
import com.example.mislibros.ui.components.PremiumTextField
import com.example.mislibros.ui.components.LibraryScaffold
import com.example.mislibros.ui.components.PremiumDropdownField
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.AmberOrange
import com.example.mislibros.ui.theme.SunnyYellow
import com.example.mislibros.ui.theme.YaleBlue
import java.text.DecimalFormat

@Composable
fun AdminRegisterUserScreen(
    userToEdit: UserModel? = null,
    authViewModel: AuthViewModel,
    onLogoutSuccess: () -> Unit,
    onHomeClick: () -> Unit,
    onBackClick: () -> Unit,
    onRegisterSubmit: (Map<String, String>, Bitmap?) -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val logoutSuccessText = stringResource(id = R.string.logout_success)
    val userProfile = authViewModel.currentUserProfile

    val imageLoadError = stringResource(id = R.string.image_load_error)
    val cameraPermissionDenied = stringResource(id = R.string.camera_permission_denied)
    val addPhotoWarning = stringResource(id = R.string.add_photo_warning)
    val onlyLettersWarningFormat = stringResource(id = R.string.only_letters_warning)
    val fillAllFieldsWarning = stringResource(id = R.string.fill_all_fields_warning)
    val zipCodeLengthWarning = stringResource(id = R.string.zip_code_length_warning)
    val phoneLengthWarning = stringResource(id = R.string.phone_length_warning)
    val invalidEmailWarning = stringResource(id = R.string.invalid_email_warning)
    val passwordLengthWarning = stringResource(id = R.string.password_length_warning)
    val passwordsMismatchWarning = stringResource(id = R.string.passwords_mismatch_warning)

    // Aquí se inicializa la variable de estado donde se guardará la foto temporalmente
    var fotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var mostrarDialogoOrigen by remember { mutableStateOf(false) }

    // Lanzador para capturar foto directamente con la aplicacion de camara (retorna un Bitmap de vista previa)
    val camaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            fotoBitmap = bitmap // Guarda el bitmap de la foto tomada
        }
    }

    // Lanzador para seleccionar una foto de la galeria de fotos (retorna una Uri y se decodifica a Bitmap)
    val galeriaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {

                // Decodifica la Uri en un Bitmap según la versión de Android

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

    // Lanzador para solicitar permisos en tiempo real al usuario (requiere permiso CAMERA antes de abrir la camara)
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

    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    // ESTADOS DEL FORMULARIO
    val isEditMode = userToEdit != null

    var nombre by remember { mutableStateOf(userToEdit?.nombre ?: "") }
    var apellidos by remember { mutableStateOf(userToEdit?.apellidos ?: "") }

    var calle by remember { mutableStateOf(userToEdit?.calle ?: "") }
    var numExterior by remember { mutableStateOf(userToEdit?.noExterior ?: "") }
    var numInterior by remember { mutableStateOf(userToEdit?.noInterior ?: "") }
    var colonia by remember { mutableStateOf(userToEdit?.colonia ?: "") }
    var alcaldiaMunicipio by remember { mutableStateOf(userToEdit?.alcaldia ?: "") }
    var codigoPostal by remember { mutableStateOf(userToEdit?.codigoPostal ?: "") }

    var email by remember { mutableStateOf(userToEdit?.email ?: "") }
    var telefono by remember { mutableStateOf(userToEdit?.telefono ?: "") }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Dropdown de Rol / Tipo de Usuario
    val initialTipoUsuario = if (isEditMode) {
        if (userToEdit?.role.equals("ADMIN", ignoreCase = true)) "Administrador" else "Público"
    } else {
        "-------"
    }
    var tipoUsuario by remember { mutableStateOf(initialTipoUsuario) }
    val opcionesRol = listOf("-------", "Administrador", "Público")

    // Función auxiliar para filtrar solo letras y lanzar Toast si hay números
    val filterOnlyLetters: (String, String) -> String = { input, fieldName ->
        if (input.all { it.isLetter() || it.isWhitespace() }) {
            input
        } else {
            Toast.makeText(context, onlyLettersWarningFormat.format(fieldName), Toast.LENGTH_SHORT).show()
            input.filter { it.isLetter() || it.isWhitespace() }
        }
    }

    LibraryScaffold(
        authViewModel = authViewModel,
        title = if (isEditMode) "Editar Usuario" else "Registrar Usuario",
        isAdmin = true,
        onHomeClick = onHomeClick,
        onProfileClick = onProfileClick,
        onLogoutSuccess = onLogoutSuccess,
        onBackClick = onBackClick
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
                        // SECCIÓN: FOTOGRAFÍA (CÍRCULO PREMIUM)
                        Box(
                            modifier = Modifier
                                .size(100.dp)
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
                                        .size(100.dp)
                                        .clip(CircleShape)
                                )
                            } else if (isEditMode && !userToEdit?.imageUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = userToEdit!!.imageUrl,
                                    contentDescription = stringResource(id = R.string.photo_label),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = stringResource(id = R.string.photo_label),
                                        tint = YaleBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(id = R.string.photo_label),
                                        fontSize = 11.sp,
                                        color = YaleBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // SECCIÓN: ROL / TIPO DE USUARIO
                        SectionTitle(text = "Tipo de Usuario")
                        
                        PremiumDropdownField(
                            value = tipoUsuario,
                            label = "Seleccione Tipo de Usuario",
                            icon = Icons.Default.Group,
                            options = opcionesRol,
                            onOptionSelected = { tipoUsuario = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

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
                                    value = numExterior,
                                    onValueChange = { if (it.all { char -> char.isDigit() }) numExterior = it },
                                    label = stringResource(id = R.string.num_exterior_label),
                                    icon = Icons.Default.Numbers,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumTextField(
                                    value = numInterior,
                                    onValueChange = { if (it.all { char -> char.isDigit() }) numInterior = it },
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
                            icon = Icons.Default.Map,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        PremiumTextField(
                            value = alcaldiaMunicipio,
                            onValueChange = { alcaldiaMunicipio = filterOnlyLetters(it, "Alcaldía o Municipio") },
                            label = stringResource(id = R.string.municipality_label),
                            icon = Icons.Default.LocationCity,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        PremiumTextField(
                            value = codigoPostal,
                            onValueChange = { if (it.length <= 5 && it.all { char -> char.isDigit() }) codigoPostal = it },
                            label = stringResource(id = R.string.zip_code_label),
                            icon = Icons.Default.Numbers,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECCIÓN: INFORMACIÓN DE CONTACTO

                        SectionTitle(text = stringResource(id = R.string.contact_info_section))

                        PremiumTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = stringResource(id = R.string.email_label),
                            icon = Icons.Default.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
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
                            icon = Icons.Default.PhoneAndroid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECCIÓN: SEGURIDAD
                        val isGoogleUser = userToEdit != null && userToEdit.imageUrl.contains("googleusercontent.com")
                        if (!isGoogleUser) {
                            SectionTitle(text = if (isEditMode) "Cambiar Contraseña (Llenar sólo para cambiar)" else stringResource(id = R.string.security_section))

                            PremiumTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = if (isEditMode) "Nueva Contraseña" else stringResource(id = R.string.password_label),
                                icon = Icons.Default.Lock,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                hideText = !isPasswordVisible,
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = YaleBlue
                                        )
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            PremiumTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = if (isEditMode) "Confirmar Nueva Contraseña" else stringResource(id = R.string.confirm_password_label),
                                icon = Icons.Default.Lock,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                hideText = !isConfirmPasswordVisible,
                                trailingIcon = {
                                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = YaleBlue
                                        )
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(30.dp))
                        }

                        // SECCIÓN: ACCIONES
                        Button(
                            onClick = {
                                val telefonoLimpio = telefono.replace(" ", "")

                                // Validar primero rol
                                if (tipoUsuario == "-------") {
                                    Toast.makeText(context, "Por favor, seleccione un tipo de usuario válido", Toast.LENGTH_SHORT).show()
                                }
                                // Validar foto
                                else if (!isEditMode && fotoBitmap == null) {
                                    Toast.makeText(context, addPhotoWarning, Toast.LENGTH_SHORT).show()
                                }
                                // Validar campos vacíos
                                else if (nombre.trim().isEmpty() ||
                                    apellidos.trim().isEmpty() ||
                                    calle.trim().isEmpty() ||
                                    numExterior.trim().isEmpty() ||
                                    numInterior.trim().isEmpty() ||
                                    colonia.trim().isEmpty() ||
                                    alcaldiaMunicipio.trim().isEmpty() ||
                                    codigoPostal.trim().isEmpty() ||
                                    email.trim().isEmpty() ||
                                    telefonoLimpio.trim().isEmpty() ||
                                    (!isEditMode && (password.trim().isEmpty() || confirmPassword.trim().isEmpty()))
                                ) {
                                    Toast.makeText(context, fillAllFieldsWarning, Toast.LENGTH_SHORT).show()
                                }
                                // Validaciones de formato
                                else if (codigoPostal.length < 5) {
                                    Toast.makeText(context, zipCodeLengthWarning, Toast.LENGTH_SHORT).show()
                                } else if (telefonoLimpio.length != 10) {
                                    Toast.makeText(context, phoneLengthWarning, Toast.LENGTH_SHORT).show()
                                } else if (!correoValido(email)) {
                                    Toast.makeText(context, invalidEmailWarning, Toast.LENGTH_SHORT).show()
                                } else if (!isEditMode && !passwordValida(password)) {
                                    Toast.makeText(context, passwordLengthWarning, Toast.LENGTH_SHORT).show()
                                } else if (!isEditMode && password != confirmPassword) {
                                    Toast.makeText(context, passwordsMismatchWarning, Toast.LENGTH_SHORT).show()
                                } else if (isEditMode && password.isNotEmpty() && !passwordValida(password)) {
                                    Toast.makeText(context, passwordLengthWarning, Toast.LENGTH_SHORT).show()
                                } else if (isEditMode && password.isNotEmpty() && password != confirmPassword) {
                                    Toast.makeText(context, passwordsMismatchWarning, Toast.LENGTH_SHORT).show()
                                } else {
                                    // Asignar rol según selección
                                    val dbRole = if (tipoUsuario == "Administrador") "ADMIN" else "USER"
                                    val dataMap = mutableMapOf(
                                        "nombre" to nombre,
                                        "apellidos" to apellidos,
                                        "calle" to calle,
                                        "numExterior" to numExterior,
                                        "numInterior" to numInterior,
                                        "colonia" to colonia,
                                        "alcaldiaMunicipio" to alcaldiaMunicipio,
                                        "codigoPostal" to codigoPostal,
                                        "email" to email,
                                        "telefono" to telefonoLimpio,
                                        "role" to dbRole
                                    )
                                    if (!isEditMode || password.isNotEmpty()) {
                                        dataMap["password"] = password
                                    }
                                    onRegisterSubmit(dataMap, fotoBitmap)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEditMode) AirForceBlue else Color(0xFF4CAF50),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = if (isEditMode) "GUARDAR" else "REGISTRAR USUARIO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                nombre = ""
                                apellidos = ""
                                calle = ""
                                numExterior = ""
                                numInterior = ""
                                colonia = ""
                                alcaldiaMunicipio = ""
                                codigoPostal = ""
                                email = ""
                                telefono = ""
                                password = ""
                                confirmPassword = ""
                                tipoUsuario = "-------"
                                fotoBitmap = null
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
                                text = stringResource(id = R.string.cancel_button),
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

private fun formatoTelefono(numero: String): String {
    return try {
        if (numero.length == 10) {
            DecimalFormat("00 0000 0000").format(numero.toLong())
        } else {
            numero
        }
    } catch (e: Exception) {
        numero
    }
}

private fun correoValido(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS
        .matcher(email)
        .matches()
}

private fun passwordValida(password: String): Boolean {
    return password.length >= 8
}
