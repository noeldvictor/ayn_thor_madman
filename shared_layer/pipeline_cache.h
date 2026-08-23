// The driver pipeline cache. Owned by the shared layer.
//
// Extraction candidate 3. Verified 2026-08-23: all eight forks call
// vkGetPipelineCacheData in their own file. This owns the DRIVER BLOB ONLY --
// the guest-shader-to-SPIR-V cache stays with the backend, because it is keyed
// on a guest source hash and is guest knowledge.
//
// The two have different lifetimes and that difference is the design:
//   * the driver blob dies when the driver's compiled format changes;
//   * the translation cache survives that, because SPIR-V is portable;
//   * both die when the app that generated the shaders changes.
//
// The executable specification is app/shell PipelineCache.kt with 14 tests.
// This header is the native declaration; no implementation is written yet.
//
// SPDX-License-Identifier: GPL-3.0-or-later

#pragma once

#include <cstdint>
#include <cstddef>

namespace thor::pipeline_cache {

inline constexpr uint32_t kHeaderVersionOne = 1;
inline constexpr size_t kUuidSize = 16;   // VK_UUID_SIZE
inline constexpr size_t kMinHeaderLength = 32;

// The 32-byte header every Vulkan pipeline cache blob begins with.
struct Header {
  uint32_t header_length;
  uint32_t header_version;
  uint32_t vendor_id;
  uint32_t device_id;
  uint8_t uuid[kUuidSize];
};

// What the device reports today.
struct DeviceIdentity {
  uint32_t vendor_id;
  uint32_t device_id;
  uint8_t uuid[kUuidSize];
};

// Why a blob was refused.
//
// Every reason is separate on purpose. ARMSX2 logs which field disagreed, and
// that is the difference between "the cache did not load" and a diagnosis. A
// single boolean would make a driver swap and an app update look identical.
enum class Verdict : uint8_t {
  kAccept = 0,
  kHeaderTooShort,
  kBadHeaderVersion,
  kVendorMismatch,
  kDeviceMismatch,

  // The driver's compiled format changed. The Vulkan specification's own
  // signal, and the only thing that should be keyed on -- NOT a driver package
  // name or version string, which can stay identical across a format change.
  kUuidMismatch,

  // The app changed, and the device header cannot see that.
  //
  // NOTE(thor): this is the bug ARMSX2 shipped and fixed. vendor_id, device_id
  // and the UUID are all IDENTICAL across an app update on the same phone, so
  // bumping the shader-cache version wiped the SPIR-V while keeping every
  // pipeline built from the previous build's shaders. Nothing pruned the blob,
  // and it was re-serialised in full every N compiles on the render thread, so
  // the dead entries became an ever-growing mid-gameplay stall that only a
  // clean reinstall cleared. Users reported "clean install improved
  // performance".
  kEpochMismatch,
};

// Whether a blob may be handed to vkCreatePipelineCache.
//
// Order matters: the cheapest and most fundamental checks run first, so a
// truncated file never has its fields read.
Verdict Validate(const Header& header, const DeviceIdentity& device,
                 uint64_t file_epoch, uint64_t app_epoch);

// True only for kEpochMismatch.
//
// Discarding the translation cache on a driver swap throws away work that is
// still valid. Keeping it across an app update keeps work that is stale.
bool AlsoDropTranslationCache(Verdict verdict);

// Whether the blob has grown past its cap.
//
// Over the cap the action is to DISCARD, not to trim: a pipeline cache blob is
// opaque and there is no supported way to remove one entry.
bool OverCap(uint64_t size_bytes, uint64_t cap_bytes);

// The file a blob lives in: "pipeline-<uuid hex>-<epoch>.bin".
//
// One file per UUID is what makes a per-game driver override survivable. With a
// single shared file, switching driver for one game would discard the warm
// cache for EVERY backend at once. Keep the current file and the two most
// recent others, so switching back finds the old one intact.
//
// Returns the number of bytes written, or 0 if the buffer is too small.
size_t FileName(const uint8_t (&uuid)[kUuidSize], uint64_t epoch,
                char* out, size_t out_size);

}  // namespace thor::pipeline_cache
