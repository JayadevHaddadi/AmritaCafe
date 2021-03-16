package edu.amrita.amritacafe.menu

const val TOPPING = "TOPPING"
const val PIZZA = "PIZZA"
const val PORRIDGE = "PORRIDGE"

//grilled stuffs
const val BREAKFAST = "BREAKFAST"
const val SANDWICH = "SANDWICH"
const val EGGS = "EGGS"
const val TOAST = "TOAST"
const val BURGER = "BURGER"

val DEFAULT_BREAKFAST_MENU: List<MenuItemUS> = listOf(
    MenuItemUS("Grilled Cheese", "GC", 40f, SANDWICH),
    MenuItemUS("Egg Cheese Sand", "ECS", 60f, SANDWICH),
    MenuItemUS("Omelet Fries", "OMS", 50f, SANDWICH),

    //GRILLED
    MenuItemUS("Egg Sunny Side Up", "FE SSU", 20f, EGGS),
    MenuItemUS("Fried Egg Overhard", "FE OH", 20f, EGGS),
    MenuItemUS("Plain Omelet", "OM", 40f, EGGS),
    MenuItemUS("Veggie Omelet", "VEG OM", 60f, EGGS),
    MenuItemUS("Deluxe Omelet", "DXL OM", 115f, EGGS),
    MenuItemUS("Cheese Omelet", "OM CH", 60f, EGGS),
    MenuItemUS("Fresh Tomato Omelet", "OM FRT", 45f, EGGS),
    MenuItemUS("Pesto Omelet", "PSTO OM", 70f, EGGS),
    MenuItemUS("Olive Omelet", "OLV OM", 75f, EGGS),
    MenuItemUS("Veggie Cheese Omelet", "VEG CH OM", 80f, EGGS),
    MenuItemUS("Gourmet Omelet", "GM OM", 110f, EGGS),
    MenuItemUS("Onion Omelet", "ON OM", 45f, EGGS),

    //GRILLED
    MenuItemUS(TOAST, "T", 3f, TOAST),
    MenuItemUS("Butter Jam Toast", "BJT", 15f, TOAST),
    MenuItemUS("Butter Toast", "BT", 10f, TOAST),
    MenuItemUS("Jam Toast", "JT", 10f, TOAST),
    MenuItemUS("Peanut But Toast", "PBT", 10f, TOAST),
    MenuItemUS("Cheese Toast", "CHT", 25f, TOAST),
    MenuItemUS("Honey Toast", "HT", 10f, TOAST),
    MenuItemUS("Butter Honey Toast", "BHT", 20f, TOAST),
    MenuItemUS("Choco Toast", "CHOC T", 15f, TOAST),

    //GRILLED
    MenuItemUS("French Toast", "FT", 40f, BREAKFAST),
    MenuItemUS("Ragi Pancake", "RAG PAN", 40f, BREAKFAST),
    MenuItemUS("Pancake", "PAN", 40f, BREAKFAST),
    MenuItemUS("Hash Brown", "HASH", 20f, BREAKFAST),
    MenuItemUS("Vegan Omelet", "VN OM", 40f, BREAKFAST),

    MenuItemUS("Oatmeal", "OAT", 30f, PORRIDGE),
    MenuItemUS("Ragi Porridge", "POR", 30f, PORRIDGE),

    MenuItemUS("Jam", "+JAM", 10f, TOPPING),
    MenuItemUS("Peanut Butter", "+PB", 10f, TOPPING),
    MenuItemUS("Butter", "+BUTT", 10f, TOPPING),
    MenuItemUS("Honey", "+HON", 10f, TOPPING),
    MenuItemUS("Choco Spread", "+CHOC", 15f, TOPPING),

    MenuItemUS("Bread", "BREAD", 3f, "Side"),
    MenuItemUS("Butter", "BUTT", 10f, "Side"),
    MenuItemUS("Capsicum", "CAP", 10f, "Side"),
    MenuItemUS("Carrot", "CARROT", 10f, "Side"),
    MenuItemUS("Honey", "HON", 10f, "Side"),
    MenuItemUS("Jam", "JAM", 10f, "Side"),
    MenuItemUS("Sliced Cheese", "SL CH", 20f, "Side"),
    MenuItemUS("Choco Spread", "CHOC", 15f, "Side"),
    MenuItemUS("Peanut Butter", "PB", 10f, "Side"),
    MenuItemUS("Pesto", "PSTO", 25f, "Side"),
    MenuItemUS("Raw Egg", "RAW EGG", 10f, "Side"),
    MenuItemUS("Ketchup", "KETUP", 5f, "Side"),
    MenuItemUS("Olive", "OLV", 30f, "Side"),
    MenuItemUS("Onion", "ON", 5f, "Side"),
    MenuItemUS("Fresh Tomato", "FRT", 5f, "Side"),
    MenuItemUS("Tomato Sauce", "TOM", 10f, "Side"),
    MenuItemUS("Grated Cheese", "CH", 20f, "Side")
)

