#!/usr/bin/env python3
"""Count the Vulkan device extensions each fork asks the driver for.

The Thor has one GPU and one pinned driver, so every fork is negotiating with a
device whose answer is known ahead of time. This measures how far apart their
answers are.

    python tools/vk_capability_census.py
    python tools/vk_capability_census.py --min-forks 4   # what the fleet agrees on
    python tools/vk_capability_census.py --orphans       # asked for by one fork only
    python tools/vk_capability_census.py --check VK_KHR_synchronization2 ...

It reads. It never modifies a fork.

WHY A FIXED FILE LIST AND NOT A SEARCH: a fleet-wide grep for extension names
returns the vendored Vulkan headers, which declare every extension that exists.
ARMSX2 read 482 that way and its real answer is 35. The device layer is named per
fork below, and the file it names is the only thing read.

LIMITS, stated because the number is easy to misread:
  * This counts what a fork REQUESTS, not what it uses. A requested extension
    with no call site still counts.
  * A fork may enable an extension outside its device layer. The count is a floor.
  * "Requested by nobody" means nobody in THIS list of files. It is not a claim
    about the whole fork. Confirm with a second search before recording it as a
    negative -- this repo has been wrong every time it skipped that step.
"""

import argparse
import os
import re
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FLEET = os.path.dirname(REPO)

# fork -> (path beside this repo, the file that builds the device extension list)
DEVICE_LAYERS = {
    "ARMSX2": ("armsx2-thor/ARMSX2",
               "pcsx2/GS/Renderers/Vulkan/GSDeviceVK.cpp"),
    "xenia": ("xenia-thor-workspace/xenia-thor",
              "src/xenia/ui/vulkan/vulkan_device.cc"),
    "Cemu": ("cemu-thor-experiment",
             "src/Cafe/HW/Latte/Renderer/Vulkan/VulkanRenderer.cpp"),
    "azahar": ("azahar-thor/azahar",
               "src/video_core/renderer_vulkan/vk_instance.cpp"),
    "melonDS": ("melonds_HD/melonDS-android",
                "melonDS-android-lib/src/VulkanContext.cpp"),
    "Vita3K": ("psvita/Vita3K-Thor",
               "vita3k/renderer/src/vulkan/renderer.cpp"),
    "eden": ("eden-thor",
             "src/video_core/vulkan_common/vulkan_device.cpp"),
    # ADDED 2026-08-24. Omitting rpcsx made the no-user list wrong twice: rpcsx
    # requests VK_KHR_dynamic_rendering_local_read, which this tool reported as
    # requested by nobody. PS3 is out of the packed binary; it is not out of the
    # fleet, and a capability census that skips a fork cannot say "nobody".
    "rpcsx": ("ps3-thor/rpcsx-ui-android",
              "app/src/main/cpp/rpcsx/rpcs3/Emu/RSX/VK/vkutils/device.cpp"),
}

# Forks spell an extension two ways: the string "VK_KHR_swapchain" and the macro
# VK_KHR_SWAPCHAIN_EXTENSION_NAME. Both normalise to the string form.
TOKEN = re.compile(r"VK_(KHR|EXT|AMD|NV|GOOGLE|ANDROID|QCOM|ARM|NVX|INTEL)_[A-Za-z0-9_]+")

# A THIRD spelling, and missing it understated two forks badly. Forks on
# vulkan-hpp write vk::KHRBufferDeviceAddressExtensionName -- no VK_ prefix and no
# underscores. Vita3K and azahar are both on vulkan-hpp.
HPP_TOKEN = re.compile(r"vk::(KHR|EXT|AMD|NV|GOOGLE|ANDROID|QCOM|ARM|NVX|INTEL)"
                       r"([A-Za-z0-9]+)ExtensionName")

