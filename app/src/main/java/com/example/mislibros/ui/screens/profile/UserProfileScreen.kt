package com.example.mislibros.ui.screens.profile

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
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
import com.example.mislibros.model.UserModel
import com.example.mislibros.ui.components.PremiumTextField
import com.example.mislibros.ui.components.PremiumDropdownField
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.AmberOrange
import com.example.mislibros.ui.theme.SunnyYellow
import com.example.mislibros.ui.theme.YaleBlue
import com.example.mislibros.viewmodel.AuthViewModel

@Composable
fun UserProfileScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onSaveProfile: (Map<String, String>, Bitmap?) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val userProfile = authViewModel.currentUserProfile
    val logoutSuccessText = stringResource(id = R.string.logout_success)

    // Si por alguna razón no hay perfil, mostramos un indicador y no cargamos
    if (userProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        }
        return
    }

    // ESTADOS LOCALES DEL PERFIL
    var nombre by remember { mutableStateOf(userProfile.nombre) }
    var apellidos by remember { mutableStateOf(userProfile.apellidos) }
    var telefono by remember { mutableStateOf(userProfile.telefono) }
    var fechaNacimiento by remember { mutableStateOf(userProfile.fechaNacimiento) }
    
    // Dirección
    var calle by remember { mutableStateOf(userProfile.calle) }
    var numExterior by remember { mutableStateOf(userProfile.noExterior) }
    var numInterior by remember { mutableStateOf(userProfile.noInterior) }
    var colonia by remember { mutableStateOf(userProfile.colonia) }
    var alcaldiaMunicipio by remember { mutableStateOf(userProfile.alcaldia) }
    var codigoPostal by remember { mutableStateOf(userProfile.codigoPostal) }

    // Imagen
    var fotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var mostrarDialogoOrigen by remember { mutableStateOf(false) }

    // Launcher de cámara y galería
    val camaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> if (bitmap != null) fotoBitmap = bitmap }

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
                Toast.makeText(context, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permisoCamaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { esAceptado ->
        if (esAceptado) camaraLauncher.launch(null)
        else Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }

    // Determinar color de acento según rol del usuario
    val isAdmin = userProfile.role.equals("ADMIN", ignoreCase = true)
    val accentColor = if (isAdmin) AmberOrange else SunnyYellow
    val topBarColor = YaleBlue

    var isHelpDialogOpen by remember { mutableStateOf(false) }

    // Diálogo de ayuda de perfil
    if (isHelpDialogOpen) {
        AlertDialog(
            onDismissRequest = { isHelpDialogOpen = false },
            title = { Text(text = "Edición de Perfil", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Aquí puedes actualizar tus datos personales y dirección. Pulsa en tu foto de perfil para cambiarla. Haz clic en guardar para sincronizar tus cambios en la nube.") },
            confirmButton = {
                TextButton(onClick = { isHelpDialogOpen = false }) {
                    Text(text = stringResource(id = R.string.close_button), fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Diálogo de foto
    if (mostrarDialogoOrigen) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoOrigen = false },
            title = { Text(text = "Foto de Perfil", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Selecciona el origen para actualizar tu foto de perfil") },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            mostrarDialogoOrigen = false
                            permisoCamaraLauncher.launch(android.Manifest.permission.CAMERA)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YaleBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.PhotoCamera, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Cámara", fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = {
                            mostrarDialogoOrigen = false
                            galeriaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YaleBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.PhotoLibrary, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Galería", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoOrigen = false }) {
                    Text("Cancelar", fontWeight = FontWeight.Bold, color = Color.Red)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(topBarColor)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onBackClick() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.profile_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
            }
        },
        bottomBar = {
            if (!isKeyboardOpen) {
                NavigationBar(containerColor = topBarColor, tonalElevation = 8.dp) {
                    val navBarColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                        indicatorColor = AirForceBlue.copy(alpha = 0.3f)
                    )
                    NavigationBarItem(
                        selected = true,
                        onClick = { },
                        icon = { Icon(Icons.Default.AccountCircle, null) },
                        label = { Text(stringResource(id = R.string.bottom_profile), fontSize = 10.sp) },
                        colors = navBarColors
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { onHomeClick() },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text(stringResource(id = R.string.bottom_home), fontSize = 10.sp) },
                        colors = navBarColors
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { isHelpDialogOpen = true },
                        icon = { Icon(Icons.Default.Info, null) },
                        label = { Text(stringResource(id = R.string.bottom_info), fontSize = 10.sp) },
                        colors = navBarColors
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            authViewModel.signOut()
                            Toast.makeText(context, logoutSuccessText, Toast.LENGTH_SHORT).show()
                            onLogoutSuccess()
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                        label = { Text(stringResource(id = R.string.bottom_logout), fontSize = 10.sp) },
                        colors = navBarColors
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SECCIÓN SUPERIOR: CABECERA Y AVATAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(YaleBlue, YaleBlue.copy(alpha = 0.8f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clickable { mostrarDialogoOrigen = true },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        // Renderizar foto o marcador de posición
                        if (fotoBitmap != null) {
                            Image(
                                bitmap = fotoBitmap!!.asImageBitmap(),
                                contentDescription = "Foto elegida",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                            )
                        } else if (userProfile.imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = userProfile.imageUrl,
                                contentDescription = "Foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .background(AirForceBlue, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (userProfile.nombre.firstOrNull()?.toString() ?: "U").uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 42.sp
                                )
                            }
                        }

                        // Badge de edición flotante
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(accentColor, CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (isAdmin) Color.White else YaleBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${userProfile.nombre} ${userProfile.apellidos}".trim(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isAdmin) "Administrador" else "Normal",
                            color = if (isAdmin) AmberOrange else YaleBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                //  TARJETA 1: DATOS PERSONALES
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, tint = YaleBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.profile_section_personal),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = YaleBlue
                            )
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        PremiumTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = "Nombre",
                            icon = Icons.Default.Badge,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        PremiumTextField(
                            value = apellidos,
                            onValueChange = { apellidos = it },
                            label = "Apellidos",
                            icon = Icons.Default.Badge,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        PremiumTextField(
                            value = telefono,
                            onValueChange = { telefono = it },
                            label = "Teléfono",
                            icon = Icons.Default.Phone,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        PremiumTextField(
                            value = fechaNacimiento,
                            onValueChange = { fechaNacimiento = it },
                            label = stringResource(id = R.string.profile_label_birthdate),
                            icon = Icons.Default.CalendarToday,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                    }
                }

                //  TARJETA 2: DIRECCIÓN
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Home, null, tint = YaleBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.profile_section_address),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = YaleBlue
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        PremiumTextField(
                            value = calle,
                            onValueChange = { calle = it },
                            label = "Calle",
                            icon = Icons.Default.LocationOn,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumTextField(
                                    value = numExterior,
                                    onValueChange = { numExterior = it },
                                    label = "Num. Ext",
                                    icon = Icons.Default.Numbers,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumTextField(
                                    value = numInterior,
                                    onValueChange = { numInterior = it },
                                    label = "Num. Int",
                                    icon = Icons.Default.Numbers,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                                )
                            }
                        }

                        PremiumTextField(
                            value = colonia,
                            onValueChange = { colonia = it },
                            label = "Colonia",
                            icon = Icons.Default.Map,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        PremiumTextField(
                            value = alcaldiaMunicipio,
                            onValueChange = { alcaldiaMunicipio = it },
                            label = "Alcaldía / Municipio",
                            icon = Icons.Default.LocationCity,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        PremiumTextField(
                            value = codigoPostal,
                            onValueChange = { codigoPostal = it },
                            label = "Código Postal",
                            icon = Icons.Default.Pin,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))


                //  SECCIÓN DE ACCIONES (Guardar y Cancelar)

                Button(
                    onClick = {
                        if (nombre.trim().isEmpty() || apellidos.trim().isEmpty() || telefono.trim().isEmpty() ||
                            calle.trim().isEmpty() || numExterior.trim().isEmpty() || colonia.trim().isEmpty() ||
                            alcaldiaMunicipio.trim().isEmpty() || codigoPostal.trim().isEmpty()) {
                            Toast.makeText(context, "Por favor llene todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                        } else {
                            val dataMap = mapOf(
                                "nombre" to nombre,
                                "apellidos" to apellidos,
                                "telefono" to telefono,
                                "fechaNacimiento" to fechaNacimiento,
                                "calle" to calle,
                                "noExterior" to numExterior,
                                "noInterior" to numInterior,
                                "colonia" to colonia,
                                "alcaldia" to alcaldiaMunicipio,
                                "codigoPostal" to codigoPostal
                            )
                            onSaveProfile(dataMap, fotoBitmap)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = YaleBlue),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.profile_button_save),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Button(
                    onClick = {
                        // El botón de cancelar sólo limpia y restablece los valores del formulario
                        nombre = userProfile.nombre
                        apellidos = userProfile.apellidos
                        telefono = userProfile.telefono
                        fechaNacimiento = userProfile.fechaNacimiento
                        calle = userProfile.calle
                        numExterior = userProfile.noExterior
                        numInterior = userProfile.noInterior
                        colonia = userProfile.colonia
                        alcaldiaMunicipio = userProfile.alcaldia
                        codigoPostal = userProfile.codigoPostal
                        fotoBitmap = null
                        Toast.makeText(context, "Edición cancelada. Campos restablecidos.", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel_button),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
