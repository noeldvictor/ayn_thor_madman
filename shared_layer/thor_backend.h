// The native backend contract.
//
// This is the C++ half. The Kotlin half, in app/shell/.../Backend.kt, covers
// what the app UI needs. This covers what the shared Vulkan layer needs.
//
// STATUS: draft, 2026-08-22. Nothing implements it. It exists so the
// architecture can be argued against something concrete rather than in prose.
//
// Three rules govern this file. See CLAUDE.md.
//
//  1. THIN. The cores are widely different. A PS2 recompiler, a Wii U graphic
//     pack system and a DS plane compositor do not fit one shape.
//
//  2. NOT A MODULE BOUNDARY. One binary, one link unit. This exists for
//     clarity and it must stay inlinable. No virtual call in a hot path
//     without a measured reason.
//
//  3. THE BACKEND KEEPS ITS GUEST SIDE. It hands over the host side: the
//     Vulkan device, the caches, the allocator, the scheduler, the present
//     path. See shared_layer/PATTERNS.md for the line, pipeline by pipeline.

#pragma once

#include <cstdint>
#include <string_view>
#include <vulkan/vulkan.h>

namespace thor {

// ---------------------------------------------------------------- the device
//
// The shared layer owns exactly one of these. A backend never creates an
// instance, never picks a physical device, never makes an allocator.
//
// Seven forks each built their own. That is the duplication this removes, and
// it is the only kind that cannot be guest-specific: instance creation, device
// selection, queues and allocation carry no guest semantics.

struct DeviceHandles {
  VkInstance instance;
  VkPhysicalDevice physical;
  VkDevice device;

  // One graphics-capable family on this part. Kept explicit so a backend does
  // not re-query and disagree.
  uint32_t graphics_family;
  VkQueue graphics_queue;

  // The queue is shared. Take this before submitting.
  // melonDS already learned this and carries its own queue mutex.
  void* queue_lock;
};

// Facts about the Thor, resolved once. A backend reads these instead of
// probing.
//
// On this device every one of these is a constant. They exist as fields rather
// than #defines so a debug build can assert them against the real device, and
// so a future part does not silently inherit wrong answers.
struct DeviceFacts {
  uint32_t vendor_id;          // 0x5143, Qualcomm
  uint32_t api_version;        // 1.3.128 measured on this device
  bool is_tiler;               // true. Adreno 740.
  bool timeline_semaphores;    // true under pinned Turnip
  bool dynamic_texture_index;  // true
  bool non_uniform_index;      // true

  // The pinned driver identity. The pipeline cache is keyed on this, and it is
  // why the cache never has to invalidate. See CLAUDE.md, The driver baseline.
  std::string_view driver_id;
};

// --------------------------------------------------------------- the caches
//
// One texture cache, one pipeline cache, one allocator, one budget owner.
//
// The KEY IS OPAQUE. The backend computes it, because hashing a texture means
// hashing guest texel formats and a guest palette. ARMSX2 and melonDS both key
// on (data hash, palette hash) and Cemu keys on physical address; the shared
// layer must not care which.

using TextureKey = uint64_t;

// A texture class the backend can distinguish at upload time.
//
// NOT AN ENUM. ARMSX2 has two classes, World and Ui. melonDS has three,
// 3D, OBJ sprite and BG layer. Cemu has none. A fixed enum would force one
// emulator's taxonomy onto the others.
using TextureClassId = uint8_t;
inline constexpr TextureClassId kTextureClassNone = 0xFF;

struct TextureDesc {
  TextureKey key;
  TextureClassId klass;
  uint32_t width;
  uint32_t height;
  VkFormat format;

  // True when the backend knows this texture came from a palette. The shared
  // upscaler treats paletted art differently, and only the backend knows.
  bool paletted;
};

// What the shared layer did with an upload request.
//
// Every decline reason is separate on purpose. ARMSX2 learned this: "nothing
// got upscaled" has half a dozen very different causes and they need different
// fixes.
enum class UploadResult : uint8_t {
  kCacheHit,
  kUploaded,
  kUpscaled,
  kReplacedByPack,
  kDeclinedClassDisabled,
  kDeclinedBudget,
  kDeclinedRateLimit,
  kDeclinedUnsupportedFormat,
  kDeclinedNoModel,  // a neural algorithm was picked but no weights installed
};

// ------------------------------------------------------------- guest screens
//
// The backend declares what exists. The app decides where it goes.
// The Thor has two internal touch panels; see CLAUDE.md, Two displays.

struct GuestScreen {
  std::string_view name;  // "top", "bottom", "tv", "gamepad"
  uint32_t width;
  uint32_t height;
  bool takes_touch;

