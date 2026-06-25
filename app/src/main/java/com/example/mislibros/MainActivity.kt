package com.example.mislibros

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.graphics.Bitmap
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.mislibros.model.UserModel
import com.example.mislibros.ui.navigation.AppScreen
import com.example.mislibros.ui.screens.login.LoginScreen
import com.example.mislibros.ui.screens.login.RegisterScreen
import com.example.mislibros.ui.screens.admin.AdminHomeScreen
import com.example.mislibros.ui.screens.admin.AdminRegisterUserScreen
import com.example.mislibros.ui.screens.admin.AdminQueryUsersScreen
import com.example.mislibros.ui.screens.admin.AdminRegisterBookScreen
import com.example.mislibros.ui.screens.admin.AdminQueryBooksScreen
import com.example.mislibros.model.BookModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.example.mislibros.ui.screens.user.UserHomeScreen
import com.example.mislibros.ui.screens.user.UserQueryBooksScreen
import com.example.mislibros.ui.screens.user.UserProfileScreen
import com.example.mislibros.ui.screens.user.BookLoanScreen
import com.example.mislibros.ui.screens.user.QueryLoansScreen
import com.example.mislibros.ui.screens.user.NotificationsScreen
import com.example.mislibros.ui.screens.user.SubmitReportScreen
import com.example.mislibros.ui.screens.user.QueryReportsScreen
import com.example.mislibros.ui.screens.user.BookDetailScreen
import com.example.mislibros.model.AuthorModel
import com.example.mislibros.ui.screens.user.UserQueryAuthorsScreen
import com.example.mislibros.ui.screens.user.AuthorDetailScreen
import com.example.mislibros.ui.screens.user.InfoScreen
import com.example.mislibros.ui.theme.MisLibrosTheme
import com.example.mislibros.viewmodel.AuthViewModel
import com.example.mislibros.ui.navigation.LocalNavController
import androidx.compose.runtime.CompositionLocalProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        // Inicializamos Firebase Auth
        auth = FirebaseAuth.getInstance()

        setContent {
            // Envolvemos en tu tema para mantener tus colores premium originales
            MisLibrosTheme {
                val navController = rememberNavController()
                var isRegistering by remember { mutableStateOf(false) }
                var userToEdit by remember { mutableStateOf<UserModel?>(null) }
                var bookToEdit by remember { mutableStateOf<BookModel?>(null) }
                var bookToView by remember { mutableStateOf<BookModel?>(null) }
                var authorToView by remember { mutableStateOf<AuthorModel?>(null) }
                // Estado para forzar completar el perfil (p.ej. usuarios nuevos de Google)
                var isMandatoryProfile by remember { mutableStateOf(false) }

                CompositionLocalProvider(LocalNavController provides navController) {
                    Box(modifier = Modifier.fillMaxSize()) {
                    //  El NavHost controla cuál pantalla se ve primero (Sin transiciones/parpadeos)
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                    ) {
                        //  PANTALLA SPLASH (Para validar sesión y redirigir sin destellos)
                        composable("splash") {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {}

                            LaunchedEffect(Unit) {
                                authViewModel.checkActiveSession { role ->
                                    if (role != null) {
                                        // Si el usuario existe pero tiene datos pendientes (por ejemplo, recien registrado con Google),
                                        // activamos isMandatoryProfile para forzar la pantalla de perfil e impedir que use la app.
                                        val profileComplete = authViewModel.isProfileComplete()
                                        if (!profileComplete) {
                                            isMandatoryProfile = true
                                            navController.navigate(AppScreen.UserProfile.route) {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                        } else if (role.equals("ADMIN", ignoreCase = true)) {
                                            // Redirigir al panel de administrador segun el rol
                                            navController.navigate(AppScreen.HomeAdmin.route) {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                        } else {
                                            // Redirigir al panel de usuario normal
                                            navController.navigate(AppScreen.HomeUser.route) {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                        }
                                    } else {
                                        navController.navigate(AppScreen.Login.route) {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                }
                            }
                        }
                        //  PANTALLA DE LOGIN (Si ya tiene cuenta, ingresa aquí)
                        composable(AppScreen.Login.route) {
                            LoginScreen(
                                onLoginClick = { email, password ->
                                    authViewModel.login(email, password, this@MainActivity) { role ->
                                        Toast.makeText(this@MainActivity, "Bienvenido", Toast.LENGTH_SHORT).show()
                                        // Evalua el rol obtenido para decidir a donde navegar
                                        if (role.equals("ADMIN", ignoreCase = true)) {
                                            navController.navigate(AppScreen.HomeAdmin.route) {
                                                popUpTo(AppScreen.Login.route) { inclusive = true }
                                            }
                                        } else {
                                            navController.navigate(AppScreen.HomeUser.route) {
                                                popUpTo(AppScreen.Login.route) { inclusive = true }
                                            }
                                        }
                                    }
                                },
                                onGoogleLoginClick = { idToken ->
                                    authViewModel.loginWithGoogle(idToken, this@MainActivity) { role ->
                                        val profileComplete = authViewModel.isProfileComplete()
                                        if (!profileComplete) {
                                            // Si es un usuario nuevo de Google y no tiene direccion ni telefono,
                                            // se bloquea el acceso activando isMandatoryProfile y mandandolo al perfil.
                                            Toast.makeText(this@MainActivity, "¡Bienvenido! Llena tu perfil para continuar.", Toast.LENGTH_SHORT).show()
                                            isMandatoryProfile = true
                                            navController.navigate(AppScreen.UserProfile.route) {
                                                popUpTo(AppScreen.Login.route) { inclusive = true }
                                            }
                                        } else {
                                            Toast.makeText(this@MainActivity, "Bienvenido", Toast.LENGTH_SHORT).show()
                                            if (role.equals("ADMIN", ignoreCase = true)) {
                                                navController.navigate(AppScreen.HomeAdmin.route) {
                                                    popUpTo(AppScreen.Login.route) { inclusive = true }
                                                }
                                            } else {
                                                navController.navigate(AppScreen.HomeUser.route) {
                                                    popUpTo(AppScreen.Login.route) { inclusive = true }
                                                }
                                            }
                                        }
                                    }
                                },
                                onRegisterClick = {
                                    // Si no tiene cuenta, este botón lo lleva al Formulario de Registro
                                    navController.navigate(AppScreen.Register.route)
                                }
                            )
                        }

                        //  PANTALLA DE REGISTRO (Para crear una cuenta nueva)
                        composable(AppScreen.Register.route) {
                            RegisterScreen(
                                onBackToLoginClick = {
                                    // Si decide no registrarse, regresa al Login original
                                    navController.popBackStack()
                                },
                                onRegisterSubmit = { dataMap, bitmap ->
                                    val email = dataMap["email"] ?: ""
                                    val password = dataMap["password"] ?: ""

                                    if (email.isNotBlank() && password.isNotBlank()) {
                                        isRegistering = true
                                        auth.createUserWithEmailAndPassword(email, password)
                                            .addOnCompleteListener(this@MainActivity) { task ->
                                                if (task.isSuccessful) {
                                                    val userId = auth.currentUser?.uid
                                                    if (userId != null) {
                                                        // 1. Subir la imagen a Firebase Storage
                                                        val storageRef = FirebaseStorage.getInstance().reference
                                                            .child("profile_photos/${userId}.jpg")
                                                        
                                                        val baos = ByteArrayOutputStream()
                                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                                                        val data = baos.toByteArray()

                                                        storageRef.putBytes(data)
                                                            .addOnSuccessListener {
                                                                // 2. Obtener URL de descarga
                                                                storageRef.downloadUrl.addOnSuccessListener { uri ->
                                                                    val imageUrl = uri.toString()

                                                                    // 3. Crear UserModel y guardarlo en Realtime Database
                                                                    val db = FirebaseDatabase.getInstance()
                                                                    val roleAsignado = if (email.contains("roberto", ignoreCase = true) || email.contains("admin", ignoreCase = true) || email.contains("barrios", ignoreCase = true)) "ADMIN" else "USER"
                                                                    val userModel = UserModel(
                                                                        userId = userId,
                                                                        email = email,
                                                                        username = email.split("@").firstOrNull() ?: "",
                                                                        role = roleAsignado,
                                                                        status = "ACTIVE",
                                                                        imageUrl = imageUrl,
                                                                        nombre = dataMap["nombre"] ?: "",
                                                                        apellidos = "${dataMap["apellidoPaterno"] ?: ""} ${dataMap["apellidoMaterno"] ?: ""}".trim(),
                                                                        telefono = dataMap["telefono"] ?: "",
                                                                        calle = dataMap["calle"] ?: "",
                                                                        noInterior = dataMap["numInterior"] ?: "",
                                                                        noExterior = dataMap["numExterior"] ?: "",
                                                                        colonia = dataMap["colonia"] ?: "",
                                                                        alcaldia = dataMap["alcaldiaMunicipio"] ?: "",
                                                                        codigoPostal = dataMap["codigoPostal"] ?: "",
                                                                        fechaRegistro = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                                                                    )

                                                                    db.getReference("usuarios").child(userId)
                                                                        .setValue(userModel)
                                                                        .addOnSuccessListener {
                                                                            isRegistering = false
                                                                            Toast.makeText(this@MainActivity, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show()
                                                                            navController.popBackStack()
                                                                        }
                                                                        .addOnFailureListener { e ->
                                                                            isRegistering = false
                                                                            Toast.makeText(this@MainActivity, "Error al guardar perfil: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                                        }
                                                                }.addOnFailureListener { e ->
                                                                    isRegistering = false
                                                                    Toast.makeText(this@MainActivity, "Error al obtener link de imagen: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                                }
                                                            }
                                                            .addOnFailureListener { e ->
                                                                isRegistering = false
                                                                Toast.makeText(this@MainActivity, "Error al subir imagen: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                            }
                                                    } else {
                                                        isRegistering = false
                                                    }
                                                } else {
                                                    isRegistering = false
                                                    Toast.makeText(this@MainActivity, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                    } else {
                                        Toast.makeText(this@MainActivity, "Correo y contraseña no pueden estar vacíos", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        //  PANTALLA PRINCIPAL DEL ADMINISTRADOR (HomeAdmin)
                        composable(AppScreen.HomeAdmin.route) {
                            AdminHomeScreen(
                                authViewModel = authViewModel,
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                    }
                                },
                                onHomeClick = {
                                    // Ya estamos en la pantalla principal del Administrador.
                                },
                                onRegisterUserClick = {
                                    userToEdit = null
                                    navController.navigate(AppScreen.AdminRegisterUser.route)
                                },
                                onQueryUsersClick = {
                                    navController.navigate(AppScreen.AdminQueryUsers.route)
                                },
                                onRegisterBookClick = {
                                    bookToEdit = null
                                    navController.navigate(AppScreen.AdminRegisterBook.route)
                                },
                                onQueryBooksClick = {
                                    navController.navigate(AppScreen.AdminQueryBooks.route)
                                },
                                onRegisterLoanClick = {
                                    navController.navigate(AppScreen.BookLoan.route)
                                },
                                onQueryLoansClick = {
                                    navController.navigate(AppScreen.QueryLoans.route)
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                },
                                onNotificationsClick = {
                                    navController.navigate(AppScreen.Notifications.route)
                                },
                                onSubmitReportClick = {
                                    navController.navigate(AppScreen.SubmitReport.route)
                                },
                                onQueryReportsClick = {
                                    navController.navigate(AppScreen.QueryReports.route)
                                }
                            )
                        }

                        //  CONSULTA DE LIBROS (AdminQueryBooks)
                        composable(AppScreen.AdminQueryBooks.route) {
                            AdminQueryBooksScreen(
                                authViewModel = authViewModel,
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                    }
                                },
                                onHomeClick = {
                                    navController.navigate(AppScreen.HomeAdmin.route) {
                                        popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                    }
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onEditBookClick = { book ->
                                    bookToEdit = book
                                    navController.navigate(AppScreen.AdminRegisterBook.route)
                                },
                                onBookClick = { book ->
                                    bookToView = book
                                    navController.navigate(AppScreen.BookDetail.route)
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                }
                            )
                        }

                        //  CONSULTA DE USUARIOS (AdminQueryUsers)
                        composable(AppScreen.AdminQueryUsers.route) {
                            AdminQueryUsersScreen(
                                authViewModel = authViewModel,
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                    }
                                },
                                onHomeClick = {
                                    navController.navigate(AppScreen.HomeAdmin.route) {
                                        popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                    }
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onEditUserClick = { user ->
                                    userToEdit = user
                                    navController.navigate(AppScreen.AdminRegisterUser.route)
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                }
                            )
                        }

                        //  REGISTRO / EDICIÓN DE USUARIOS POR EL ADMINISTRADOR (AdminRegisterUser)
                        composable(AppScreen.AdminRegisterUser.route) {
                            AdminRegisterUserScreen(
                                userToEdit = userToEdit,
                                authViewModel = authViewModel,
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                    }
                                },
                                onHomeClick = {
                                    navController.navigate(AppScreen.HomeAdmin.route) {
                                        popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                    }
                                },
                                onBackClick = {
                                    userToEdit = null
                                    navController.popBackStack()
                                },
                                onRegisterSubmit = { dataMap, bitmap ->
                                    val email = dataMap["email"] ?: ""
                                    val roleSelected = dataMap["role"] ?: "USER"
                                    val newPassword = dataMap["password"] ?: ""

                                    if (userToEdit != null) {
                                        // --- MODO EDICIÓN DE USUARIO ---
                                        isRegistering = true
                                        val userId = userToEdit!!.userId

                                        val saveToDb: (String) -> Unit = { finalImageUrl ->
                                            val db = FirebaseDatabase.getInstance()
                                            val updatedUser = UserModel(
                                                userId = userId,
                                                email = email,
                                                username = email.split("@").firstOrNull() ?: "",
                                                role = roleSelected,
                                                status = userToEdit!!.status,
                                                imageUrl = finalImageUrl,
                                                nombre = dataMap["nombre"] ?: "",
                                                apellidos = dataMap["apellidos"] ?: "",
                                                telefono = dataMap["telefono"] ?: "",
                                                calle = dataMap["calle"] ?: "",
                                                noInterior = dataMap["numInterior"] ?: "",
                                                noExterior = dataMap["numExterior"] ?: "",
                                                colonia = dataMap["colonia"] ?: "",
                                                alcaldia = dataMap["alcaldiaMunicipio"] ?: "",
                                                codigoPostal = dataMap["codigoPostal"] ?: "",
                                                fechaRegistro = userToEdit!!.fechaRegistro,
                                                fechaNacimiento = userToEdit!!.fechaNacimiento,
                                                password = if (newPassword.isNotBlank()) newPassword else userToEdit!!.password
                                            )

                                            db.getReference("usuarios").child(userId)
                                                .setValue(updatedUser)
                                                .addOnSuccessListener {
                                                    isRegistering = false
                                                    userToEdit = null
                                                    Toast.makeText(this@MainActivity, "Usuario actualizado con éxito", Toast.LENGTH_SHORT).show()
                                                    navController.popBackStack()
                                                }
                                                .addOnFailureListener { e ->
                                                    isRegistering = false
                                                    Toast.makeText(this@MainActivity, "Error al actualizar perfil: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                }
                                        }

                                        val onImageUploadFinished: (String) -> Unit = { finalImageUrl ->
                                            if (newPassword.isNotBlank()) {
                                                val oldPassword = userToEdit!!.password
                                                if (oldPassword.isNotBlank()) {
                                                    val mainOptions = FirebaseApp.getInstance().options
                                                    val tempAppName = "TempAdminEditApp_" + System.currentTimeMillis()
                                                    try {
                                                        val tempApp = FirebaseApp.initializeApp(
                                                            this@MainActivity,
                                                            FirebaseOptions.Builder()
                                                                .setApiKey(mainOptions.apiKey!!)
                                                                .setApplicationId(mainOptions.applicationId!!)
                                                                .setDatabaseUrl(mainOptions.databaseUrl)
                                                                .setProjectId(mainOptions.projectId)
                                                                .build(),
                                                            tempAppName
                                                        )
                                                        val tempAuth = FirebaseAuth.getInstance(tempApp)
                                                        tempAuth.signInWithEmailAndPassword(userToEdit!!.email, oldPassword)
                                                            .addOnCompleteListener { task ->
                                                                if (task.isSuccessful) {
                                                                    tempAuth.currentUser?.updatePassword(newPassword)
                                                                        ?.addOnCompleteListener { updateTask ->
                                                                            tempAuth.signOut()
                                                                            tempApp.delete()
                                                                            if (updateTask.isSuccessful) {
                                                                                saveToDb(finalImageUrl)
                                                                            } else {
                                                                                isRegistering = false
                                                                                Toast.makeText(this@MainActivity, "Error al actualizar contraseña en Auth: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                                                                            }
                                                                        }
                                                                } else {
                                                                    tempApp.delete()
                                                                    isRegistering = false
                                                                    Toast.makeText(this@MainActivity, "Error al autenticar usuario en Auth: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                                                }
                                                            }
                                                    } catch (e: Exception) {
                                                        isRegistering = false
                                                        Toast.makeText(this@MainActivity, "Error de Firebase temporal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                    }
                                                } else {
                                                    Toast.makeText(this@MainActivity, "Guardando contraseña sólo en base de datos (usuario sin contraseña previa)", Toast.LENGTH_SHORT).show()
                                                    saveToDb(finalImageUrl)
                                                }
                                            } else {
                                                saveToDb(finalImageUrl)
                                            }
                                        }

                                        if (bitmap != null) {
                                            // Subir nueva foto a Firebase Storage
                                            val storageRef = FirebaseStorage.getInstance().reference
                                                .child("profile_photos/${userId}.jpg")
                                            
                                            val baos = ByteArrayOutputStream()
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                                            val data = baos.toByteArray()

                                            storageRef.putBytes(data)
                                                .addOnSuccessListener {
                                                     storageRef.downloadUrl.addOnSuccessListener { uri ->
                                                         onImageUploadFinished(uri.toString())
                                                     }.addOnFailureListener { e ->
                                                        isRegistering = false
                                                        Toast.makeText(this@MainActivity, "Error al obtener URL de foto: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    isRegistering = false
                                                    Toast.makeText(this@MainActivity, "Error al subir foto: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                }
                                        } else {
                                            // Conservar foto actual
                                            onImageUploadFinished(userToEdit!!.imageUrl)
                                        }
                                    } else {
                                        // --- MODO NUEVO REGISTRO ---
                                        val password = dataMap["password"] ?: ""
                                        if (email.isNotBlank() && password.isNotBlank() && bitmap != null) {
                                            isRegistering = true

                                            // Inicializar temporalmente otra FirebaseApp con las mismas credenciales
                                            val mainOptions = FirebaseApp.getInstance().options
                                            val tempAppName = "TempAdminRegisterApp_" + System.currentTimeMillis()
                                            
                                            try {
                                                val tempApp = FirebaseApp.initializeApp(
                                                    this@MainActivity,
                                                    FirebaseOptions.Builder()
                                                        .setApiKey(mainOptions.apiKey!!)
                                                        .setApplicationId(mainOptions.applicationId!!)
                                                        .setDatabaseUrl(mainOptions.databaseUrl)
                                                        .setProjectId(mainOptions.projectId)
                                                        .build(),
                                                    tempAppName
                                                )
                                                val tempAuth = FirebaseAuth.getInstance(tempApp)

                                                tempAuth.createUserWithEmailAndPassword(email, password)
                                                    .addOnCompleteListener(this@MainActivity) { task ->
                                                        if (task.isSuccessful) {
                                                            val userId = task.result?.user?.uid
                                                            if (userId != null) {
                                                                // 1. Subir la imagen a Firebase Storage
                                                                val storageRef = FirebaseStorage.getInstance().reference
                                                                    .child("profile_photos/${userId}.jpg")
                                                                
                                                                val baos = ByteArrayOutputStream()
                                                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                                                                val data = baos.toByteArray()

                                                                storageRef.putBytes(data)
                                                                    .addOnSuccessListener {
                                                                        // 2. Obtener URL de descarga
                                                                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                                                                            val imageUrl = uri.toString()

                                                                            // 3. Guardar UserModel en Realtime Database
                                                                            val db = FirebaseDatabase.getInstance()
                                                                            val userModel = UserModel(
                                                                                userId = userId,
                                                                                email = email,
                                                                                username = email.split("@").firstOrNull() ?: "",
                                                                                role = roleSelected,
                                                                                status = "ACTIVE",
                                                                                imageUrl = imageUrl,
                                                                                nombre = dataMap["nombre"] ?: "",
                                                                                apellidos = dataMap["apellidos"] ?: "",
                                                                                telefono = dataMap["telefono"] ?: "",
                                                                                calle = dataMap["calle"] ?: "",
                                                                                noInterior = dataMap["numInterior"] ?: "",
                                                                                noExterior = dataMap["numExterior"] ?: "",
                                                                                colonia = dataMap["colonia"] ?: "",
                                                                                alcaldia = dataMap["alcaldiaMunicipio"] ?: "",
                                                                                codigoPostal = dataMap["codigoPostal"] ?: "",
                                                                                fechaRegistro = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
                                                                                password = password
                                                                            )

                                                                            db.getReference("usuarios").child(userId)
                                                                                .setValue(userModel)
                                                                                .addOnSuccessListener {
                                                                                    tempAuth.signOut()
                                                                                    tempApp.delete()
                                                                                    isRegistering = false
                                                                                    userToEdit = null
                                                                                    Toast.makeText(this@MainActivity, "¡Usuario registrado con éxito!", Toast.LENGTH_SHORT).show()
                                                                                    navController.popBackStack()
                                                                                }
                                                                                .addOnFailureListener { e ->
                                                                                    tempApp.delete()
                                                                                    isRegistering = false
                                                                                    Toast.makeText(this@MainActivity, "Error al guardar perfil: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                                                }
                                                                        }.addOnFailureListener { e ->
                                                                            tempApp.delete()
                                                                            isRegistering = false
                                                                            Toast.makeText(this@MainActivity, "Error al obtener link de imagen: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                                        }
                                                                    }
                                                                    .addOnFailureListener { e ->
                                                                        tempApp.delete()
                                                                        isRegistering = false
                                                                        Toast.makeText(this@MainActivity, "Error al subir imagen: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                                    }
                                                            } else {
                                                                tempApp.delete()
                                                                isRegistering = false
                                                            }
                                                        } else {
                                                            tempApp.delete()
                                                            isRegistering = false
                                                            Toast.makeText(this@MainActivity, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                            } catch (e: Exception) {
                                                isRegistering = false
                                                Toast.makeText(this@MainActivity, "Error al inicializar registro: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(this@MainActivity, "Por favor llene el correo, contraseña y seleccione una foto", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                }
                            )
                        }

                        //  REGISTRO / EDICIÓN DE LIBROS POR EL ADMINISTRADOR (AdminRegisterBook)
                        composable(AppScreen.AdminRegisterBook.route) {
                            AdminRegisterBookScreen(
                                bookToEdit = bookToEdit,
                                authViewModel = authViewModel,
                                onHomeClick = {
                                    navController.navigate(AppScreen.HomeAdmin.route) {
                                        popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                    }
                                },
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                    }
                                },
                                onBackClick = {
                                    bookToEdit = null
                                    navController.popBackStack()
                                },
                                onRegisterSubmit = { dataMap, bitmap, authorBitmap ->
                                    val bookId = bookToEdit?.bookId ?: FirebaseDatabase.getInstance().reference.push().key ?: ""
                                    val isEditMode = bookToEdit != null
                                    isRegistering = true

                                    val saveToDb: (String, String) -> Unit = { finalImageUrl, finalAuthorImageUrl ->
                                        val db = FirebaseDatabase.getInstance()
                                        val bookModel = com.example.mislibros.model.BookModel(
                                            bookId = bookId,
                                            title = dataMap["title"] ?: "",
                                            author = dataMap["author"] ?: "",
                                            publisher = dataMap["publisher"] ?: "",
                                            year = dataMap["year"] ?: "",
                                            edition = dataMap["edition"] ?: "",
                                            category = dataMap["category"] ?: "",
                                            language = dataMap["language"] ?: "",
                                            pages = dataMap["pages"] ?: "",
                                            stock = dataMap["stock"] ?: "1",
                                            review = dataMap["review"] ?: "",
                                            authorBio = dataMap["authorBio"] ?: "",
                                            imageUrl = finalImageUrl,
                                            authorImageUrl = finalAuthorImageUrl,
                                            status = bookToEdit?.status ?: "AVAILABLE",
                                            dateRegistered = bookToEdit?.dateRegistered
                                                ?: SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                                        )

                                        db.getReference("libros").child(bookId)
                                            .setValue(bookModel)
                                            .addOnSuccessListener {
                                                isRegistering = false
                                                bookToEdit = null
                                                val msg = if (isEditMode) "¡Libro actualizado con éxito!" else "¡Libro registrado con éxito!"
                                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            }
                                            .addOnFailureListener { e ->
                                                isRegistering = false
                                                Toast.makeText(this@MainActivity, "Error al guardar libro: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                    }

                                    val uploadAuthorPhoto: (String) -> Unit = { coverUrl ->
                                        if (authorBitmap != null) {
                                            val storageRef = FirebaseStorage.getInstance().reference
                                                .child("author_photos/${bookId}.jpg")

                                            val baos = ByteArrayOutputStream()
                                            authorBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                                            val data = baos.toByteArray()

                                            storageRef.putBytes(data)
                                                .addOnSuccessListener {
                                                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                                                        saveToDb(coverUrl, uri.toString())
                                                    }.addOnFailureListener { e ->
                                                        isRegistering = false
                                                        Toast.makeText(this@MainActivity, "Error al obtener URL del autor: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    isRegistering = false
                                                    Toast.makeText(this@MainActivity, "Error al subir foto del autor: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                }
                                        } else {
                                            val authorUrl = bookToEdit?.authorImageUrl ?: ""
                                            saveToDb(coverUrl, authorUrl)
                                        }
                                    }

                                    if (bitmap != null) {
                                        val storageRef = FirebaseStorage.getInstance().reference
                                            .child("book_covers/${bookId}.jpg")

                                        val baos = ByteArrayOutputStream()
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                                        val data = baos.toByteArray()

                                        storageRef.putBytes(data)
                                            .addOnSuccessListener {
                                                storageRef.downloadUrl.addOnSuccessListener { uri ->
                                                    uploadAuthorPhoto(uri.toString())
                                                }.addOnFailureListener { e ->
                                                    isRegistering = false
                                                    Toast.makeText(this@MainActivity, "Error al obtener URL de portada: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                isRegistering = false
                                                Toast.makeText(this@MainActivity, "Error al subir portada: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                    } else {
                                        val coverUrl = bookToEdit?.imageUrl ?: ""
                                        uploadAuthorPhoto(coverUrl)
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                }
                            )
                        }

//  PANTALLA PRINCIPAL DEL USUARIO (HomeUser)
                        composable(AppScreen.HomeUser.route) {
                            UserHomeScreen(
                                authViewModel = authViewModel,
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(AppScreen.HomeUser.route) { inclusive = true }
                                    }
                                },
                                onHomeClick = {
                                    // Ya estamos en la pantalla principal del Usuario.
                                },
                                onSearchBooksClick = {
                                    navController.navigate(AppScreen.UserQueryBooks.route)
                                },
                                onSearchAuthorClick = {
                                    navController.navigate(AppScreen.UserQueryAuthors.route)
                                },
                                onRequestBookClick = {
                                    navController.navigate(AppScreen.BookLoan.route)
                                },
                                onMyLoansClick = {
                                    navController.navigate(AppScreen.QueryLoans.route)
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                },
                                onNotificationsClick = {
                                    navController.navigate(AppScreen.Notifications.route)
                                },
                                onSubmitReportClick = {
                                    navController.navigate(AppScreen.SubmitReport.route)
                                },
                                onQueryReportsClick = {
                                    navController.navigate(AppScreen.QueryReports.route)
                                }
                            )
                        }

                        //  PANTALLA DE CONSULTA DE LIBROS USUARIO (UserQueryBooks)
                        composable(AppScreen.UserQueryBooks.route) {
                            UserQueryBooksScreen(
                                authViewModel = authViewModel,
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(AppScreen.HomeUser.route) { inclusive = true }
                                    }
                                },
                                onHomeClick = {
                                    navController.navigate(AppScreen.HomeUser.route) {
                                        popUpTo(AppScreen.HomeUser.route) { inclusive = true }
                                    }
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onLoanClick = { book ->
                                    bookToEdit = book
                                    navController.navigate(AppScreen.BookLoan.route)
                                },
                                onBookClick = { book ->
                                    bookToView = book
                                    navController.navigate(AppScreen.BookDetail.route)
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                }
                            )
                        }

                        //  PANTALLA DE CONSULTA DE AUTORES USUARIO (UserQueryAuthors)
                        composable(AppScreen.UserQueryAuthors.route) {
                            UserQueryAuthorsScreen(
                                authViewModel = authViewModel,
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(AppScreen.HomeUser.route) { inclusive = true }
                                    }
                                },
                                onHomeClick = {
                                    navController.navigate(AppScreen.HomeUser.route) {
                                        popUpTo(AppScreen.HomeUser.route) { inclusive = true }
                                    }
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onAuthorClick = { author ->
                                    authorToView = author
                                    navController.navigate(AppScreen.AuthorDetail.route)
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                }
                            )
                        }

                        //  PANTALLA DE DETALLE DE AUTOR (AuthorDetail)
                        composable(AppScreen.AuthorDetail.route) {
                            AuthorDetailScreen(
                                authViewModel = authViewModel,
                                author = authorToView,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onHomeClick = {
                                    navController.navigate(AppScreen.HomeUser.route) {
                                        popUpTo(AppScreen.HomeUser.route) { inclusive = true }
                                    }
                                },
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                },
                                onBookClick = { book ->
                                    bookToEdit = book
                                    navController.navigate(AppScreen.BookLoan.route)
                                }
                            )
                        }

                        //  PANTALLA DE PERFIL DE USUARIO (UserProfile)
                        composable(AppScreen.UserProfile.route) {
                            UserProfileScreen(
                                authViewModel = authViewModel,
                                isMandatory = isMandatoryProfile,
                                onBackClick = {
                                    isMandatoryProfile = false
                                    navController.popBackStack()
                                },
                                onHomeClick = {
                                    isMandatoryProfile = false
                                    val role = authViewModel.currentUserProfile?.role
                                    if (role == "ADMIN") {
                                        navController.navigate(AppScreen.HomeAdmin.route) {
                                            popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate(AppScreen.HomeUser.route) {
                                            popUpTo(AppScreen.HomeUser.route) { inclusive = true }
                                        }
                                    }
                                },
                                onLogoutSuccess = {
                                    isMandatoryProfile = false
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ℹ️ PANTALLA DE INFORMACIÓN (Info)
                        composable(AppScreen.Info.route) {
                            InfoScreen(
                                authViewModel = authViewModel,
                                onBackClick = { navController.popBackStack() },
                                onHomeClick = {
                                    val role = authViewModel.currentUserProfile?.role
                                    if (role == "ADMIN") {
                                        navController.navigate(AppScreen.HomeAdmin.route) {
                                            popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate(AppScreen.HomeUser.route) {
                                            popUpTo(AppScreen.HomeUser.route) { inclusive = true }
                                        }
                                    }
                                },
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                }
                            )
                        }

                        //  PANTALLA DE CONSULTA DE LIBROS (AdminQueryBooks)
                        composable(AppScreen.AdminQueryBooks.route) {
                            AdminQueryBooksScreen(
                                authViewModel = authViewModel,
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                    }
                                },
                                onHomeClick = {
                                    navController.navigate(AppScreen.HomeAdmin.route) {
                                        popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                    }
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onEditBookClick = { book ->
                                    bookToEdit = book // ← Carga el libro seleccionado en el estado dinámico (Regla 4-d)
                                    navController.navigate(AppScreen.AdminRegisterBook.route) // ← Redirige al formulario
                                },
                                onBookClick = { book ->
                                    bookToView = book
                                    navController.navigate(AppScreen.BookDetail.route)
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                }
                            )
                        }
                        
                        //  PANTALLA DE REGISTRO / SOLICITUD DE PRÉSTAMO
                        composable(AppScreen.BookLoan.route) {
                            BookLoanScreen(
                                authViewModel = authViewModel,
                                preselectedBook = bookToEdit,
                                onBackClick = {
                                    bookToEdit = null
                                    navController.popBackStack()
                                },
                                onHomeClick = {
                                    bookToEdit = null
                                    val role = authViewModel.currentUserProfile?.role
                                    if (role == "ADMIN") {
                                        navController.navigate(AppScreen.HomeAdmin.route) {
                                            popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate(AppScreen.HomeUser.route) {
                                            popUpTo(AppScreen.HomeUser.route) { inclusive = true }
                                        }
                                    }
                                },
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                }
                            )
                        }

                        //  PANTALLA DE CONSULTA DE PRÉSTAMOS
                        composable(AppScreen.QueryLoans.route) {
                            QueryLoansScreen(
                                authViewModel = authViewModel,
                                onBackClick = { navController.popBackStack() },
                                onHomeClick = {
                                    val role = authViewModel.currentUserProfile?.role
                                    if (role == "ADMIN") {
                                        navController.navigate(AppScreen.HomeAdmin.route) {
                                            popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate(AppScreen.HomeUser.route) {
                                            popUpTo(AppScreen.HomeUser.route) { inclusive = true }
                                        }
                                    }
                                },
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                }
                            )
                        }

                        //  PANTALLA DE NOTIFICACIONES
                        composable(AppScreen.Notifications.route) {
                            NotificationsScreen(
                                authViewModel = authViewModel,
                                onBackClick = { navController.popBackStack() },
                                onHomeClick = {
                                    val role = authViewModel.currentUserProfile?.role
                                    if (role == "ADMIN") {
                                        navController.navigate(AppScreen.HomeAdmin.route) {
                                            popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate(AppScreen.HomeUser.route) {
                                            popUpTo(AppScreen.HomeUser.route) { inclusive = true }
                                        }
                                    }
                                },
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                }
                            )
                        }

                        //  PANTALLA DE CREACIÓN DE REPORTES
                        composable(AppScreen.SubmitReport.route) {
                            SubmitReportScreen(
                                authViewModel = authViewModel,
                                onBackClick = { navController.popBackStack() },
                                onHomeClick = {
                                    val role = authViewModel.currentUserProfile?.role
                                    if (role == "ADMIN") {
                                        navController.navigate(AppScreen.HomeAdmin.route) {
                                            popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate(AppScreen.HomeUser.route) {
                                            popUpTo(AppScreen.HomeUser.route) { inclusive = true }
                                        }
                                    }
                                },
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                }
                            )
                        }

                        //  PANTALLA DE CONSULTA DE REPORTES
                        composable(AppScreen.QueryReports.route) {
                            QueryReportsScreen(
                                authViewModel = authViewModel,
                                onBackClick = { navController.popBackStack() },
                                onHomeClick = {
                                    val role = authViewModel.currentUserProfile?.role
                                    if (role == "ADMIN") {
                                        navController.navigate(AppScreen.HomeAdmin.route) {
                                            popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate(AppScreen.HomeUser.route) {
                                            popUpTo(AppScreen.HomeUser.route) { inclusive = true }
                                        }
                                    }
                                },
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                }
                            )
                        }

                        //  PANTALLA DE DETALLE DE LIBRO
                        composable(AppScreen.BookDetail.route) {
                            BookDetailScreen(
                                authViewModel = authViewModel,
                                book = bookToView,
                                onBackClick = { navController.popBackStack() },
                                onHomeClick = {
                                    val role = authViewModel.currentUserProfile?.role
                                    if (role == "ADMIN") {
                                        navController.navigate(AppScreen.HomeAdmin.route) {
                                            popUpTo(AppScreen.HomeAdmin.route) { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate(AppScreen.HomeUser.route) {
                                            popUpTo(AppScreen.HomeUser.route) { inclusive = true }
                                        }
                                    }
                                },
                                onLogoutSuccess = {
                                    navController.navigate(AppScreen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate(AppScreen.UserProfile.route)
                                },
                                onRequestLoanClick = {
                                    bookToEdit = bookToView
                                    navController.navigate(AppScreen.BookLoan.route)
                                },
                                onEditBookClick = { selectedBook ->
                                    bookToEdit = selectedBook
                                    navController.navigate(AppScreen.AdminRegisterBook.route)
                                },
                                onDeleteBookSuccess = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    }
}