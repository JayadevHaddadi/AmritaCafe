package edu.amrita.amritacafe.email

val allUsers = listOf<User>(
    // TODO CHANGE THIS
    User("YOUR USER NAME", "", "YOUR USER EMAIL @ GMAIL.COM"),
    User("YOUR USER NAME2", "", "YOUR USER EMAIL @ GMAIL.COM"),
    User("YOUR USER NAME3", "", "YOUR USER EMAIL @ GMAIL.COM")
    )

data class User(val name: String, val password: String, val email: String)