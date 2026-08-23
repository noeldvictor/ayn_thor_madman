# eden build attempt: two missing host tools, neither a fork bug

**Goal: continue Phase 0.3 with the fourth Tier 1 fork.**

Session 2026-08-23 02:07.

**Result: eden does not build on this box. Both causes are host tools that are
not installed, and the second one is not shipped by the Android NDK at all.**

---

## Attempt 1: `pkg-config`

```
BUILD FAILED in 2m 36s
Task :app:configureCMakeRelWithDebInfo[arm64-v8a] FAILED
  Could NOT find PkgConfig (missing: PKG_CONFIG_EXECUTABLE)
  Call Stack: externals/ffmpeg/CMakeLists.txt:63 (find_package)
```

**Found one and retried.** vcpkg had already downloaded an msys2 containing
`pkg-config.exe`, at
`SteamPortableTools/toolchains/vcpkg/downloads/tools/msys2/.../mingw64/bin`.
Putting it on `PATH` cleared this error completely.

## Attempt 2: `glslangValidator`

```
BUILD FAILED in 1m 32s
CMake Error at src/video_core/host_shaders/CMakeLists.txt:90:
  Required program `glslangValidator` not found.
```

**It is not on this box, and the Android NDK does not ship it.** NDK 28.2's
`shader-tools/windows-x86_64/` contains:

```
glslc.exe  spirv-as  spirv-cfg  spirv-dis  spirv-link  spirv-opt
spirv-reduce  spirv-val
```

**`glslc`, not `glslangValidator`.** eden asks for the latter by name:

```cmake
find_program(GLSLANGVALIDATOR "glslangValidator")
if ("${GLSLANGVALIDATOR}" STREQUAL "GLSLANGVALIDATOR-NOTFOUND")
    message(FATAL_ERROR "Required program `glslangValidator` not found.")
```

**Not resolved, deliberately.** The fix is either a Vulkan SDK install or
building glslang from eden's own externals. **Installing a developer SDK on
somebody's machine is a system change beyond what this work was asked to do**,
so it is recorded as a prerequisite rather than performed.

## A distinction worth keeping

**eden translates guest shaders straight to SPIR-V** — `shader_recompiler/
backend/spirv/emit_spirv.cpp` — which is what
[`PATTERNS.md`](../shared_layer/PATTERNS.md) records.

**Its own host shaders are GLSL, compiled at build time.** Those are two
different things and the earlier survey only covered the first. **"eden emits
SPIR-V directly" is true of guest shaders and false of its own.**

## The third failure class

Four forks attempted, and the obstacles keep being different:

| Fork | Result | Obstacle |
| --- | --- | --- |
| melonDS | **built**, 15 min 27 s | — |
| azahar | **built**, 14 min 33 s | — |
| Vita3K | failed | **its own recipe**, then an ABI the device cannot run |
| **eden** | failed | **host tools nobody documented** |

**Nothing has yet failed inside an emulator.** Every obstacle has been in the
periphery: a recipe, an ABI list, a plugin version, a missing host tool.

**And the host-tool class is the one that matters most for the agentic
thesis**, because it is invisible until somebody tries. A fork that needs
`pkg-config` and `glslangValidator` cannot be built by an agent on a machine
that has neither, and **nothing in the fork says so.**

## What should exist

**A prerequisites list per fork**, checked before a build rather than
discovered during one. The fleet lint is the natural home: it already reads
build files, and "does this box have the tools this fork needs" is the same
kind of question.

**Known host tools so far:** JDK 17 and 21, the Android SDK and NDK, cargo and
rustup (melonDS), vcpkg (Vita3K), `pkg-config` (eden), `glslangValidator`
(eden), and `git` with `core.longpaths` on Windows.
