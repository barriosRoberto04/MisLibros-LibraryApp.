# MisLibros - Sistema de Gestion de Biblioteca Digital

MisLibros es una aplicacion movil nativa para Android disenada para la gestion y consulta de colecciones de libros, prestamos y reportes de biblioteca. El proyecto esta desarrollado en Kotlin utilizando Jetpack Compose para la interfaz de usuario y sigue el patron de arquitectura MVVM.

## Arquitectura y Estructura del Proyecto

La aplicacion esta organizada en modulos siguiendo las mejores practicas de desarrollo en Android:

- **Modelos de Datos (com.example.mislibros.model):** Define la estructura de las entidades del sistema (UserModel, BookModel, BookLoanModel, NotificationModel, ReportModel) que mapean directamente con los nodos correspondientes en Firebase Realtime Database.
- **Vistas (com.example.mislibros.ui.screens):** Componentes visuales organizados por modulos de negocio (login, perfil, administrador y usuario general). La interfaz es completamente declarativa y adaptativa.
- **Componentes Comunes (com.example.mislibros.ui.components):** Elementos visuales reutilizables como LibraryScaffold (que gestiona la barra superior, barra inferior de navegacion y el contador de notificaciones en tiempo real) y campos de entrada de texto premium.
- **Modelos de Vista (com.example.mislibros.viewmodel):** AuthViewModel centraliza la logica de negocio, autenticacion con Firebase Auth (incluyendo integracion con Google Sign-In) y persistencia del perfil de usuario local y remoto.
- **Navegacion (com.example.mislibros.ui.navigation):** Controla el flujo de pantallas sin acoplamiento a traves de un NavHost y un proveedor global de NavController (LocalNavController).

## Integracion con Firebase

El sistema aprovecha los servicios en la nube de Firebase para la persistencia, seguridad y tiempo real:

1. **Firebase Authentication:** Autenticacion de usuarios mediante correo/contrasena y Google Sign-In. Soporta flujos de validacion para cuentas suspendidas y complecion obligatoria del perfil de usuario.
2. **Firebase Realtime Database:** Base de datos NoSQL que sincroniza la informacion en tiempo real. 
   - Control de inventario (el stock de los libros disminuye automaticamente al activarse un prestamo y aumenta al completarse la devolucion).
   - Bandeja de notificaciones en tiempo real con contadores en la barra superior.
3. **Firebase Storage:** Almacenamiento seguro de archivos multimedia (fotos de perfil de usuarios, portadas de libros y fotografias de autores). Las imagenes se comprimen localmente en formato JPEG al 80% de calidad antes de subirse para optimizar el consumo de red.

## Requisitos de Instalacion y Configuracion

Para compilar y ejecutar el proyecto en Android Studio, siga estos pasos:

1. Clonar el repositorio.
2. Asegurarse de tener instalado JDK 17 o superior y el SDK de Android compatible.
3. Descargar el archivo `google-services.json` desde su consola de Firebase y colocarlo en el directorio del modulo de la aplicacion (`app/google-services.json`).
4. Sincronizar el proyecto con los archivos de Gradle.
5. Para verificar que no existan errores de compilacion, puede ejecutar el siguiente comando en la terminal:
   ```cmd
   .\gradlew compileDebugKotlin
   ```
6. Ejecutar la aplicacion en un dispositivo fisico o emulador mediante Android Studio.
