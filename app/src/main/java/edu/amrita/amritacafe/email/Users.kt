package edu.amrita.amritacafe.email

var allUsers = listOf(
    User("Jayadev", "", "jayadev.haddadi@gmail.com"),
    User("Kurunandan", "", "kuru.jain@gmail.com"),
    User("Akshay", "", "akshay.schneiderhan@gmail.com"),
    User("Mata", "mymother", "akshay.schneiderhan@gmail.com"),
    User("Amrita", "eternal", "akshay.schneiderhan@gmail.com"),
    User("Ananada", "mybliss", "akshay.schneiderhan@gmail.com"),
    User("Mayi", "myself", "akshay.schneiderhan@gmail.com"),
    User("Devi", "mygod", "akshay.schneiderhan@gmail.com")
)

var adminEmail = "amritacafeapp@gmail.com"

data class User(val name: String, val password: String, val email: String)