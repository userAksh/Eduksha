package com.dobi.app.data

import com.dobi.app.domain.model.McqQuestion

object CuratedQuestionBank {

    private val sci6_u01_food_where_does_it_come_from = listOf(
        McqQuestion(
            id = "sci6_u01_q1",
            text = "Plants that we grow only for one season and use as food are called?",
            options = listOf("Crops", "Weeds", "Shrubs", "Saplings"),
            correctIndex = 0,
            explanation = "Plants of the same kind grown together on a large scale for food are called crops."
        ),
        McqQuestion(
            id = "sci6_u01_q2",
            text = "Honey is mainly produced by which of these?",
            options = listOf("Ants", "Honey bees", "Butterflies", "Spiders"),
            correctIndex = 1,
            explanation = "Honey bees collect nectar from flowers and convert it into honey inside their hive."
        ),
        McqQuestion(
            id = "sci6_u01_q3",
            text = "Animals that eat only plants are called?",
            options = listOf("Carnivores", "Herbivores", "Omnivores", "Scavengers"),
            correctIndex = 1,
            explanation = "Cow, goat, deer and rabbit eat only plants — they are called herbivores."
        ),
        McqQuestion(
            id = "sci6_u01_q4",
            text = "Which part of the wheat plant do we eat?",
            options = listOf("Roots", "Stem", "Seeds", "Leaves"),
            correctIndex = 2,
            explanation = "Wheat grains are seeds. They are ground into flour to make rotis and bread."
        ),
        McqQuestion(
            id = "sci6_u01_q5",
            text = "From which plant do we get sugar?",
            options = listOf("Wheat", "Sugarcane", "Mustard", "Cotton"),
            correctIndex = 1,
            explanation = "Sugar is made from the juice of the sugarcane plant."
        ),
        McqQuestion(
            id = "sci6_u01_q6",
            text = "Which of these animals provides milk?",
            options = listOf("Cow", "Hen", "Crocodile", "Snake"),
            correctIndex = 0,
            explanation = "Cow, buffalo and goat are common milk-giving animals in India."
        ),
        McqQuestion(
            id = "sci6_u01_q7",
            text = "Pulses such as moong and chana are obtained from which group of plants?",
            options = listOf("Cereals", "Legumes", "Oilseeds", "Grasses"),
            correctIndex = 1,
            explanation = "Pulses come from leguminous plants — they store protein in their seeds."
        ),
        McqQuestion(
            id = "sci6_u01_q8",
            text = "The edible part of a carrot is its?",
            options = listOf("Stem", "Fruit", "Root", "Leaf"),
            correctIndex = 2,
            explanation = "We eat the underground root of the carrot plant — it stores food for the plant."
        ),
        McqQuestion(
            id = "sci6_u01_q9",
            text = "Tigers and lions are examples of?",
            options = listOf("Herbivores", "Carnivores", "Omnivores", "Producers"),
            correctIndex = 1,
            explanation = "Animals that eat other animals are called carnivores."
        ),
        McqQuestion(
            id = "sci6_u01_q10",
            text = "Humans, who eat both plants and animals, are called?",
            options = listOf("Herbivores", "Carnivores", "Omnivores", "Decomposers"),
            correctIndex = 2,
            explanation = "Animals that eat both plants and animal products are called omnivores."
        )
    )

