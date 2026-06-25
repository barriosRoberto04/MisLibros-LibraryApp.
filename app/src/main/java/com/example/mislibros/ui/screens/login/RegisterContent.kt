package com.example.mislibros.ui.screens.login

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.mislibros.R
import com.example.mislibros.ui.components.PremiumTextField
import java.text.DecimalFormat
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image

@Composable
fun RegisterContent(
    onRegisterClick: (Map<String, String>) -> Unit,
    onCancelClick: () -> Unit,
    fotoBitmap: android.graphics.Bitmap?,
    onFotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val onlyLettersWarningFormat = stringResource(id = R.string.only_letters_warning)
    val addPhotoWarning = stringResource(id = R.string.add_photo_warning)
    val fillAllFieldsWarning = stringResource(id = R.string.fill_all_fields_warning)
    val zipCodeLengthWarning = stringResource(id = R.string.zip_code_length_warning)
    val phoneLengthWarning = stringResource(id = R.string.phone_length_warning)
    val invalidEmailWarning = stringResource(id = R.string.invalid_email_warning)
    val passwordLengthWarning = stringResource(id = R.string.password_length_warning)
    val passwordsMismatchWarning = stringResource(id = R.string.passwords_mismatch_warning)

    // --- ESTADOS DEL FORMULARIO ---
    var nombre by remember { mutableStateOf("") }
    var apellidoPaterno by remember { mutableStateOf("") }
    var apellidoMaterno by remember { mutableStateOf("") }

    var calle by remember { mutableStateOf("") }
    var numExterior by remember { mutableStateOf("") }
    var numInterior by remember { mutableStateOf("") }
    var colonia by remember { mutableStateOf("") }
    var alcaldiaMunicipio by remember { mutableStateOf("") }
    var codigoPostal by remember { mutableStateOf("") }

    var email by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Función auxiliar para filtrar solo letras y lanzar Toast si hay números
    val filterOnlyLetters: (String, String) -> String = { input, fieldName ->
        if (input.all { it.isLetter() || it.isWhitespace() }) {
            input
        } else {
            Toast.makeText(context, onlyLettersWarningFormat.format(fieldName), Toast.LENGTH_SHORT).show()
            input.filter { it.isLetter() || it.isWhitespace() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Encabezado
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.create_account_title),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    //  SECCIÓN: FOTOGRAFÍA (CÍRCULO PREMIUM)
                    // SECCIÓN: FOTOGRAFÍA (CÍRCULO)
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape)
                            .clickable { onFotoClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (fotoBitmap != null) {
                            //  Se corrigieron los prefijos para usar las importaciones limpias de arriba
                            Image(
                                bitmap = fotoBitmap.asImageBitmap(),
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
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = stringResource(id = R.string.photo_label), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    //  SECCIÓN: DATOS PERSONALES

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
                        value = apellidoPaterno,
                        onValueChange = { apellidoPaterno = filterOnlyLetters(it, "Apellido Paterno") },
                        label = stringResource(id = R.string.last_name_paternal_label),
                        icon = Icons.Default.Badge,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    PremiumTextField(
                        value = apellidoMaterno,
                        onValueChange = { apellidoMaterno = filterOnlyLetters(it, "Apellido Materno") },
                        label = stringResource(id = R.string.last_name_maternal_label),
                        icon = Icons.Default.Badge,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    //  SECCIÓN: DIRECCIÓN
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

                    //  SECCIÓN: INFORMACIÓN DE CONTACTO
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


                    //  SECCIÓN: SEGURIDAD
                    SectionTitle(text = stringResource(id = R.string.security_section))

                    PremiumTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(id = R.string.password_label),
                        icon = Icons.Default.Lock,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        hideText = !isPasswordVisible,
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    PremiumTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = stringResource(id = R.string.confirm_password_label),
                        icon = Icons.Default.Lock,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        hideText = !isConfirmPasswordVisible,
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(imageVector = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    // SECCIÓN: ACCIONES

                    Button(
                        onClick = {
                            val telefonoLimpio = telefono.replace(" ", "")

                            //  REGLA DE ORO: Validar primero que NINGÚN campo esté vacío
                            if (fotoBitmap == null) {
                                Toast.makeText(context, addPhotoWarning, Toast.LENGTH_SHORT).show()
                            } else if (nombre.trim().isEmpty() ||
                                apellidoPaterno.trim().isEmpty() ||
                                apellidoMaterno.trim().isEmpty() ||
                                calle.trim().isEmpty() ||
                                numExterior.trim().isEmpty() ||
                                numInterior.trim().isEmpty() ||
                                colonia.trim().isEmpty() ||
                                alcaldiaMunicipio.trim().isEmpty() ||
                                codigoPostal.trim().isEmpty() ||
                                email.trim().isEmpty() ||
                                telefonoLimpio.trim().isEmpty() ||
                                password.trim().isEmpty() ||
                                confirmPassword.trim().isEmpty()
                            ) {
                                Toast.makeText(context, fillAllFieldsWarning, Toast.LENGTH_SHORT).show()
                            }
                            // Luego procedemos con las validaciones de formato específicas
                            else if (codigoPostal.length < 5) {
                                Toast.makeText(context, zipCodeLengthWarning, Toast.LENGTH_SHORT).show()
                            } else if (telefonoLimpio.length != 10) {
                                Toast.makeText(context, phoneLengthWarning, Toast.LENGTH_SHORT).show()
                            } else if (!correoValido(email)) {
                                Toast.makeText(context, invalidEmailWarning, Toast.LENGTH_SHORT).show()
                            } else if (!passwordValida(password)) {
                                Toast.makeText(context, passwordLengthWarning, Toast.LENGTH_SHORT).show()
                            } else if (password != confirmPassword) {
                                Toast.makeText(context, passwordsMismatchWarning, Toast.LENGTH_SHORT).show()
                            } else {
                                // Si todo está perfecto, se genera el mapa
                                val dataMap = mapOf(
                                    "nombre" to nombre,
                                    "apellidoPaterno" to apellidoPaterno,
                                    "apellidoMaterno" to apellidoMaterno,
                                    "calle" to calle,
                                    "numExterior" to numExterior,
                                    "numInterior" to numInterior,
                                    "colonia" to colonia,
                                    "alcaldiaMunicipio" to alcaldiaMunicipio,
                                    "codigoPostal" to codigoPostal,
                                    "email" to email,
                                    "telefono" to telefonoLimpio,
                                    "password" to password
                                )
                                onRegisterClick(dataMap)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(text = stringResource(id = R.string.register_button), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            //  1. LIMPIAMOS TODOS LOS CAMPOS DE TEXTO
                            nombre = ""
                            apellidoPaterno = ""
                            apellidoMaterno = ""
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

                            //  2. EJECUTAMOS LA ACCIÓN DE SALIR (Regresar al Login)
                            onCancelClick()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(text = stringResource(id = R.string.cancel_button), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 16.dp)) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        )
    }
}

fun formatoTelefono(numero: String): String {
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

fun correoValido(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS
        .matcher(email)
        .matches()
}

fun passwordValida(password: String): Boolean {
    return password.length >= 8
}