  // False when the title never draws it. A Wii U game may never use the
  // GamePad screen, and the layout picker must not offer a panel for it.
  bool required_by_title;
};

// ------------------------------------------------------- guest system UI
//
// The guest OS asks the host to show something and waits for an answer. A
// software keyboard, an account picker, an avatar selector, an error dialog.
//
// THE APP RENDERS ALL OF IT, IN THE APP'S STYLE. Seven backends each drawing
// their own system dialogs is the inconsistency this project exists to remove,
// and a person should not be able to tell which backend asked.
//
// Modelled on azahar Frontend::SoftwareKeyboard, which is the most developed
// version of this in the fleet and already splits it the right way: the guest
// HLE fills in a config, the frontend renders it and returns data. Its own
// header says so.
//
// azahar is GPL-2.0-or-later, so this may be used as GPL-3.0.

enum class AppletKind : uint8_t {
  kTextEntry,     // software keyboard
  kUserSelect,    // account, profile, Mii, gamertag
  kErrorDialog,   // the guest raised an error and wants it shown
};

// What kind of input the guest will accept. Taken from azahar's AcceptedInput,
// which learned these cases from real 3DS titles.
enum class AcceptedInput : uint8_t {
  kAnything,
  kNotEmpty,
  kNotEmptyAndNotBlank,
  kNotBlank,
  kFixedLength,
};

struct TextEntryConfig {
  AcceptedInput accept;
  bool multiline;
  uint16_t max_length;
  uint16_t max_digits;
  std::string_view hint;

  // Button labels the guest supplies. The app styles them; it does not rename
  // them. A guest may ask for one, two or three.
  uint8_t button_count;
  std::string_view button_text[3];

  // Guest-imposed restrictions. Kept as a flag set rather than a callback, so
  // the app can validate before dismissing rather than round-tripping.
  bool prevent_digit;
  bool prevent_at;
  bool prevent_percent;
  bool prevent_backslash;
  bool prevent_profanity;

