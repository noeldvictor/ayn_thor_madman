package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The lesson suite.
 *
 * **Each test encodes something a fork in this fleet learned the hard way, so
 * that nobody in this repo has to learn it again.**
 *
 * These are deliberately cheap: no emulator, no device, no toolchain migration.
 * See shared_layer/UNIFICATION.md — a test is the cheapest form of unification
 * that resists being forgotten, and this project had almost none.
 */
class LessonTest {

    // ---------------------------------------------------- Vita3K's overlay lesson

    private fun inputs(
        controller: Boolean = false,
        on: Boolean = false,
        off: Boolean = false,
        elements: Boolean = true,
        editing: Boolean = false,
    ) = OverlayInputs(controller, on, off, elements, editing)

    @Test
    fun `a physical controller hides the touch overlay`() {
        // Vita3K solved this and nobody else did. The Thor has real buttons,
        // so an overlay drawn permanently over the game is wrong here.
        assertEquals(
            OverlayVisibility.HIDDEN,
            OverlayPolicy.resolve(inputs(controller = true)),
        )
        assertEquals(
            OverlayVisibility.SHOWN,
            OverlayPolicy.resolve(inputs(controller = false)),
        )
    }

    @Test
    fun `an explicit choice beats the hardware guess`() {
        // Never override a stated preference with an inference. Someone may
        // want the overlay with a controller attached; that is their call.
        assertEquals(
            OverlayVisibility.SHOWN,
            OverlayPolicy.resolve(inputs(controller = true, on = true)),
        )
        assertEquals(
            OverlayVisibility.HIDDEN,
            OverlayPolicy.resolve(inputs(controller = false, off = true)),
        )
    }

    @Test
    fun `editing keeps the overlay on screen whatever else says`() {
        // Both azahar and Vita3K kept an explicit edit mode through twelve
        // years of independent divergence. You cannot reposition what is hidden.
        assertEquals(
            OverlayVisibility.SHOWN,
            OverlayPolicy.resolve(inputs(controller = true, off = true, editing = true)),
        )
    }

    @Test
    fun `a backend that declares no elements has no overlay`() {
        // Cemu declares 21 elements. xenia declares none. A fixed overlay
        // would impose one console's controller on every backend.
        assertEquals(
            OverlayVisibility.HIDDEN,
            OverlayPolicy.resolve(inputs(elements = false, on = true)),
        )
    }

    // ------------------------------------------------------- rpcsx's ABI lesson

    @Test
    fun `the shell targets arm64-v8a and nothing else`() {
        // rpcsx measured this: adding x86_64 put 26 MiB compressed of
        // unreachable code into a 96 MiB APK and DOUBLED the native compile.
        // Five forks in the fleet still pay that cost. Vita3K's build actually
        // FAILS on the ABI the Thor cannot run.
        //
        // This is a build guard in test form: the cheapest way to stop the
        // shell ever acquiring the same problem.
        val gradle = File("build.gradle.kts")
        assertTrue("expected app/build.gradle.kts beside the test working dir", gradle.exists())
        val text = gradle.readText()

        val forbidden = listOf("x86_64", "armeabi-v7a", "\"x86\"", "riscv64")
        for (abi in forbidden) {
            assertTrue(
                "$abi must not appear in the shell's build file. The Thor is arm64-v8a; " +
                    "see research_log/20260823_0030_abi_waste.md",
                !text.contains(abi),
            )
        }
        assertTrue(
            "the shell must state arm64-v8a explicitly rather than defaulting to every ABI",
            text.contains("arm64-v8a"),
        )
    }
}
