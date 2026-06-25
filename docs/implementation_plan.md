# Implementation Plan — Book Details, Author Biography, and Detailed Book View

This plan outlines the implementation of new fields for books (book review, author biography, and author photo) in the registration form, expanding search filtering, and creating a new book details view screen.

## Proposed Changes

We will extend `BookModel` and update the registration form (`AdminRegisterBookScreen`) to collect the new information. We will upload the author's photo to Firebase Storage and store its reference URL in Firebase Realtime Database. We will then update search screens to support searching by author and to open a new details screen (`BookDetailScreen`) when clicking on any book.

---

### Data Model

#### [MODIFY] [BookModel.kt](file:///c:/Users/rbarr/Desktop/MisLibros/app/src/main/java/com/example/mislibros/model/BookModel.kt)
Add new properties:
- `review`: String (Book review/synopsis)
- `authorBio`: String (Author's short biography)
- `authorImageUrl`: String (URL to the author's photo in Firebase Storage)

---

### Navigation

#### [MODIFY] [AppScreen.kt](file:///c:/Users/rbarr/Desktop/MisLibros/app/src/main/java/com/example/mislibros/ui/navigation/AppScreen.kt)
- Add a new route object: `object BookDetail : AppScreen("book_detail")`

#### [MODIFY] [MainActivity.kt](file:///c:/Users/rbarr/Desktop/MisLibros/app/src/main/java/com/example/mislibros/MainActivity.kt)
- Add a state variable to track the currently selected book for viewing: `var bookToView by remember { mutableStateOf<BookModel?>(null) }`
- Update `composable(AppScreen.AdminRegisterBook.route)` callback parameter `onRegisterSubmit` to accept two bitmaps (book cover and author photo).
- Implement sequential uploading in `MainActivity` for both the cover photo and author photo, saving the resulting URLs along with the new fields (`review`, `authorBio`) in `/libros/{bookId}`.
- Add route `composable(AppScreen.BookDetail.route)` to render the new `BookDetailScreen`.

---

### Book Registration & Details Screens

#### [MODIFY] [AdminRegisterBookScreen.kt](file:///c:/Users/rbarr/Desktop/MisLibros/app/src/main/java/com/example/mislibros/ui/screens/admin/AdminRegisterBookScreen.kt)
- Modify `onRegisterSubmit` signature to accept `(Map<String, String>, Bitmap?, Bitmap?)`.
- Add local state for `review`, `authorBio`, and `authorFotoBitmap` (author's photo).
- Add a camera/gallery photo picker dialog and UI block to select and preview the author's photo.
- Add text fields for "Reseña del Libro" and "Mini Biografía del Autor" (multiline inputs).
- Modify the "CANCELAR" button to also reset the new fields and the author's photo bitmap.

#### [NEW] [BookDetailScreen.kt](file:///c:/Users/rbarr/Desktop/MisLibros/app/src/main/java/com/example/mislibros/ui/screens/user/BookDetailScreen.kt)
Create a new details screen that displays:
- A premium layout with the book cover (large image, rounded corners).
- Title (large, bold), Author name, and classification details (Publisher, Year, Edition, Category, Language, Pages, Stock, Status).
- **Reseña del Libro (Book Review)** section.
- **Biografía del Autor (Author Bio)** section, including the author's photo (circular/rounded).
- A Back button and a Home button using `LibraryScaffold`.
- For normal users, a "PEDIR PRESTADO" button to directly navigate to the loan form with this book pre-selected.

---

### Book Query Screens

#### [MODIFY] [AdminQueryBooksScreen.kt](file:///c:/Users/rbarr/Desktop/MisLibros/app/src/main/java/com/example/mislibros/ui/screens/admin/AdminQueryBooksScreen.kt)
- Update filter logic to check both `book.title` and `book.author` (case-insensitive).
- Make the `BookListItem` Card clickable so that clicking on a book sets `bookToView` and navigates to the details screen.

#### [MODIFY] [UserQueryBooksScreen.kt](file:///c:/Users/rbarr/Desktop/MisLibros/app/src/main/java/com/example/mislibros/ui/screens/user/UserQueryBooksScreen.kt)
- Update filter logic to check both `book.title` and `book.author` (case-insensitive).
- Make the `UserBookListItem` Card clickable so that clicking on a book sets `bookToView` and navigates to the details screen.

---

## Verification Plan

### Automated Build Check
- Compile the app using `.\gradlew compileDebugKotlin` to ensure no syntax errors.

### Manual Verification Flow
1. **Admin Form Verification**:
   - Go to "Registrar Libro".
   - Fill in details, select a book cover photo, fill in Book Review, Author Biography, and select an Author Photo.
   - Click "REGISTRAR" and verify upload completes successfully.
2. **Search Verification**:
   - In both Admin and User profiles, search for a book by its author name. Verify it displays correctly.
3. **Details View Verification**:
   - Click on the book card. Verify the new screen displays the book cover, review, author biography, and author photo in a premium layout.
