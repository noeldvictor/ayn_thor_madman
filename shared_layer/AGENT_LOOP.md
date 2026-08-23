# The paused agent loop

**Pause the guest, look, decide, inject, resume. The model's latency costs the
guest nothing.**

**The idea in one line:** an emulator can stop time. An agent driving a real
console cannot, so it loses every race against an animation. **An agent driving
an emulator never has to enter the race.**

---

## The loop

1. **Pause** the guest. Deterministic, and it costs no CPU.
2. **Capture** the framebuffer — both panels if the title uses two.
3. **Ask** a vision model what state this is and which input advances the goal.
4. **Inject** the input.
5. **Resume** for a bounded number of frames.
6. Repeat.

**Steps 3 and 4 happen while the guest is frozen.** From the guest's side the
input arrives on the frame after resume. **No timer expires, no animation
desyncs, no window is missed.**

> **Model latency is free. That is the whole trick, and it is only available
> because we own the emulator.**

## Why this matters more than it first looks

### 1. It removes the worst measurement problem in the fleet

rpcsx measured the spread of **one** configuration re-run:

| workload | spread |
| --- | --- |
| gated title screen | +/-0.2% |
| restored savestate | +/-5% |
| **button presses through cutscenes** | **about +/-50%, unusable** |

**Getting to a scene is the noisy part, not measuring at it.** Vita3K's render
skill says it outright: *"Do not keep replaying long intros manually."*

**A paused loop makes routing deterministic**, which turns "reach the scene"
from the dominant source of variance into a fixed prologue. **Same start state
plus the same input sequence gives the same result** — that is what makes it a
test rather than an attempt.

### 2. It unblocks queued work that is currently blocked on a human

`DEVICE_QUEUE.md` entries need somebody to reach a scene. **A route that an
agent can drive is a route that runs unattended**, which is the difference
between a queue and a backlog.

**And it enables the compatibility sweep** this project has specified and never
built: launch every title, drive past the menus, record how far each reaches.

### 3. It is a product feature, not only a harness

**Judge a feature by the time it costs the person.** Auto-navigating to a save
point, skipping a cutscene, or getting past a menu somebody cannot read is the
same mechanism pointed at the user rather than the test.

---

## Knowing when NOT to look at the frame

**A vision model asked "what button?" during a pre-rendered movie will guess.**
The answer is not to guess better. It is to know that a movie is playing.

**Every console has a dedicated video-decode path, and every fork implements
it:**

| Guest | Signal | Fork files matching |
| --- | --- | --- |
| PS3 | `cellVdec`, `cellPamf`, `cellDmux` | rpcsx, 12 |
| PS2 | IPU (MPEG decode) | ARMSX2, 40 |
| Wii U | `H264` / AVC decode | Cemu, 72 |
| 3DS | `mvd` service | azahar, 7 |
| Switch | `nvdec` | eden, 32 |
| Vita | `SceAvcdec` | Vita3K, 3 |
| Xbox 360 | XMA and video media player | xenia, 30 |

**So "am I in a movie" is answerable in every backend, by watching whether the
guest is calling its decoder.**

**It is worth more than the agent loop alone.** A backend that knows its guest
activity state can also:

- **refuse to be measured** during a state with a 50% noise floor;
- **not generate frames** for a fixed-rate video, where extrapolation invents
  motion that is not there;
- **not run a texture upscaler** on decoded video frames;
- **not draw the overlay** over a cutscene, and offer skip instead;
- **change the power and thermal policy**, since a movie is decode-heavy and
  CPU-light.

---

## The contract addition

**A backend declares its guest activity state.** Minimum:

| State | Meaning |
| --- | --- |
| `GAMEPLAY` | the guest is running normally |
| `VIDEO` | the guest's video decoder is active |
| `LOADING` | disc or storage access dominates |
| `MENU` | best-effort, and optional |

**`VIDEO` is the one that must be accurate**, because it gates measurement,
frame generation and upscaling. **The rest may be `UNKNOWN`.**

**And the control surface the loop needs**, all of which already exist:

| Call | Fork evidence |
| --- | --- |
| `pause` / `resume` | **all seven forks** — ARMSX2 35 files, eden 34, xenia 24, azahar 14, melonDS 11, Vita3K 4, Cemu 2 |
| `advance N frames` | present where frame-advance exists |
| `capture(screen)` | every fork screenshots; rpcsx and Vita3K both have **burst** capture skills |
| `inject(input)` | **Vita3K `vita3k-input-automation`, 120 lines**; rpcsx `thor-game-controller` |
| `save_state` / `load_state` | every fork |

**eden's cheat VM already exposes exactly this shape as host callbacks** —
`PauseProcess`, `ResumeProcess`, `HidKeysDown`, `MemoryReadUnsafe`. **The
primitive set is already contract-shaped somewhere in the fleet.**

**And ARMSX2's `docs/mcp-server.md` already specifies the surface**: framebuffer
capture, emulator control, save and load state, pause, fast-forward and send
input. **It names the same bottleneck this document does** — "boot game, reach a
scene, open the pause menu, change algorithm, screenshot, compare, repeat".

---

## Rules, so this does not become a source of bad results

- **Burst capture, not one frame.** Both rpcsx and Vita3K learned this
  separately. **A single lucky frame is not evidence.**
- **A truncated input macro is harness noise.** rpcsx's rule: do not let
  screenshots from a truncated run become a route boundary or a proof.
- **Resume by a bounded frame count, not by wall-clock.** Wall-clock reintroduces
  the variance the pause removed.
- **Record the input sequence.** A route that cannot be replayed without the
  model is not deterministic; **the model's output is the artifact, not the
  model.**
- **The agent drives the route. It does not judge the result.** Judging stays
  with golden images and counters.

## What is not claimed

- **Nothing here is built.** Every primitive exists in at least one fork; **the
  loop does not exist anywhere.**
- **No latency number.** How long a pause-capture-decide-resume cycle takes on
  this device is unmeasured.
- **Video detection is proposed, not verified.** The decoder files are counted,
  not read. **Whether each fork can cheaply report "decoder active" is unknown.**
- **Vision-model accuracy on emulated console UI is unmeasured**, and menu text
  at 240p is not a favourable case.
