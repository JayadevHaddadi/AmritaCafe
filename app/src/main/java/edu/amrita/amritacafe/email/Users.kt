package edu.amrita.amritacafe.email

val allUsers = listOf<User>(
    User("Jayadev", "a", "jayadev.haddadi@gamil.com"),
    User("Kurunandan", "kuru1", "kurunandan@gamil.com")
)

data class User(val name: String, val password: String, val email: String)