# Task List — Edición de Contraseña, Barra Superior Simplificada y Pantalla Informativa

- [x] **UserModel.kt** — Añadir campo `password` para persistir contraseñas.
- [x] **LocalNavController.kt** — Definir CompositionLocal global para navegación sin sobrecargar llamadas.
- [x] **AppScreen.kt** — Añadir ruta `Info` para pantalla de información de la app.
- [x] **InfoScreen.kt** — Crear pantalla con ¿Quiénes Somos?, Misión, Visión y datos de Contacto/Soporte en tarjetas.
- [x] **LibraryScaffold.kt** —
  - [x] Simplificar el TopBar en la Home para mostrar sólo el logo y nombre de la app (MisLibros) centrados.
  - [x] Adaptar el botón Info de la barra inferior para navegar a la nueva `InfoScreen` usando el controlador de navegación.
- [x] **AdminRegisterUserScreen.kt** — Habilitar sección de seguridad en modo edición para actualizar la contraseña del usuario (opcional y validando formato/coincidencia).
- [x] **MainActivity.kt** —
  - [x] Integrar `CompositionLocalProvider` para registrar globalmente el NavController.
  - [x] Registrar la ruta `AppScreen.Info` en el NavHost.
  - [x] Modificar flujo de edición de usuario: si hay nueva contraseña, re-autenticar al usuario temporalmente con la contraseña anterior en Firebase Auth, actualizarla en Auth y actualizarla en la base de datos de Realtime Database.
- [x] **Verification** — Compilar el proyecto con éxito sin fallas sintácticas de Kotlin.