  // True when the guest wants to vet the string itself. The app must then
  // call back before accepting.
  bool guest_validates;
};

// Why the app rejected input, so it can say so in place rather than failing
// silently. Superset of azahar's ValidationError.
enum class ValidationError : uint8_t {
  kNone,
  kButtonOutOfRange,
  kMaxDigitsExceeded,
  kAtSignNotAllowed,
  kPercentNotAllowed,
  kBackslashNotAllowed,
  kProfanityNotAllowed,
  kGuestRejected,
  kFixedLengthRequired,
  kMaxLengthExceeded,
  kBlankInputNotAllowed,
  kEmptyInputNotAllowed,
};

struct TextEntryResult {
  std::string_view text;
  uint8_t button;   // which of the guest's buttons was pressed
  bool cancelled;
};

// A guest user the backend knows about. Cemu has Account, azahar has Mii,
// eden has profiles; the shapes differ and only the id crosses the boundary.
struct GuestUser {
  std::string_view id;
  std::string_view display_name;
};

// RULE: an applet request BLOCKS THE GUEST. Show it immediately. Do not queue
// it behind an animation and do not batch it with a frame.

// ------------------------------------------------------------------ settings
//
// The minimum contract names settings: the key namespace, and the resolution
// order for a per-game override. This section was missing until 2026-08-22.
//
// It is modelled on ARMSX2's ConfigStore, which is the only production
// implementation in the fleet: 240 fields, two storage tiers, and three fixes
// that the obvious design does not have. Do not simplify it. The Kotlin
// counterpart in app/shell/Backend.kt has JUnit tests, and the obvious design
// fails four of eight.
//
// See research_log/20260822_2203_armsx2_frontend_is_the_shell.md.

enum class SettingType : uint8_t { kBool, kInt, kFloat, kEnum, kString };

// Which storage tier a setting may live in.
//
// kPerGame is the normal case and the project's stated requirement. The
// exception is real: some settings are structurally process-wide, because
// there is one server, one loaded driver and one device. ARMSX2's PINE server
// is one instance for the whole process, so its per-game file refuses the key.
// Toggling it from the in-game menu wrote it NOWHERE and it read as enabled
// until the process restarted.
enum class SettingScope : uint8_t {
  kPerGame,     // sparse, and sticky once set
  kPromoted,    // offered per game, WRITTEN TO GLOBAL
  kGlobalOnly,  // never offered per game
};

struct SettingSpec {
  // Stable forever. A setting with no stable key cannot carry an override.
  std::string_view key;
  std::string_view label;
  std::string_view group;
  SettingType type;
  SettingScope scope;
  std::string_view default_value;
  // Empty unless type is kEnum.
  const std::string_view* options;
  uint32_t option_count;
  // False means changing it needs a restart, and the UI must say so BEFORE
  // the change rather than after.
  bool live_changeable;
};

// Where a resolved value came from. The order is fixed and lives in one place;
// a backend never invents its own.
enum class SettingSource : uint8_t {
  kPerGame,
  kGlobal,
  kThorProfile,
  kBackendDefault,
};

// THE THREE RULES A WRITER MUST OBEY. Each was paid for by a reported bug.
//
// 1. SPARSE. A field the user never touched is absent, so a later global
//    change still reaches it. That inheritance is the point.
//
// 2. STICKY. A field is PINNED once overridden and stays pinned even when its
//    value equals global. Without this, setting a value per game while global
//    agrees stores nothing; a later global change then silently takes the
//    game's setting with it. ARMSX2's symptom: cheats on per game while global
//    was also on, global later off, THE GAME SILENTLY LOST ITS SETTING.
//
// 3. CHANGE-TRACKED. Rule 2 makes a wrong value permanent, so a pinned key the
//    caller did not just touch must keep its STORED value rather than whatever
//    the incoming object holds. Screens write whole settings objects, so the
//    incoming object can be a stale snapshot. ARMSX2's symptom: A PER-GAME
//    FRAME CAP OF 30 CAME BACK AS 0 AND STAYED 0, surviving even after the
//    writers were fixed.
//
// Rule 3 exists BECAUSE of rule 2. A contract that specifies pinning without
// change-tracking ships the third bug.
//
// A kPromoted field is written to global by copying THAT ONE FIELD, never by
// saving the resolved object, which would leak every per-game value upward.

// Settings need versioning. ARMSX2 carries seven one-time migration keys for
// fields that changed scope or default.
inline constexpr uint32_t kSettingsSchemaVersion = 1;

// ------------------------------------------------------------- the interface

class Backend {
 public:
  virtual ~Backend() = default;

  // ---- lifecycle

  // Called once, before anything else. The backend stores the handles and
  // facts. It must not create a device of its own.
  virtual bool AttachDevice(const DeviceHandles& handles,
                            const DeviceFacts& facts) = 0;
  virtual void DetachDevice() = 0;

  // ---- what the backend is

  virtual std::string_view Id() const = 0;

  // The texture classes this backend can distinguish, in id order. Empty is
  // valid and means no per-class routing. Cemu is the empty case.
  virtual uint32_t TextureClassCount() const = 0;
  virtual std::string_view TextureClassName(TextureClassId) const = 0;

  virtual uint32_t GuestScreenCount() const = 0;
  virtual GuestScreen GuestScreenAt(uint32_t index) const = 0;

  // ---- settings
  //
  // The backend declares. The app renders, resolves and stores. A backend
  // never reads or writes a setting file itself, and never resolves its own
  // override order.
  virtual uint32_t SettingCount() const = 0;
  virtual SettingSpec SettingAt(uint32_t index) const = 0;

  // Apply a fully resolved value. The backend is told the answer; it does not
  // compute it. Returns false if the key is unknown or the value is invalid.
  virtual bool ApplySetting(std::string_view key, std::string_view value) = 0;

  // ---- guest users
  //
  // Empty is valid. Not every system has accounts.
  virtual uint32_t GuestUserCount() const = 0;
  virtual GuestUser GuestUserAt(uint32_t index) const = 0;
  virtual bool SetActiveGuestUser(std::string_view id) = 0;

