package edu.amrita.amritacafe.quotes

object AmmaQuotes {

    // 108 authentic, concise quotes from Sri Mata Amritanandamayi Devi (Amma)
    // Sourced from "Awaken Children!", "From Amma's Heart", "Lead Us to the Light", and Amritapuri teachings.
    val quotes = listOf(
        "Where there is love, there is no distance.",
        "Love is the only medicine that can heal the wounds of the world.",
        "Let us be content with the minimum and give the maximum to others.",
        "In the end, only love and kindness remain.",
        "If you take one step towards God, God will take ten steps towards you.",
        "Patience is the foundation of life. You need patience to enjoy the beauty of life.",
        "Compassion is the language that the deaf can hear and the blind can see.",
        "Real love is the complete absence of any negative feelings towards anyone.",
        "Smile from the heart; that smile can dispel darkness everywhere.",
        "Let our words be soft and sweet, carrying love to all.",
        "Selfless service to suffering humanity is true worship of God.",
        "Grace is openness. Open your heart to the divine flow.",
        "Whatever happens, accept it with a smile and surrender to God.",
        "We are not isolated islands; we are all connected links in a chain.",
        "When we look into the mirror of our conscience, we find peace.",
        "True beauty lies in the goodness of the heart.",
        "Forgiveness is the highest ornament of a pure mind.",
        "Nature is our mother; when we protect nature, nature protects us.",
        "Happiness does not come from what you get, but from what you give.",
        "Be like a candle, giving light and warmth to everyone around you.",
        "True strength comes from humility and surrender.",
        "Peace of mind is the greatest wealth in this world.",
        "A loving heart is the true temple of God.",
        "Do every action with love, dedication, and mindfulness.",
        "See God in every living being and serve all with respect.",
        "The fragrance of flowers spreads with the wind, but goodness spreads in all directions.",
        "Let your life be an offering of love and service.",
        "When you give joy to another, that joy returns to you multiplied.",
        "Life is a sweet melody when lived in harmony with love.",
        "Through prayer and love, all obstacles can be overcome.",
        "A peaceful mind can face any challenge with courage.",
        "The best prayer is a pure heart and loving actions.",
        "Where there is faith, there is victory.",
        "Kindness is the golden chain by which society is bound together.",
        "Live in the present moment with gratitude in your heart.",
        "God's grace flows constantly, like the sun giving light.",
        "Let go of resentment, and embrace the lightness of forgiveness.",
        "A drop of water in the ocean becomes the ocean; surrender your ego.",
        "True spirituality begins with compassion for all beings.",
        "Keep a pure thought in your mind, and peace will naturally follow.",
        "In giving, we receive; in loving, we are loved.",
        "Pure love expects nothing in return; it only knows how to give.",
        "Life is a journey from the head to the heart.",
        "The tree gives shade even to the one who comes to cut it down.",
        "Patience and perseverance conquer all difficulties.",
        "Contentment is the greatest treasure one can possess.",
        "Let your thoughts be pure, words kind, and actions noble.",
        "The divine light shines equally within every creation.",
        "When love fills the heart, fear disappears.",
        "Silence of the mind is the gateway to inner bliss.",
        "Every moment is a gift; use it to do good.",
        "Cultivate gratitude, and life will blossom with joy.",
        "A gentle word can heal a broken heart.",
        "Serve without expectation, love without condition.",
        "When you smile, you bring sunshine into another's life.",
        "True wisdom is knowing how to live in peace with others.",
        "Do not worry about the future; live righteously today.",
        "Faith gives you wings to fly above life's storms.",
        "Love is the bridge between heaven and earth.",
        "To love is to see the sacred in everything.",
        "Let devotion guide your feelings, and knowledge guide your intellect.",
        "Do your duty with joy, and let God take care of the results.",
        "Inner peace is the foundation for world peace.",
        "Never lose faith; grace is always working behind the scenes.",
        "Fill each moment with love, and life becomes a celebration.",
        "A small act of kindness can change someone's entire day.",
        "Keep your mind like a clean mirror, reflecting only the pure light.",
        "True greatness lies in making others feel valued and loved.",
        "Simplicity is the sign of spiritual maturity.",
        "The mind is like a wild elephant; tame it with love and mindfulness.",
        "God is love, and love is God.",
        "Look upon all as your own brothers and sisters.",
        "True wealth is a peaceful mind and a loving heart.",
        "When you see suffering, let your heart melt and your hands serve.",
        "Prayer is the telephone line connecting you directly to God.",
        "Live lightly, love deeply, and laugh often.",
        "When you forgive, you liberate your own soul.",
        "Work is worship when performed with devotion and selfless intent.",
        "Let peace begin with you.",
        "Beauty is not in the face; beauty is a light in the heart.",
        "Be patient with yourself and patient with others.",
        "The secret of happiness is detachment from results.",
        "True happiness is born from selfless service.",
        "A generous heart never lacks anything.",
        "The sun shines for everyone; let your love be universal.",
        "Speak truth softly, and with compassion.",
        "Through selfless love, ordinary actions become sacred offerings.",
        "Always keep a child-like innocence and wonder in your heart.",
        "The greatest strength is the power of love.",
        "Do not dwell on mistakes; learn and move forward with courage.",
        "A compassionate heart is the greatest blessing.",
        "Love knows no boundaries, conditions, or limits.",
        "Let your heart overflow with gratitude for life's simple gifts.",
        "A calm mind is a powerful mind.",
        "Serve humanity, for humanity is the manifestation of God.",
        "Even a smile given to someone can be a great spiritual practice.",
        "Live in harmony with nature and all living beings.",
        "When you hold onto God, you hold onto infinite peace.",
        "Kindness costs nothing, but enriches everyone.",
        "Open your arms to the world with love and acceptance.",
        "The light within you can illuminate the darkest night.",
        "Purity of thought leads to peace of mind.",
        "Let go of anger before it burns your own peace.",
        "Treat every being as an expression of the Divine.",
        "Love is the essence of life; without it, life is dry.",
        "When compassion awakens, heaven descends onto earth.",
        "Always remember: God's love for you is boundless and eternal.",
        "Love, serve, and be happy."
    )

    fun getQuoteForOrder(orderNumber: Int): String {
        val index = (orderNumber.coerceAtLeast(1) - 1) % quotes.size
        return quotes[index]
    }

    fun getFormattedLines(orderNumber: Int, maxCharsPerLine: Int): List<String> {
        val rawQuote = getQuoteForOrder(orderNumber)
        val quote = "\"$rawQuote\""
        val words = quote.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            if (current.isEmpty()) {
                current.append(word)
            } else if (current.length + 1 + word.length <= maxCharsPerLine) {
                current.append(" ").append(word)
            } else {
                lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) {
            lines.add(current.toString())
        }
        lines.add("- Amma")
        return lines
    }

    fun getRandomQuote(): String {
        return quotes.random()
    }
}
