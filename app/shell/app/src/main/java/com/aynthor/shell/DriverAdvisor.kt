package com.aynthor.shell

/**
 * Whether a GPU driver package is safe to load on this device.
 *
 * **The idea is rpcsx's `GpuDriverAdvisor`. The code is not.** rpcsx is
 * GPL-2.0-only and cannot share this binary, but a design is not expression —
 * see shared_layer/UNIFICATION.md section 4. This is a reimplementation from
 * the shape, not a copy.
 *
 * **The hazard is real and present on this device.** `CLAUDE.md` records that
 * `Turnip_Gen8_V33.zip` and `a8xx-gen8-V24.zip` are sitting in storage. **Those
 * target a8xx — the Snapdragon 8 Elite generation. The Thor is a740, which is
 * a7xx.** Loading one is a wrong-GPU driver.
 *
 * **The clever part worth stealing is decoding the marketing name.** Qualcomm
 * sells a8xx as "Gen 8", so a file called `Turnip_Gen8_V33` claims a8xx while
 * never saying so. A picker that only matched `a8xx` would miss it.
 *
 * **It advises rather than lists.** rpcsx's insight: the useful output is a
 * verdict with a reason, not a directory of choices for a person to guess at.
 * That is [Foundation](../../../../../../../CLAUDE.md) point 4.
 */
enum class Verdict { COMPATIBLE, RISKY, INCOMPATIBLE }

data class Assessment(val verdict: Verdict, val reason: String)

object DriverAdvisor {

    /** This device. Measured, not detected: the app targets one device. */
    const val DEVICE_FAMILY = "a7xx"
    const val DEVICE_MODEL = 740

    /**
     * Turnip on a7xx needs a Mesa recent enough to support the part.
     * rpcsx encodes a per-family minimum; the value here is deliberately
     * conservative and should be revisited when the driver is pinned.
     */
    const val A7XX_MESA_MIN = 24.0

    // `(TM)` is OPTIONAL here, and that is a deliberate difference from rpcsx.
    // Its regex requires the literal `tm` because it parses the Vulkan renderer
    // string, which is literally "Adreno (TM) 740". This one also has to read
    // package FILENAMES, where a human writes "Adreno 740". Reimplementing
    // surfaced that the original quietly assumed one input format.
    private val ADRENO_MODEL = Regex("""adreno\s*(?:\(?tm\)?)?\s*(\d{3})""",
                                     RegexOption.IGNORE_CASE)
    private val FAMILY = Regex("""\ba([678])xx\b""", RegexOption.IGNORE_CASE)
    private val GEN = Regex("""\bgen\s*([0-9])\b""", RegexOption.IGNORE_CASE)
    private val MESA = Regex("""(?<![0-9.])(\d{2}\.\d+(?:\.\d+)?)(?![0-9])""")
    private val TURNIP = Regex("""turnip|freedreno|mesa""", RegexOption.IGNORE_CASE)

    /**
     * Separator characters become spaces before matching.
     *
     * **This step is load-bearing and it is easy to skip.** `_` is a word
     * character, so in `Turnip_Gen8_V33` there is no word boundary before
     * `Gen` and `gen` never matches — the driver that must be rejected sails
     * through as merely RISKY. rpcsx normalises for exactly this reason, and
     * reimplementing without it reproduced the bug. The test caught it.
     */
    private fun normalize(text: String): String =
        text.replace(Regex("""[^A-Za-z0-9.]+"""), " ")

    /**
     * Which GPU families a package name claims to target.
     *
     * Three ways a name can say it, and **a package often uses only the
     * marketing one**:
     *  - explicitly: `a7xx`
     *  - by model:   `Adreno 740` -> a7xx
     *  - by Qualcomm generation: `Gen 8` -> a8xx   <- the one that catches people
     */
    fun claimedFamilies(name: String): Set<String> {
        val text = normalize(name)
        val out = mutableSetOf<String>()
        FAMILY.findAll(text).forEach { out += "a${it.groupValues[1]}xx" }
        ADRENO_MODEL.findAll(text).forEach {
            it.groupValues[1].firstOrNull()?.let { d -> out += "a${d}xx" }
        }
        // "Gen 8" is Qualcomm marketing for the a8xx generation.
        GEN.findAll(text).forEach { out += "a${it.groupValues[1]}xx" }
        return out
    }

    fun mesaVersion(name: String): Double? =
        MESA.find(normalize(name))?.groupValues?.get(1)?.let {
            val parts = it.split(".")
            "${parts[0]}.${parts.getOrElse(1) { "0" }}".toDoubleOrNull()
        }

    fun isTurnip(name: String): Boolean = TURNIP.containsMatchIn(normalize(name))

    /**
     * Assess a driver package by its name.
     *
     * **Honest about its own limits, as rpcsx is.** AdrenoTools metadata
     * carries no target-GPU field, so this is a heuristic over a filename. It
     * is allowed to say RISKY, and saying RISKY is more useful than pretending
     * to know.
     */
    fun assess(name: String): Assessment {
        val claimed = claimedFamilies(name)

        if (claimed.isNotEmpty() && DEVICE_FAMILY !in claimed) {
            return Assessment(
                Verdict.INCOMPATIBLE,
                "targets ${claimed.sorted().joinToString("/")}; this device is $DEVICE_FAMILY " +
                    "(Adreno $DEVICE_MODEL)",
            )
        }

        if (!isTurnip(name)) {
            return Assessment(Verdict.RISKY, "not recognisably a Turnip or Mesa build")
        }

        val mesa = mesaVersion(name)
        if (mesa != null && mesa < A7XX_MESA_MIN) {
            return Assessment(
                Verdict.INCOMPATIBLE,
                "Mesa $mesa predates a7xx support; $A7XX_MESA_MIN or newer is required",
            )
        }

        if (claimed.isEmpty()) {
            return Assessment(
                Verdict.RISKY,
                "names no GPU family — AdrenoTools metadata carries no target field, so this " +
                    "cannot be verified from the package",
            )
        }

        return Assessment(Verdict.COMPATIBLE, "targets $DEVICE_FAMILY" +
            (mesa?.let { ", Mesa $it" } ?: ""))
    }
}