  // ---- the frame
  //
  // The backend records into command buffers the shared layer supplies, and
  // presents through the shared present path.
  //
  // NOTE, and this is the open architectural question: RENDER PASS STRUCTURE
  // STAYS WITH THE BACKEND for now. THOR_RENDER.md commitment 2 wants a shared
  // render graph that plans GMEM residency, which would take this away.
  //
  // Read 2026-08-22: taking it away costs nothing, because no fork plans
  // passes. eden keys its render pass cache on formats and sample count only,
  // then hardcodes LOAD_OP_LOAD, STORE_OP_STORE, one subpass, zero input
  // attachments and no resolve attachments. Vita3K is also format-keyed.
  //
  // So a shared graph would be ADDITIVE. It is still not done here, for a
  // different and smaller reason: the cheap experiment has not run. Correcting
  // load and store ops per backend is one struct filled in differently and it
  // moves the same numbers. Do that first.
  virtual void BeginFrame() = 0;
  virtual void RecordFrame(VkCommandBuffer cb) = 0;
  virtual void EndFrame() = 0;

  // ---- counters
  //
  // A declaration, not a fixed list. A backend that cannot report GPU busy
  // time should not be made to lie about it.
  virtual uint32_t CounterCount() const = 0;
  virtual std::string_view CounterName(uint32_t) const = 0;
  virtual double CounterValue(uint32_t) const = 0;
};

// ------------------------------------------------ what the shared layer gives
//
// Free functions rather than an interface, because these are called from hot
// paths and must inline.

// Upload or fetch a texture. The shared layer owns storage, eviction, the
// budget, per-class routing, upscaling and pack replacement.
//
// The backend supplies the key and the class. It does not own the lifetime.
//
// This split follows ARMSX2 over melonDS. melonDS puts the pack and the filter
// cache inside its cache entry; ARMSX2 keeps its upscaler pure and outside,
// because "inventing a second owner of that lifetime is how it gets
// corrupted". A shared upscaler cannot own seven caches' lifetimes.
UploadResult UploadTexture(const TextureDesc& desc, const void* pixels,
                           size_t size, VkImageView* out_view);

// Look up without uploading. Returns kCacheHit or a decline.
UploadResult LookupTexture(TextureKey key, VkImageView* out_view);

// Ask the shared pipeline cache. Keyed by the pinned driver, so it survives
// across runs, sessions and backends.
VkPipeline GetPipeline(uint64_t pipeline_key,
                       const VkGraphicsPipelineCreateInfo& info);

// Report a resolve the backend could not avoid, and why. Resolves are the most
// common way to lose frames on a tiler, so they are a first-class statistic
// rather than something to infer from a capture.
void ReportResolve(std::string_view reason);

// Report an LRZ break, for the same reason.
void ReportLrzBreak(std::string_view reason);

// ---- guest system UI, host side
//
// The backend calls these when the guest asks. They block until the person
// answers, because the guest is blocked too.

ValidationError RequestTextEntry(const TextEntryConfig& config,
                                 TextEntryResult* out);

// Returns false if the person cancelled.
bool RequestUserSelect(std::string_view* out_user_id);

// The guest raised an error and wants it shown. Returns when acknowledged.
void ShowGuestError(std::string_view title, std::string_view message);

}  // namespace thor

// ---------------------------------------------------------------------------
// OPEN QUESTIONS. Recorded here so they are not lost between the design docs
// and the code.
//
// 1. Does the shared layer take render pass structure? THOR_RENDER.md
//    commitment 2 says the render graph should be a GMEM residency plan. This
//    header leaves passes with the backend. Resolving this needs a measurement
//    on one backend, not an argument.
//
// 2. Is a virtual Backend acceptable in the frame path? Rule 2 says the
//    contract must stay inlinable. A virtual RecordFrame per frame is
//    harmless; a virtual call per draw would not be. If the interface grows
//    toward per-draw calls, revisit.
//
// 3. Who owns the swapchains? The app owns both panels today. A backend that
//    presents two guest screens needs a path to both, and that path is not
//    designed.
//
// 4. What does a backend do when the budget owner declines an upload? Falling
//    back to a native-resolution texture is the obvious answer and it is not
//    specified.
//
// 5. Threading. The Thor has one Cortex-X3 and the A510s share a vector unit
//    in a complex. Command buffer recording, pipeline compilation and upload
//    all want placing deliberately. Nothing here expresses that.