    private val sci6_u02_components_of_food = listOf(
        McqQuestion(
            id = "sci6_u02_q1",
            text = "Which nutrient mainly provides energy to our body?",
            options = listOf("Proteins", "Carbohydrates", "Vitamins", "Minerals"),
            correctIndex = 1,
            explanation = "Carbohydrates are the body's main source of energy. Rice, wheat and potatoes are rich in them."
        ),
        McqQuestion(
            id = "sci6_u02_q2",
            text = "Proteins are also known as?",
            options = listOf("Energy-giving foods", "Body-building foods", "Protective foods", "Junk foods"),
            correctIndex = 1,
            explanation = "Proteins help in the growth and repair of our body, so they are called body-building foods."
        ),
        McqQuestion(
            id = "sci6_u02_q3",
            text = "Scurvy is caused by the deficiency of which vitamin?",
            options = listOf("Vitamin A", "Vitamin B", "Vitamin C", "Vitamin D"),
            correctIndex = 2,
            explanation = "Lack of Vitamin C causes scurvy — bleeding gums and slow healing of wounds. Citrus fruits prevent it."
        ),
        McqQuestion(
            id = "sci6_u02_q4",
            text = "Goitre is caused by deficiency of which mineral?",
            options = listOf("Iron", "Calcium", "Iodine", "Phosphorus"),
            correctIndex = 2,
            explanation = "Iodine deficiency leads to goitre — a swelling in the neck. Iodised salt helps prevent it."
        ),
        McqQuestion(
            id = "sci6_u02_q5",
            text = "Which of these is the richest source of fats?",
            options = listOf("Butter", "Spinach", "Apples", "Bread"),
            correctIndex = 0,
            explanation = "Butter, ghee, and oils are rich in fats — they give a lot of energy in a small quantity."
        ),
        McqQuestion(
            id = "sci6_u02_q6",
            text = "Roughage in our food is provided mainly by?",
            options = listOf("Sugar", "Meat", "Fibre from plants", "Salt"),
            correctIndex = 2,
            explanation = "Roughage is plant fibre — it does not give energy but helps the body get rid of waste."
        ),
        McqQuestion(
            id = "sci6_u02_q7",
            text = "Rickets is caused by lack of which vitamin?",
            options = listOf("Vitamin A", "Vitamin B", "Vitamin C", "Vitamin D"),
            correctIndex = 3,
            explanation = "Vitamin D helps absorb calcium for strong bones. Without it, children can develop rickets."
        ),
        McqQuestion(
            id = "sci6_u02_q8",
            text = "Anaemia is caused by deficiency of which mineral?",
            options = listOf("Iron", "Iodine", "Calcium", "Sodium"),
            correctIndex = 0,
            explanation = "Iron is needed to make haemoglobin in blood. Lack of iron causes weakness called anaemia."
        ),
        McqQuestion(
            id = "sci6_u02_q9",
            text = "Which of these is a balanced diet?",
            options = listOf(
                "Only rice and water",
                "Rice, dal, vegetables, fruits and milk",
                "Only chocolates",
                "Only fried food"
            ),
            correctIndex = 1,
            explanation = "A balanced diet has all nutrients in the right amounts — carbs, proteins, fats, vitamins and minerals."
        ),
        McqQuestion(
            id = "sci6_u02_q10",
            text = "Which simple test is used to detect starch in food?",
            options = listOf("Burning test", "Iodine test", "Litmus test", "Salt test"),
            correctIndex = 1,
            explanation = "A drop of iodine on a starchy food turns blue-black — that's how we test for starch."
        )
    )

    private val sci6_c001_sources_plants = listOf(
        McqQuestion("sci6_c001_q1",
            "Which of these is the edible part of a carrot plant?",
            listOf("Leaf", "Root", "Flower", "Fruit"), 1,
            "We eat the underground root of a carrot — it stores food for the plant."),
        McqQuestion("sci6_c001_q2",
            "From which plant do we get the spice 'ginger'?",
            listOf("Roots", "Stems", "Leaves", "Flowers"), 1,
            "Ginger is an underground stem we use as a spice."),
        McqQuestion("sci6_c001_q3",
            "Wheat that we eat is which part of the plant?",
            listOf("Stem", "Root", "Leaf", "Seed"), 3,
            "Wheat grains are seeds that are ground to make flour."),
        McqQuestion("sci6_c001_q4",
            "Cauliflower belongs to which plant part?",
            listOf("Flower", "Fruit", "Root", "Stem"), 0,
            "Cauliflower is a flower head we eat as a vegetable."),
        McqQuestion("sci6_c001_q5",
            "Which of these plant parts gives us spinach?",
            listOf("Leaves", "Roots", "Seeds", "Fruits"), 0,
            "Spinach is a leafy vegetable — we eat its leaves.")
    )

    private val sci6_c002_sources_animals = listOf(
        McqQuestion("sci6_c002_q1",
            "Which of these animals gives us honey?",
            listOf("Cow", "Honey bee", "Hen", "Goat"), 1,
            "Honey bees collect nectar from flowers and turn it into honey."),
        McqQuestion("sci6_c002_q2",
            "Eggs are usually obtained from which animal?",
            listOf("Goat", "Cow", "Hen", "Dog"), 2,
            "Hens lay eggs that are a common protein source."),
        McqQuestion("sci6_c002_q3",
            "Which of these is an animal product?",
            listOf("Rice", "Butter", "Wheat", "Sugar"), 1,
            "Butter is made from milk, which comes from animals like cows."),
        McqQuestion("sci6_c002_q4",
            "Curd is made from?",
            listOf("Milk", "Eggs", "Honey", "Meat"), 0,
            "Curd is set by adding a starter to warm milk."),
        McqQuestion("sci6_c002_q5",
            "Which of these animals is reared mainly for milk?",
            listOf("Buffalo", "Tiger", "Crocodile", "Snake"), 0,
            "Buffalo is one of the most common milk-giving animals in India.")
    )

