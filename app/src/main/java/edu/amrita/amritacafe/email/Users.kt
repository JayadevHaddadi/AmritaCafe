package edu.amrita.amritacafe.email

val allUsers = listOf<User>(
    User("Jayadev", "", "jayadev.haddadi@gmail.com"),
    User("Kurunandan", "kuru1", "jayadev.haddadi@gmail.com"),
    User("Mata", "mymother", "jayadev.haddadi@gmail.com"),
    User("Amrita", "eternal", "jayadev.haddadi@gmail.com"),
    User("Ananada", "mybliss", "kurunandan@gmail.com"),
    User("Mayi", "myself", "kurunandan@gmail.com"),
    User("Devi", "mygod", "kurunandan@gmail.com")
)

data class User(val name: String, val password: String, val email: String)