# Every high-value feature bit below probed as 1 on the Thor, 2026-06-20. See
# xenia-thor/docs/research/20260620-adreno-turnip-feature-gap-audit.md.
PROBED_ON_DEVICE = [
    "VK_KHR_dynamic_rendering", "VK_KHR_dynamic_rendering_local_read",
    "VK_EXT_rasterization_order_attachment_access", "VK_KHR_buffer_device_address",
    "VK_EXT_descriptor_indexing", "VK_EXT_descriptor_buffer",
    "VK_EXT_load_store_op_none", "VK_EXT_graphics_pipeline_library",
    "VK_EXT_pipeline_creation_cache_control", "VK_KHR_fragment_shading_rate",
    "VK_KHR_shader_float16_int8", "VK_KHR_synchronization2",
    "VK_EXT_multi_draw", "VK_EXT_extended_dynamic_state3",
]


VENDORED_LINE = re.compile(r"third_party|3rdparty|externals|dependencies|"
                           r"vulkan_core|volk|vk_mem_alloc", re.I)


def normalise(token):
    """VK_KHR_SWAPCHAIN_EXTENSION_NAME and VK_KHR_swapchain -> VK_KHR_swapchain."""
    body = token[3:]
    vendor, _, rest = body.partition("_")
    rest = re.sub(r"_EXTENSION_NAME$", "", rest, flags=re.IGNORECASE)
    return "VK_%s_%s" % (vendor.upper(), rest.lower())


def normalise_hpp(vendor, camel):
    """vk::KHRBufferDeviceAddressExtensionName -> VK_KHR_buffer_device_address."""
    snake = re.sub(r"(?<!^)(?=[A-Z])", "_", camel).lower()
    return "VK_%s_%s" % (vendor.upper(), snake)


def read(fork):
    rel, path = DEVICE_LAYERS[fork]
    root = os.path.join(FLEET, rel)
    if not os.path.isdir(root):
        return None
    try:
        # bytes, not text: fork sources carry non-UTF-8 bytes and Windows
        # defaults to cp1252, which raises on them.
        blob = subprocess.run(["git", "-C", root, "show", "HEAD:" + path],
                              capture_output=True, timeout=60)
    except (OSError, subprocess.SubprocessError):
        return None
    if blob.returncode != 0:
        return None
    text = blob.stdout.decode("utf-8", errors="replace")
    found = {normalise(m.group(0)) for m in TOKEN.finditer(text)}
    found |= {normalise_hpp(m.group(1), m.group(2)) for m in HPP_TOKEN.finditer(text)}
    return found



def whole_fork(fork, extension):
    """Does the extension appear anywhere in the fork's own source?

    The device-layer file is not the whole fork. This is the check that six
    wrong "nobody uses this" claims on 2026-08-23 did not have.
    """
    rel = DEVICE_LAYERS[fork][0]
    root = os.path.join(FLEET, rel)
    body = extension[3:]
    vendor, _, rest = body.partition("_")
    camel = "".join(w.capitalize() for w in rest.split("_"))
    pattern = "%s|%s|vk::%s%sExtensionName" % (
        extension, extension.upper(), vendor.upper(), camel)
    # --recurse-submodules: a parent-repo grep cannot see submodule contents,
    # which produced three wrong results in this project.
    cmd = ["git", "-C", root, "grep", "--recurse-submodules", "-lIE", pattern,
           "--", "*.cpp", "*.cc", "*.h", "*.hpp"]
    try:
        r = subprocess.run(cmd, capture_output=True, timeout=120)
    except (OSError, subprocess.SubprocessError):
        return False
    out = r.stdout.decode("utf-8", errors="replace").splitlines()
    return any(not VENDORED_LINE.search(ln) for ln in out)


# POSITIVE CONTROLS. This tool is PATH-SCOPED: it reads one named device-layer
# file per fork. A renamed or moved file returns zero extensions, which looks
# exactly like a fork that requests none.
#
#     A path-scoped search that covers nothing looks identical to one that
#     found nothing.
#
# The counts below are the 2026-08-23 census. They are a floor, not an exact
# expectation: a fork adding an extension should not fail the control.
CONTROLS = {
    "eden": 30, "ARMSX2": 25, "Vita3K": 25, "Cemu": 18,
    "xenia": 8, "azahar": 8, "melonDS": 5,
}