val DEFAULT_LUNCH_DINNER_MENU: List<MenuItemUS> = listOf(
    MenuItemUS("Cheese Pizza", "CH PZA", 100f, PIZZA),
    MenuItemUS("Olive Pizza", "OLV PZA", 135f, PIZZA),
    MenuItemUS("Veg Pizza", "VEG PZA", 125f, PIZZA),
    MenuItemUS("Vegan Veg Pizza", "VN VEG PZA", 100f, PIZZA),
    MenuItemUS("Gourmet Pizza", "GM PZA", 150f, PIZZA),
    MenuItemUS("Vegan Gourmet PZA", "VN GM PZA", 125f, PIZZA),
    MenuItemUS("Pesto Pizza", "PSTO PZA", 125f, PIZZA),
    MenuItemUS("Paneer Pizza", "PAN PZA", 150f, PIZZA),
    MenuItemUS("Mediter. Pizza", "MED PZA", 125f, PIZZA),

    //grilled
    MenuItemUS("Grilled Cheese", "GC", 40f, SANDWICH),
    MenuItemUS("Egg Cheese Sand", "ECS", 60f, SANDWICH),
    MenuItemUS("Omelet Fries", "OMS", 50f, SANDWICH),
    MenuItemUS("Deluxe Grill CH", "DXL GC", 75f, SANDWICH),
    MenuItemUS("Grilled Ch FRT", "GC FRT", 45f, SANDWICH),

    MenuItemUS("Veggie Burger", "VB", 40f, BURGER),
    MenuItemUS("Cheese Burger", "CB", 60f, BURGER),
    MenuItemUS("Patty", "PATTY", 20f, BURGER),
    MenuItemUS("Deluxe Burger", "DLXB", 80f, BURGER),
    MenuItemUS("Pesto Burger", "PSTOB", 65f, BURGER),

    MenuItemUS("Egg Sunny Side Up", "FE SSU", 20f, EGGS),
    MenuItemUS("Fried Egg Overhard", "FE OH", 20f, EGGS),
    MenuItemUS("Plain Omelet", "OM", 40f, EGGS),
    MenuItemUS("Veggie Omelet", "VEG OM", 60f, EGGS),
    MenuItemUS("Deluxe Omelet", "DXL OM", 115f, EGGS),
    MenuItemUS("Cheese Omelet", "OM CH", 60f, EGGS),
    MenuItemUS("Fresh Tomato Omelet", "OM FRT", 45f, EGGS),
    MenuItemUS("Pesto Omelet", "PSTO OM", 70f, EGGS),
    MenuItemUS("Olive Omelet", "OLV OM", 75f, EGGS),
    MenuItemUS("Veggie Cheese Omelet", "VEG CH OM", 80f, EGGS),
    MenuItemUS("Gourmet Omelet", "GM OM", 110f, EGGS),
    MenuItemUS("Onion Omelet", "ON OM", 45f, EGGS),

    MenuItemUS(TOAST, "T", 3f, TOAST),
    MenuItemUS("Butter Jam Toast", "BJT", 15f, TOAST),
    MenuItemUS("Butter Toast", "BT", 10f, TOAST),
    MenuItemUS("Jam Toast", "JT", 10f, TOAST),
    MenuItemUS("Peanut But Toast", "PBT", 10f, TOAST),
    MenuItemUS("Cheese Toast", "CHT", 25f, TOAST),
    MenuItemUS("Honey Toast", "HT", 10f, TOAST),
    MenuItemUS("Butter Honey Toast", "BHT", 20f, TOAST),
    MenuItemUS("Choco Toast", "CHOC T", 15f, TOAST),

    MenuItemUS("Plain Pasta", "PL PSTA", 30f, "Pasta"),
    MenuItemUS("Pasta Tomato Sauce", "PSTA TOM", 60f, "Pasta"),
    MenuItemUS("Pasta Tomato Cheese", "PAST TOM CH", 80f, "Pasta"),
    MenuItemUS("Pasta All Soy", "PAST ALL SOY", 90f, "Pasta"),
    MenuItemUS("Pesto Pasta", "PSTO PAST", 60f, "Pasta"),
    MenuItemUS("Vegan Gourmet Pasta", "VN GM PAST", 100f, "Pasta"),
    MenuItemUS("Deluxe Pasta", "DLX PAST", 115f, "Pasta"),

    MenuItemUS("Sm Fries", "SMFF", 30f, "Fries"),
    MenuItemUS("Lg Fries", "LGFF", 60f, "Fries"),
    MenuItemUS("Sm Fries Salt", "SMSAFF", 35f, "Fries"),
    MenuItemUS("Lg Fries Salt", "LGSAFF", 70f, "Fries"),

    MenuItemUS("Simple Salad", "SAL", 30f, "Salad"),
    MenuItemUS("Sprout Salad", "SPRSAL", 50f, "Salad"),
    MenuItemUS("Deluxe Salad", "DXL SAL", 90f, "Salad"),
    MenuItemUS("Pesto Salad", "PSTO SAL", 80f, "Salad"),
    MenuItemUS("Quinoa Salad", "QNA SAL", 150f, "Salad"),

    MenuItemUS("Kitcheri", "KITCH", 30f, "Kitcheri"),
    MenuItemUS("Steamed Veg", "ST VEG", 30f, "Kitcheri"),
    MenuItemUS("Quinoa", "QNA", 100f, "Kitcheri"),
    MenuItemUS("Steamed Broccoli", "ST BROC", 100f, "Kitcheri"),
    MenuItemUS("Hummus", "HUM", 30f, "Kitcheri"),

    MenuItemUS("Bread", "BREAD", 3f, "Side"),
    MenuItemUS("Butter", "BUTT", 10f, "Side"),
    MenuItemUS("Capsicum", "CAP", 10f, "Side"),
    MenuItemUS("Carrot", "CARROT", 10f, "Side"),
    MenuItemUS("Honey", "HON", 10f, "Side"),
    MenuItemUS("Jam", "JAM", 10f, "Side"),
    MenuItemUS("Sliced Cheese", "SL CH", 20f, "Side"),
    MenuItemUS("Mustard", "MRD", 10f, "Side"),
    MenuItemUS("Choco Spread", "CHOC", 15f, "Side"),
    MenuItemUS("Peanut Butter", "PB", 10f, "Side"),
    MenuItemUS("Patty", "PATTY", 20f, "Side"),
    MenuItemUS("Cucumber", "CUKE", 10f, "Side"),
    MenuItemUS("Pesto", "PSTO", 25f, "Side"),
    MenuItemUS("Raw Egg", "RAW EGG", 10f, "Side"),
    MenuItemUS("Gomasio", "GOMAZ", 10f, "Side"),
    MenuItemUS("Ketchup", "KETUP", 5f, "Side"),
    MenuItemUS("Olive", "OLV", 30f, "Side"),
    MenuItemUS("Onion", "ON", 5f, "Side"),
    MenuItemUS("Pickle", "PKL", 10f, "Side"),
    MenuItemUS("Dressing", "DRS", 10f, "Side"),
    MenuItemUS("Soya", "SOYA", 10f, "Side"),
    MenuItemUS("Sprouts", "SPRTS", 10f, "Side"),
    MenuItemUS("Fresh Tomato", "FRT", 5f, "Side"),
    MenuItemUS("Tomato Sauce", "TOM", 10f, "Side"),
    MenuItemUS("Bun", "BUN", 10f, "Side"),
    MenuItemUS("Grated Cheese", "CH", 20f, "Side"),
    MenuItemUS("Mayo", "MAYO", 10f, "Side")
)