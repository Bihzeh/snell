package gg.snell.launcher.update

/** Minimal semantic version (major.minor.patch); tolerant of a leading "v". */
data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val RE = Regex("""v?(\d+)\.(\d+)(?:\.(\d+))?""")
        fun parse(text: String): SemVer? {
            val m = RE.find(text.trim()) ?: return null
            val (a, b, c) = m.destructured
            return SemVer(a.toInt(), b.toInt(), c.ifEmpty { "0" }.toInt())
        }
    }
}
