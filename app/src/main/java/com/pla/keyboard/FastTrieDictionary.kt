package com.pla.keyboard

class FastTrieDictionary {

    private class TrieNode {
        val children = HashMap<Char, TrieNode>()
        var isWord = false
        var frequency = 0
    }

    private val root = TrieNode()

    private val coreVocabulary = listOf(
        "saya" to 100, "yang" to 98, "dan" to 96, "untuk" to 95, "dengan" to 94,
        "tidak" to 93, "ini" to 92, "itu" to 91, "akan" to 90, "pada" to 89,
        "dari" to 88, "dalam" to 87, "bisa" to 86, "ada" to 85, "juga" to 84,
        "sudah" to 83, "sangat" to 82, "terima" to 81, "kasih" to 81, "selamat" to 80,
        "pagi" to 79, "siang" to 78, "sore" to 77, "malam" to 76, "halo" to 75,
        "apa" to 74, "kabar" to 73, "baik" to 72, "hari" to 71, "kerja" to 70,
        "aplikasi" to 69, "android" to 68, "keyboard" to 67, "cepat" to 66, "nyaman" to 65,
        "tinggi" to 64, "layar" to 63, "suara" to 62, "getar" to 61, "spasi" to 60,
        "tolong" to 59, "bantu" to 58, "kirim" to 57, "pesan" to 56, "nomor" to 55,
        "rumah" to 54, "jalan" to 53, "waktu" to 52, "sekarang" to 51, "besok" to 50,
        "the" to 85, "and" to 84, "you" to 83, "that" to 82, "have" to 81,
        "for" to 80, "with" to 79, "this" to 78, "good" to 77, "thank" to 76
    )

    init {
        for ((word, freq) in coreVocabulary) {
            insert(word, freq)
        }
    }

    fun insert(word: String, freq: Int = 50) {
        var current = root
        for (ch in word.lowercase()) {
            current = current.children.getOrPut(ch) { TrieNode() }
        }
        current.isWord = true
        current.frequency = maxOf(current.frequency, freq)
    }

    fun getTopSuggestions(prefix: String, limit: Int = 3): List<String> {
        if (prefix.isBlank()) return listOf("saya", "yang", "dan")
        var current = root
        for (ch in prefix.lowercase()) {
            current = current.children[ch] ?: return listOf(prefix)
        }

        val matches = mutableListOf<Pair<String, Int>>()
        collectWords(current, StringBuilder(prefix.lowercase()), matches)
        val sorted = matches.sortedByDescending { it.second }.map { it.first }.take(limit)
        return if (sorted.isEmpty()) listOf(prefix) else sorted
    }

    /**
     * Pencocokan jalur usapan jari (Glide/Swipe Typing) berdasarkan huruf awal, akhir, & urutan
     */
    fun matchSwipeTrail(trailLetters: String): String? {
        if (trailLetters.length < 2) return null
        val firstChar = trailLetters.first()
        val lastChar = trailLetters.last()

        var bestWord: String? = null
        var bestScore = -1

        for ((word, freq) in coreVocabulary) {
            if (word.length < 2) continue
            if (word.first() == firstChar && word.last() == lastChar) {
                val subseqScore = countSubsequenceMatches(word, trailLetters) * 20 + freq
                if (subseqScore > bestScore) {
                    bestScore = subseqScore
                    bestWord = word
                }
            }
        }
        return bestWord
    }

    private fun countSubsequenceMatches(targetWord: String, trail: String): Int {
        var wIdx = 0
        var tIdx = 0
        var hits = 0
        while (wIdx < targetWord.length && tIdx < trail.length) {
            if (targetWord[wIdx] == trail[tIdx]) {
                hits++
                wIdx++
            }
            tIdx++
        }
        return hits
    }

    private fun collectWords(
        node: TrieNode,
        builder: StringBuilder,
        results: MutableList<Pair<String, Int>>
    ) {
        if (results.size >= 15) return
        if (node.isWord) {
            results.add(builder.toString() to node.frequency)
        }
        for ((ch, child) in node.children) {
            builder.append(ch)
            collectWords(child, builder, results)
            builder.deleteCharAt(builder.length - 1)
        }
    }
}