package com.example.mislibros.ui.navigation

sealed class AppScreen(val route: String) {
    object Login : AppScreen("login")
    object Register : AppScreen("register")
    object HomeAdmin : AppScreen("home_admin")
    object HomeUser : AppScreen("home_user")
    object AdminRegisterUser : AppScreen("admin_register_user")
    object AdminQueryUsers : AppScreen("admin_query_users")
    object AdminRegisterBook : AppScreen("admin_register_book")

    object AdminQueryBooks : AppScreen("admin_query_books")
    object UserQueryBooks : AppScreen("user_query_books")
    object UserProfile : AppScreen("user_profile")
    object BookLoan : AppScreen("book_loan")
    object QueryLoans : AppScreen("query_loans")
    object Notifications : AppScreen("notifications")
    object SubmitReport : AppScreen("submit_report")
    object QueryReports : AppScreen("query_reports")
    object BookDetail : AppScreen("book_detail")
    object UserQueryAuthors : AppScreen("user_query_authors")
    object AuthorDetail : AppScreen("author_detail")
    object Info : AppScreen("info")
}