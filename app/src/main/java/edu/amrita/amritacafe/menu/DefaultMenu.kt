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

val DEFAULT_BREAKFAST_MENU: List<MenuItem> = listOf(
    MenuItem("Grilled Cheese", "GC", 40f, SANDWICH),
    MenuItem("Egg Cheese Sand", "ECS", 60f, SANDWICH),
    MenuItem("Omelet Fries", "OMS", 50f, SANDWICH),

    //GRILLED
    MenuItem("Egg Sunny Side Up", "FE SSU", 20f, EGGS),
    MenuItem("Fried Egg Overhard", "FE OH", 20f, EGGS),
    MenuItem("Plain Omelet", "OM", 40f, EGGS),
    MenuItem("Veggie Omelet", "VEG OM", 60f, EGGS),
    MenuItem("Deluxe Omelet", "DXL OM", 115f, EGGS),
    MenuItem("Cheese Omelet", "OM CH", 60f, EGGS),
    MenuItem("Fresh Tomato Omelet", "OM FRT", 45f, EGGS),
    MenuItem("Pesto Omelet", "PSTO OM", 70f, EGGS),
    MenuItem("Olive Omelet", "OLV OM", 75f, EGGS),
    MenuItem("Veggie Cheese Omelet", "VEG CH OM", 80f, EGGS),
    MenuItem("Gourmet Omelet", "GM OM", 110f, EGGS),
    MenuItem("Onion Omelet", "ON OM", 45f, EGGS),

    //GRILLED
    MenuItem(TOAST, "T", 3f, TOAST),
    MenuItem("Butter Jam Toast", "BJT", 15f, TOAST),
    MenuItem("Butter Toast", "BT", 10f, TOAST),
    MenuItem("Jam Toast", "JT", 10f, TOAST),
    MenuItem("Peanut But Toast", "PBT", 10f, TOAST),
    MenuItem("Cheese Toast", "CHT", 25f, TOAST),
    MenuItem("Honey Toast", "HT", 10f, TOAST),
    MenuItem("Butter Honey Toast", "BHT", 20f, TOAST),
    MenuItem("Choco Toast", "CHOC T", 15f, TOAST),

    //GRILLED
    MenuItem("French Toast", "FT", 40f, BREAKFAST),
    MenuItem("Ragi Pancake", "RAG PAN", 40f, BREAKFAST),
    MenuItem("Pancake", "PAN", 40f, BREAKFAST),
    MenuItem("Hash Brown", "HASH", 20f, BREAKFAST),
    MenuItem("Vegan Omelet", "VN OM", 40f, BREAKFAST),

    MenuItem("Oatmeal", "OAT", 30f, PORRIDGE),
    MenuItem("Ragi Porridge", "POR", 30f, PORRIDGE),

    MenuItem("Jam", "+JAM", 10f, TOPPING),
    MenuItem("Peanut Butter", "+PB", 10f, TOPPING),
    MenuItem("Butter", "+BUTT", 10f, TOPPING),
    MenuItem("Honey", "+HON", 10f, TOPPING),
    MenuItem("Choco Spread", "+CHOC", 15f, TOPPING),

    MenuItem("Bread", "BREAD", 3f, "Side"),
    MenuItem("Butter", "BUTT", 10f, "Side"),
    MenuItem("Capsicum", "CAP", 10f, "Side"),
    MenuItem("Carrot", "CARROT", 10f, "Side"),
    MenuItem("Honey", "HON", 10f, "Side"),
    MenuItem("Jam", "JAM", 10f, "Side"),
    MenuItem("Sliced Cheese", "SL CH", 20f, "Side"),
    MenuItem("Choco Spread", "CHOC", 15f, "Side"),
    MenuItem("Peanut Butter", "PB", 10f, "Side"),
    MenuItem("Pesto", "PSTO", 30f, "Side"),
    MenuItem("Raw Egg", "RAW EGG", 15f, "Side"),
    MenuItem("Ketchup", "KETUP", 5f, "Side"),
    MenuItem("Olive", "OLV", 30f, "Side"),
    MenuItem("Onion", "ON", 5f, "Side"),
    MenuItem("Fresh Tomato", "FRT", 10f, "Side"),
    MenuItem("Tomato Sauce", "TOM", 10f, "Side"),
    MenuItem("Grated Cheese", "CH", 20f, "Side")
)