def self_test():
    """Prove the census can see a device layer at all."""
    failures = 0
    print("positive controls -- a miss means the FILE moved, not that the fork")
    print("stopped requesting extensions.")
    print("")
    for fork, floor in CONTROLS.items():
        try:
            got = read(fork)
        except Exception as e:                     # noqa: BLE001
            print("  %-9s FAIL  read() raised %s" % (fork, e))
            failures += 1
            continue
        if got is None:
            print("  %-9s SKIP  not present beside this repo" % fork)
            continue
        n = len(got)
        if n < floor:
            print("  %-9s FAIL  %d extensions, expected at least %d" % (fork, n, floor))
            failures += 1
        else:
            print("  %-9s ok    %d extensions" % (fork, n))
    print("")
    print("%d failure(s)." % failures)
    if failures:
        print("DO NOT REPORT AN EXTENSION AS UNREQUESTED until these pass.")
    return failures


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--min-forks", type=int, help="only list extensions this many forks request")
    ap.add_argument("--orphans", action="store_true", help="only one fork requests it")
    ap.add_argument("--self-test", action="store_true",
                    help="prove the census can see each fork's device layer. This "
                         "tool is PATH-scoped, so a moved file reads as a fork that "
                         "requests nothing.")
    ap.add_argument("--check", nargs="*", help="report who requests these (default: the probed set)")
    args = ap.parse_args()
    
    if args.self_test:
        return 1 if self_test() else 0

    sets = {}
    for fork in DEVICE_LAYERS:
        got = read(fork)
        if got is None:
            print("[SKIP] %s: device layer not readable beside this repo" % fork)
            continue
        sets[fork] = got
    if not sets:
        return 1

    print("\n%-10s %s" % ("fork", "device extensions requested"))
    for fork, got in sorted(sets.items(), key=lambda kv: -len(kv[1])):
        print("%-10s %3d   %s" % (fork, len(got), DEVICE_LAYERS[fork][1]))

    counts = {}
    for got in sets.values():
        for e in got:
            counts[e] = counts.get(e, 0) + 1

    print("\nunion %d   asked by one fork only %d   asked by 4+ forks %d"
          % (len(counts),
             sum(1 for n in counts.values() if n == 1),
             sum(1 for n in counts.values() if n >= 4)))

    if args.orphans:
        print("\nrequested by exactly one fork:")
        for e, n in sorted(counts.items()):
            if n == 1:
                who = next(f for f, g in sets.items() if e in g)
                print("  %-56s %s" % (e, who))
    elif args.min_forks:
        print("\nrequested by %d or more forks:" % args.min_forks)
        for e, n in sorted(counts.items(), key=lambda kv: (-kv[1], kv[0])):
            if n >= args.min_forks:
                print("  %d  %s" % (n, e))

    wanted = args.check if args.check else PROBED_ON_DEVICE
    label = "named on the command line" if args.check else "probed = 1 on the Thor"
    print("\nextensions %s, and who asks for them:" % label)
    print("  extension                                            device-layer | elsewhere in fork")
    for e in wanted:
        who = sorted(f for f, g in sets.items() if e in g)
        # A device-layer miss is NOT absence. Six of eight such claims made on
        # 2026-08-23 were wrong because the extension was used elsewhere in the
        # fork. This column is that second search, built in.
        elsewhere = sorted(f for f in sets if f not in who and whole_fork(f, e))
        print("  %-52s %-12s | %s"
              % (e, " ".join(who) if who else "-",
                 " ".join(elsewhere) if elsewhere else "-"))
    print("")
    print("A blank left column with names on the right means the fork uses the")
    print("extension without its device layer naming it. A row blank on BOTH")
    print("sides is a candidate for absence, not a proof of it.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