    private val sci6_c003_food_habits = listOf(
        McqQuestion("sci6_c003_q1",
            "Animals that eat only plants are called?",
            listOf("Carnivores", "Herbivores", "Omnivores", "Decomposers"), 1,
            "Cow, deer and rabbit eat only plants — they are herbivores."),
        McqQuestion("sci6_c003_q2",
            "Which of these is a carnivore?",
            listOf("Goat", "Cow", "Lion", "Rabbit"), 2,
            "Lions hunt and eat other animals — they are carnivores."),
        McqQuestion("sci6_c003_q3",
            "Humans eat both plants and animal products. We are?",
            listOf("Herbivores", "Carnivores", "Omnivores", "Insectivores"), 2,
            "Animals that eat both plants and animals are called omnivores."),
        McqQuestion("sci6_c003_q4",
            "Which of these is an omnivore?",
            listOf("Cow", "Bear", "Tiger", "Deer"), 1,
            "Bears eat fruits, honey AND fish — they're omnivores."),
        McqQuestion("sci6_c003_q5",
            "An eagle eats other animals. It is a?",
            listOf("Herbivore", "Carnivore", "Omnivore", "Producer"), 1,
            "Eagles hunt smaller animals — they are carnivores.")
    )

    private val sci6_c004_edible_plant_parts = listOf(
        McqQuestion("sci6_c004_q1",
            "Potato is which part of the plant?",
            listOf("Root", "Stem", "Leaf", "Fruit"), 1,
            "Potato is an underground stem (a tuber)."),
        McqQuestion("sci6_c004_q2",
            "Which of these is a seed we eat?",
            listOf("Cabbage", "Radish", "Mustard", "Spinach"), 2,
            "Mustard seeds are used as spice and to extract oil."),
        McqQuestion("sci6_c004_q3",
            "Sugarcane is which plant part?",
            listOf("Stem", "Root", "Fruit", "Leaf"), 0,
            "We crush the sugarcane stem to get sweet juice."),
        McqQuestion("sci6_c004_q4",
            "Which of these is a fruit we eat?",
            listOf("Carrot", "Mango", "Spinach", "Sugarcane"), 1,
            "Mango is a sweet, juicy fruit of the mango tree."),
        McqQuestion("sci6_c004_q5",
            "Cabbage is which plant part?",
            listOf("Root", "Flower", "Leaf", "Stem"), 2,
            "A cabbage is a tight ball of leaves we eat as a vegetable.")
    )

    private val sci6_c005_nutrients = listOf(
        McqQuestion("sci6_c005_q1",
            "How many main nutrients do we usually count in food?",
            listOf("Three", "Four", "Five", "Seven"), 2,
            "The five main nutrients: carbohydrates, proteins, fats, vitamins, and minerals."),
        McqQuestion("sci6_c005_q2",
            "Roughage is also known as?",
            listOf("Fibre", "Fat", "Mineral", "Vitamin"), 0,
            "Roughage is dietary fibre that helps our digestion."),
        McqQuestion("sci6_c005_q3",
            "Which of these is needed in small amounts but very important?",
            listOf("Carbohydrates", "Vitamins", "Fats", "Water"), 1,
            "Vitamins are micronutrients — needed in tiny amounts but essential."),
        McqQuestion("sci6_c005_q4",
            "Which nutrient is the body's main source of quick energy?",
            listOf("Proteins", "Vitamins", "Carbohydrates", "Minerals"), 2,
            "Carbohydrates are broken down quickly to give energy."),
        McqQuestion("sci6_c005_q5",
            "Water in our body helps with?",
            listOf("Digestion only", "Cooling only", "Many body functions", "Just thirst"), 2,
            "Water helps with digestion, temperature control, and removing waste.")
    )

    private val byUnit: Map<String, List<McqQuestion>> = mapOf(
        "sci6_u01" to sci6_u01_food_where_does_it_come_from,
        "sci6_u02" to sci6_u02_components_of_food
    )

    private val byConcept: Map<String, List<McqQuestion>> = mapOf(
        "sci6_c001" to sci6_c001_sources_plants,
        "sci6_c002" to sci6_c002_sources_animals,
        "sci6_c003" to sci6_c003_food_habits,
        "sci6_c004" to sci6_c004_edible_plant_parts,
        "sci6_c005" to sci6_c005_nutrients
    )

    fun forUnit(unitId: String): List<McqQuestion> = byUnit[unitId].orEmpty()

    fun forConcept(conceptId: String): List<McqQuestion> = byConcept[conceptId].orEmpty()
}
