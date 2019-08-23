
package edu.amrita.jayadev.amritacafe.settings

import edu.amrita.jayadev.amritacafe.menu.Availability
import edu.amrita.jayadev.amritacafe.menu.Category
import edu.amrita.jayadev.amritacafe.menu.Location
import edu.amrita.jayadev.amritacafe.menu.MenuItem

val defaultConfiguration = Configuration(
    "192.168.1.17",
    "192.168.1.17",
    listOf(
        MenuItem(
            "Veggie Burger",
            "VB",
            40,
            Availability.LunchDinner,
            Location.Grill,
            Category.Burger
        ),
        MenuItem(
            "Cheese Burger",
            "CVB",
            60,
            Availability.LunchDinner,
            Location.Grill,
            Category.Burger
        ),
        MenuItem(
            "Deluxe Burger",
            "DLX VB",
            80,
            Availability.LunchDinner,
            Location.Grill,
            Category.Burger
        ),
        MenuItem(
            "Patty",
            "PATTY",
            20,
            Availability.LunchDinner,
            Location.Grill,
            Category.Burger
        ),
        MenuItem(
            "Pesto Burger",
            "PST B",
            40,
            Availability.LunchDinner,
            Location.Grill,
            Category.Burger
        ),
        MenuItem(
            "Egg Sunnyside Up",
            "FE SSU",
            15,
            Availability.All,
            Location.Grill,
            Category.Eggs
        ),
        MenuItem(
            "Egg Over Hard",
            "FE OH",
            15,
            Availability.All,
            Location.Grill,
            Category.Eggs
        ),
        MenuItem(
            "Plain Omelet",
            "OM",
            30,
            Availability.All,
            Location.Grill,
            Category.Eggs
        ),
        MenuItem(
            "Cheese Omelet",
            "OM CH",
            50,
            Availability.All,
            Location.Grill,
            Category.Eggs
        ),
        MenuItem(
            "Onion Omelet",
            "ON OM",
            35,
            Availability.All,
            Location.Grill,
            Category.Eggs
        ),
        MenuItem(
            "Deluxe Omelet",
            "DLX OM",
            95,
            Availability.All,
            Location.Grill,
            Category.Eggs
        )
    )
)


