package com.example.mislibros.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mislibros.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuthViewModel : ViewModel() {

    // Inicialización bajo demanda de las instancias de Firebase
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseDatabase.getInstance()

    // Estado reactivo de carga para la UI
    var isLoading by mutableStateOf(false)
        private set

    // Estado reactivo del perfil del usuario actual
    var currentUserProfile by mutableStateOf<UserModel?>(null)
        private set

    /**
     * Inicia sesión con Email y Contraseña, recupera el rol de Firestore.
     */
    fun login(email: String, password: String, context: Context, onSuccess: (String) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Hacemos uso del viewModelScope para lanzar la corrutina asíncrona
        viewModelScope.launch {
            isLoading = true
            try {
                // 1. Intentar autenticar en Firebase Auth y esperar el resultado con .await()
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid

                if (uid != null) {
                    // 2. Consultar el nodo del usuario en Realtime Database para obtener su rol
                    val dataSnapshot = db.getReference("usuarios").child(uid).get().await()

                    if (dataSnapshot.exists()) {
                        // Mapeo automático usando el molde de UserModel
                        val userProfile = dataSnapshot.getValue(UserModel::class.java)

                        if (userProfile != null) {
                            if (userProfile.status == "BLOCKED") {
                                auth.signOut()
                                currentUserProfile = null
                                Toast.makeText(context, "Esta cuenta se encuentra suspendida.", Toast.LENGTH_LONG).show()
                            } else {
                                // Almacenar el perfil y pasar el rol obtenido (ADMIN o USER) al callback de exito
                                currentUserProfile = userProfile
                                onSuccess(userProfile.role)
                            }
                        } else {
                            Toast.makeText(context, "Error al procesar el perfil.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Si es Administrador por correo, se auto-crea el nodo con rol ADMIN
                        val user = auth.currentUser
                        if (user != null && (user.email?.contains("roberto", ignoreCase = true) == true || user.email?.contains("admin", ignoreCase = true) == true || user.email?.contains("barrios", ignoreCase = true) == true)) {
                            val userModel = UserModel(
                                userId = uid,
                                email = user.email ?: "",
                                username = (user.email ?: "").split("@").firstOrNull() ?: "",
                                role = "ADMIN",
                                status = "ACTIVE",
                                nombre = "Roberto",
                                apellidos = "Barrios Méndez",
                                fechaRegistro = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                            )
                            db.getReference("usuarios").child(uid).setValue(userModel).await()
                            currentUserProfile = userModel
                            onSuccess("ADMIN")
                        } else {
                            Toast.makeText(context, "El perfil de usuario no existe.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Validar si el usuario no esta registrado en el sistema
                if (e is com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                    Toast.makeText(context, "Usuario no registrado", Toast.LENGTH_LONG).show()
                } else if (e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                    try {
                        // Buscar en base de datos si el correo existe
                        val dbRef = FirebaseDatabase.getInstance().getReference("usuarios")
                        val snapshot = dbRef.orderByChild("email").equalTo(email).get().await()
                        if (!snapshot.exists()) {
                            // Si el correo no existe en la base de datos
                            Toast.makeText(context, "Usuario no registrado", Toast.LENGTH_LONG).show()
                        } else {
                            // Si el correo existe pero la contrasena es incorrecta
                            Toast.makeText(context, "Credenciales equivocadas", Toast.LENGTH_LONG).show()
                        }
                    } catch (dbEx: Exception) {
                        Toast.makeText(context, "Credenciales equivocadas", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Mostrar error general de ingreso
                    Toast.makeText(context, "Error de ingreso: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Cierra la sesión activa en la aplicación.
     */
    fun signOut() {
        auth.signOut()
        currentUserProfile = null
    }

    /**
     * Verifica si existe una sesión activa y recupera el perfil y rol del usuario.
     */
    fun checkActiveSession(onResult: (String?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onResult(null)
            return
        }
        viewModelScope.launch {
            isLoading = true
            try {
                val uid = currentUser.uid
                val dataSnapshot = db.getReference("usuarios").child(uid).get().await()
                if (dataSnapshot.exists()) {
                    val userProfile = dataSnapshot.getValue(UserModel::class.java)
                    if (userProfile != null) {
                        if (userProfile.status == "BLOCKED") {
                            auth.signOut()
                            currentUserProfile = null
                            onResult(null)
                        } else {
                            currentUserProfile = userProfile
                            onResult(userProfile.role)
                        }
                    } else {
                        onResult(null)
                    }
                } else {
                    // Si no existe el perfil en DB pero es Administrador (Roberto/admin en correo), auto-crear nodo
                    if (currentUser.email?.contains("roberto", ignoreCase = true) == true ||
                        currentUser.email?.contains("admin", ignoreCase = true) == true ||
                        currentUser.email?.contains("barrios", ignoreCase = true) == true
                    ) {
                        val userModel = UserModel(
                            userId = uid,
                            email = currentUser.email ?: "",
                            username = (currentUser.email ?: "").split("@").firstOrNull() ?: "",
                            role = "ADMIN",
                            status = "ACTIVE",
                            nombre = "Roberto",
                            apellidos = "Barrios Méndez",
                            fechaRegistro = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                        )
                        db.getReference("usuarios").child(uid).setValue(userModel).await()
                        currentUserProfile = userModel
                        onResult("ADMIN")
                    } else {
                        onResult(null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Si ocurre un error de red o de Firebase, devolvemos null temporalmente para seguridad
                onResult(null)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Actualiza localmente el perfil del usuario actual.
     */
    fun updateProfileLocal(updatedProfile: UserModel) {
        currentUserProfile = updatedProfile
    }

    /**
     * Actualiza el perfil del usuario en Firebase y localmente, subiendo una nueva foto si se proporciona.
     */
    fun updateProfile(updatedProfile: UserModel, context: Context, newPhoto: Bitmap? = null, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            isLoading = true
            try {
                var finalImageUrl = updatedProfile.imageUrl
                if (newPhoto != null) {
                    val storageRef = FirebaseStorage.getInstance().reference
                        .child("profile_photos/${uid}.jpg")
                    
                    val baos = ByteArrayOutputStream()
                    // Comprime la imagen Bitmap en formato JPEG a una calidad de 80% para reducir su tamano de archivo
                    newPhoto.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                    val data = baos.toByteArray() // Obtiene el arreglo de bytes para subirlo a Firebase Storage

                    storageRef.putBytes(data).await()
                    val downloadUri = storageRef.downloadUrl.await()
                    finalImageUrl = downloadUri.toString()
                }

                val finalProfile = updatedProfile.copy(imageUrl = finalImageUrl)
                db.getReference("usuarios").child(uid).setValue(finalProfile).await()
                currentUserProfile = finalProfile
                Toast.makeText(context, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error al actualizar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Verifica si el perfil del usuario tiene todos los campos obligatorios llenos.
     * Campos obligatorios: telefono, calle, noExterior, colonia, alcaldia, codigoPostal
     */
    fun isProfileComplete(profile: UserModel? = currentUserProfile): Boolean {
        if (profile == null) return false
        return profile.telefono.isNotBlank() &&
               profile.calle.isNotBlank() &&
               profile.noExterior.isNotBlank() &&
               profile.colonia.isNotBlank() &&
               profile.alcaldia.isNotBlank() &&
               profile.codigoPostal.isNotBlank()
    }

    /**
     * Inicia sesión con credenciales de Google
     */
    fun loginWithGoogle(idToken: String, context: Context, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                // Convierte el idToken de Google en una credencial de Firebase
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                // Inicia sesión en Firebase Auth con dicha credencial

                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user
                val uid = user?.uid

                if (uid != null) {
                    val email = user.email ?: ""
                    
                    // Obtener todos los usuarios y filtrar en Kotlin para evitar errores de índice en Firebase
                    val usersSnapshot = db.getReference("usuarios").get().await()
                    var existingProfile: UserModel? = null
                    var oldUid: String? = null

                    if (usersSnapshot.exists()) {
                        for (child in usersSnapshot.children) {
                            val profile = child.getValue(UserModel::class.java)
                            if (profile != null) {
                                val googlePrefix = email.split("@").firstOrNull() ?: ""
                                val dbPrefix = profile.email.split("@").firstOrNull() ?: ""
                                if (googlePrefix.isNotEmpty() && googlePrefix.equals(dbPrefix, ignoreCase = true)) {
                                    existingProfile = profile
                                    oldUid = child.key
                                    break
                                }
                            }
                        }
                    }

                    if (existingProfile != null && oldUid != null) {
                        if (existingProfile.status == "BLOCKED") {
                            auth.signOut()
                            currentUserProfile = null
                            Toast.makeText(context, "Esta cuenta se encuentra suspendida.", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        // Si el UID registrado es diferente al UID nuevo de Google, migramos
                        if (oldUid != uid) {
                            // A. Copiamos el perfil al nuevo UID actualizando el campo userId al nuevo y corrigiendo el email si tenía algún error de dedo
                            val migratedProfile = existingProfile.copy(userId = uid, email = email)
                            db.getReference("usuarios").child(uid).setValue(migratedProfile).await()
                            
                            // B. Eliminamos el nodo del viejo UID de la tabla de usuarios
                            db.getReference("usuarios").child(oldUid).removeValue().await()
                            
                            // C. Migramos los préstamos
                            val loansSnapshot = db.getReference("prestamos").get().await()
                            if (loansSnapshot.exists()) {
                                for (loanChild in loansSnapshot.children) {
                                    val loanUserId = loanChild.child("userId").getValue(String::class.java)
                                    if (loanUserId == oldUid) {
                                        loanChild.ref.child("userId").setValue(uid).await()
                                    }
                                }
                            }

                            // D. Migramos los reportes
                            val reportsSnapshot = db.getReference("reportes").get().await()
                            if (reportsSnapshot.exists()) {
                                for (reportChild in reportsSnapshot.children) {
                                    val reportUserId = reportChild.child("userId").getValue(String::class.java)
                                    if (reportUserId == oldUid) {
                                        reportChild.ref.child("userId").setValue(uid).await()
                                    }
                                }
                            }

                            currentUserProfile = migratedProfile
                        } else {
                            // Es exactamente el mismo UID, no requiere migración
                            currentUserProfile = existingProfile
                        }
                        onSuccess(currentUserProfile!!.role)
                    } else {
                        // Crear perfil para el nuevo usuario de Google (es totalmente nuevo)
                        val displayName = user.displayName ?: ""
                        val nameParts = displayName.split(" ")
                        val nombre = nameParts.firstOrNull() ?: ""
                        val apellidos = if (nameParts.size > 1) {
                            nameParts.drop(1).joinToString(" ")
                        } else {
                            ""
                        }

                        val role = "USER"
// Crear Perfil si es un Usuario Totalmente Nuevo
                        val userModel = UserModel(
                            userId = uid,
                            email = email,
                            username = email.split("@").firstOrNull() ?: "",
                            role = role,
                            status = "ACTIVE",
                            imageUrl = user.photoUrl?.toString() ?: "",
                            nombre = nombre,
                            apellidos = apellidos,
                            fechaRegistro = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                        )
                        db.getReference("usuarios").child(uid).setValue(userModel).await()
                        currentUserProfile = userModel
                        onSuccess(role)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error de ingreso con Google: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }
}