val DEFAULT_LUNCH_DINNER_MENU: List<MenuItem> = listOf(
    MenuItem("Cheese Pizza", "CH PZA", 100f, PIZZA),
    MenuItem("Olive Pizza", "OLV PZA", 135f, PIZZA),
    MenuItem("Veg Pizza", "VEG PZA", 125f, PIZZA),
    MenuItem("Vegan Veg Pizza", "VN VEG PZA", 100f, PIZZA),
    MenuItem("Gourmet Pizza", "GM PZA", 150f, PIZZA),
    MenuItem("Vegan Gourmet PZA", "VN GM PZA", 125f, PIZZA),
    MenuItem("Pesto Pizza", "PSTO PZA", 125f, PIZZA),
    MenuItem("Paneer Pizza", "PAN PZA", 150f, PIZZA),
    MenuItem("Mediter. Pizza", "MED PZA", 125f, PIZZA),

    //grilled
    MenuItem("Grilled Cheese", "GC", 40f, SANDWICH),
    MenuItem("Egg Cheese Sand", "ECS", 60f, SANDWICH),
    MenuItem("Omelet Fries", "OMS", 50f, SANDWICH),
    MenuItem("Deluxe Grill CH", "DXL GC", 75f, SANDWICH),
    MenuItem("Grilled Ch FRT", "GC FRT", 45f, SANDWICH),

    MenuItem("Veggie Burger", "VB", 40f, BURGER),
    MenuItem("Cheese Burger", "CB", 60f, BURGER),
    MenuItem("Patty", "PATTY", 30f, BURGER),
    MenuItem("Deluxe Burger", "DLXB", 80f, BURGER),
    MenuItem("Pesto Burger", "PSTOB", 70f, BURGER),

    MenuItem("Egg Sunny Side Up", "FE SSU", 20f, EGGS),
    MenuItem("Fried Egg Overhard", "FE OH", 20f, EGGS),
    MenuItem("Plain Omelet", "OM", 40f, EGGS),
    MenuItem("Veggie Omelet", "VEG OM", 60f, EGGS),
    MenuItem("Deluxe Omelet", "DXL OM", 115f, EGGS),
    MenuItem("Cheese Omelet", "OM CH", 60f, EGGS),
    MenuItem("Fresh Tomato Omelet", "OM FRT", 45f, EGGS),
    MenuItem("Pesto Omelet", "PSTO OM", 70f, EGGS),
    MenuItem("Olive Omelet", "OLV OM", 75f, EGGS),
    MenuItem("Veggie Cheese Omelet", "VEG CH OM", 80f, EGGS),
    MenuItem("Gourmet Omelet", "GM OM", 110f, EGGS),
    MenuItem("Onion Omelet", "ON OM", 45f, EGGS),

    MenuItem("Toast", "T", 3f, TOAST),
    MenuItem("Butter Jam Toast", "BJT", 15f, TOAST),
    MenuItem("Butter Toast", "BT", 10f, TOAST),
    MenuItem("Jam Toast", "JT", 10f, TOAST),
    MenuItem("Peanut But Toast", "PBT", 10f, TOAST),
    MenuItem("Cheese Toast", "CHT", 25f, TOAST),
    MenuItem("Honey Toast", "HT", 10f, TOAST),
    MenuItem("Butter Honey Toast", "BHT", 20f, TOAST),
    MenuItem("Choco Toast", "CHOC T", 15f, TOAST),

    MenuItem("Plain Pasta", "PL PSTA", 30f, "Pasta"),
    MenuItem("Pasta Tomato Sauce", "PSTA TOM", 60f, "Pasta"),
    MenuItem("Pasta Tomato Cheese", "PAST TOM CH", 80f, "Pasta"),
    MenuItem("Pasta All Soy", "PAST ALL SOY", 90f, "Pasta"),
    MenuItem("Pesto Pasta", "PSTO PAST", 65f, "Pasta"),
    MenuItem("Vegan Gourmet Pasta", "VN GM PAST", 100f, "Pasta"),
    MenuItem("Deluxe Pasta", "DLX PAST", 115f, "Pasta"),

    MenuItem("Sm Fries", "SMFF", 30f, "Fries"),
    MenuItem("Lg Fries", "LGFF", 60f, "Fries"),
    MenuItem("Sm Fries Salt", "SMSAFF", 35f, "Fries"),
    MenuItem("Lg Fries Salt", "LGSAFF", 70f, "Fries"),

    MenuItem("Simple Salad", "SAL", 30f, "Salad"),
    MenuItem("Sprout Salad", "SPRSAL", 50f, "Salad"),
    MenuItem("Deluxe Salad", "DXL SAL", 90f, "Salad"),
    MenuItem("Pesto Salad", "PSTO SAL", 80f, "Salad"),
    MenuItem("Quinoa Salad", "QNA SAL", 90f, "Salad"),

    MenuItem("Kitcheri", "KITCH", 30f, "Kitcheri"),
    MenuItem("Steamed Veg", "ST VEG", 30f, "Kitcheri"),
    MenuItem("Quinoa", "QNA", 100f, "Kitcheri"),
    MenuItem("Steamed Broccoli", "ST BROC", 100f, "Kitcheri"),
    MenuItem("Hummus", "HUM", 30f, "Kitcheri"),

    MenuItem("Bread", "BREAD", 3f, "Side"),
    MenuItem("Butter", "BUTT", 10f, "Side"),
    MenuItem("Capsicum", "CAP", 10f, "Side"),
    MenuItem("Carrot", "CARROT", 10f, "Side"),
    MenuItem("Honey", "HON", 10f, "Side"),
    MenuItem("Jam", "JAM", 10f, "Side"),
    MenuItem("Sliced Cheese", "SL CH", 20f, "Side"),
    MenuItem("Mustard", "MRD", 10f, "Side"),
    MenuItem("Choco Spread", "CHOC", 15f, "Side"),
    MenuItem("Peanut Butter", "PB", 10f, "Side"),
    MenuItem("Patty", "PATTY", 30f, "Side"),
    MenuItem("Cucumber", "CUKE", 10f, "Side"),
    MenuItem("Pesto", "PSTO", 30f, "Side"),
    MenuItem("Raw Egg", "RAW EGG", 15f, "Side"),
    MenuItem("Gomasio", "GOMAZ", 10f, "Side"),
    MenuItem("Ketchup", "KETUP", 5f, "Side"),
    MenuItem("Olive", "OLV", 30f, "Side"),
    MenuItem("Onion", "ON", 5f, "Side"),
    MenuItem("Pickle", "PKL", 10f, "Side"),
    MenuItem("Dressing", "DRS", 10f, "Side"),
    MenuItem("Soya", "SOYA", 10f, "Side"),
    MenuItem("Sprouts", "SPRTS", 10f, "Side"),
    MenuItem("Fresh Tomato", "FRT", 10f, "Side"),
    MenuItem("Tomato Sauce", "TOM", 10f, "Side"),
    MenuItem("Bun", "BUN", 10f, "Side"),
    MenuItem("Grated Cheese", "CH", 20f, "Side"),
    MenuItem("Mayo", "MAYO", 10f, "Side")
)