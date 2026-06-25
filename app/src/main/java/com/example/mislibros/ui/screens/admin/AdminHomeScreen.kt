package com.example.mislibros.ui.screens.admin

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mislibros.R
import com.example.mislibros.ui.theme.YaleBlue
import com.example.mislibros.ui.theme.AirForceBlue
import com.example.mislibros.ui.theme.AmberOrange
import com.example.mislibros.ui.theme.SunnyYellow
import com.example.mislibros.ui.components.LibraryScaffold
import com.example.mislibros.viewmodel.AuthViewModel

@Composable
fun AdminHomeScreen(
    authViewModel: AuthViewModel,
    onLogoutSuccess: () -> Unit,
    onHomeClick: () -> Unit,
    onRegisterUserClick: () -> Unit,
    onQueryUsersClick: () -> Unit,
    onRegisterBookClick: () -> Unit,
    onQueryBooksClick: () -> Unit,
    onRegisterLoanClick: () -> Unit,
    onQueryLoansClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSubmitReportClick: () -> Unit,
    onQueryReportsClick: () -> Unit
) {
    val context = LocalContext.current
    val comingSoonText = stringResource(id = R.string.coming_soon)
    val menuRegisterUserTitle = stringResource(id = R.string.menu_register_user)
    val menuQueryUserTitle = stringResource(id = R.string.menu_query_user)
    val menuRegisterBookTitle = stringResource(id = R.string.menu_register_book)
    val menuQueryBookTitle = stringResource(id = R.string.menu_query_book)
    val menuRegisterLoanTitle = stringResource(id = R.string.menu_register_loan)
    val menuQueryLoansTitle = stringResource(id = R.string.menu_query_loans)
    val menuReportsTitle = stringResource(id = R.string.menu_reports)
    val menuQueryReportsTitle = stringResource(id = R.string.menu_query_reports)
    val scrollState = rememberScrollState()

    LibraryScaffold(
        authViewModel = authViewModel,
        title = null,
        isAdmin = true,
        onHomeClick = onHomeClick,
        onProfileClick = onProfileClick,
        onLogoutSuccess = onLogoutSuccess,
        onNotificationsClick = onNotificationsClick
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                        Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = YaleBlue,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.my_books),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Grid de 7 opciones para el administrador
            val menuItems = listOf(
                AdminMenuItem(
                    title = stringResource(id = R.string.menu_register_user),
                    icon = Icons.Default.PersonAdd,
                    color = YaleBlue
                ),
                AdminMenuItem(
                    title = stringResource(id = R.string.menu_query_user),
                    icon = Icons.Default.Group,
                    color = YaleBlue
                ),
                AdminMenuItem(
                    title = stringResource(id = R.string.menu_register_book),
                    icon = Icons.Default.LibraryAdd,
                    color = YaleBlue
                ),
                AdminMenuItem(
                    title = stringResource(id = R.string.menu_query_book),
                    icon = Icons.Default.Search,
                    color = YaleBlue
                ),
                AdminMenuItem(
                    title = stringResource(id = R.string.menu_register_loan),
                    icon = Icons.Default.Book,
                    color = YaleBlue
                ),
                AdminMenuItem(
                    title = stringResource(id = R.string.menu_query_loans),
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    color = YaleBlue
                ),
                AdminMenuItem(
                    title = stringResource(id = R.string.menu_reports),
                    icon = Icons.Default.CloudUpload,
                    color = YaleBlue
                ),
                AdminMenuItem(
                    title = stringResource(id = R.string.menu_query_reports),
                    icon = Icons.Default.Feedback,
                    color = YaleBlue
                )
            )

            // Renderizado en filas de 2 elementos
            for (i in menuItems.indices step 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Item izquierdo
                    Box(modifier = Modifier.weight(1f)) {
                        AdminMenuCard(item = menuItems[i]) {
                            if (menuItems[i].title == menuRegisterUserTitle) {
                                onRegisterUserClick()
                            } else if (menuItems[i].title == menuQueryUserTitle) {
                                onQueryUsersClick()
                            } else if (menuItems[i].title == menuRegisterBookTitle) {
                                onRegisterBookClick()
                            } else if (menuItems[i].title == menuQueryBookTitle) {
                                onQueryBooksClick()
                            } else if (menuItems[i].title == menuRegisterLoanTitle) {
                                onRegisterLoanClick()
                            } else if (menuItems[i].title == menuQueryLoansTitle) {
                                onQueryLoansClick()
                            } else if (menuItems[i].title == menuReportsTitle) {
                                onSubmitReportClick()
                            } else if (menuItems[i].title == menuQueryReportsTitle) {
                                onQueryReportsClick()
                            } else {
                                Toast.makeText(context, "${menuItems[i].title}: $comingSoonText", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    // Item derecho (si existe)
                    if (i + 1 < menuItems.size) {
                        Box(modifier = Modifier.weight(1f)) {
                            AdminMenuCard(item = menuItems[i + 1]) {
                                if (menuItems[i + 1].title == menuRegisterUserTitle) {
                                    onRegisterUserClick()
                                } else if (menuItems[i + 1].title == menuQueryUserTitle) {
                                    onQueryUsersClick()
                                } else if (menuItems[i + 1].title == menuRegisterBookTitle) {
                                    onRegisterBookClick()
                                } else if (menuItems[i + 1].title == menuQueryBookTitle) {
                                    onQueryBooksClick()
                                } else if (menuItems[i + 1].title == menuRegisterLoanTitle) {
                                    onRegisterLoanClick()
                                } else if (menuItems[i + 1].title == menuQueryLoansTitle) {
                                    onQueryLoansClick()
                                } else if (menuItems[i + 1].title == menuReportsTitle) {
                                    onSubmitReportClick()
                                } else if (menuItems[i + 1].title == menuQueryReportsTitle) {
                                    onQueryReportsClick()
                                } else {
                                    Toast.makeText(context, "${menuItems[i + 1].title}: $comingSoonText", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        // Caja vacía de relleno para conservar el alineamiento
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

data class AdminMenuItem(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun AdminMenuCard(
    item: AdminMenuItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(item.color.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
