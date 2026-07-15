# Chunk XP Fatigue

<p style="color: #d6a100;"><strong>AI usage disclaimer:</strong> This mod was developed with AI-agent assistance. I reviewed the work step by step during development and test releases in my personal Cleanroom modpack with 300+ mods.</p>

Chunk XP Fatigue reduces mob XP through accumulated, slowly recovering chunk pressure, making sustained mob-XP farming in one location progressively less rewarding. Mobs still spawn normally and keep their item drops, while players who travel and fight across different locations should see little or no effect.

## How It Works

- Qualifying mob deaths build shared pressure in the chunk where the mob dies.
- The chunk's existing pressure reduces later mob XP according to a configurable curve.
- Pressure recovers slowly while world is running, including while the chunk is unloaded.
- Crowding Acceleration makes dense groups build pressure faster while leaving small groups at the normal gain.
- Pressure persists across restarts, but time while the server is shut down does not count as recovery.

XP is rounded down after scaling, so small XP rewards can become zero at high pressure.

With the default settings:

- Each qualifying death adds `1` pressure, up to a maximum of `100`.
- XP remains at `100%` through `20` pressure, then decreases linearly to `10%` at `100` pressure.
- Recovery removes `1` pressure every `3` minutes of server uptime. A chunk at `100` pressure returns to the full-XP threshold after `4` hours and reaches zero after `5` hours if no new pressure is added.
- Crowding Acceleration is enabled with a `3`-block radius. The first `4` nearby live mobs add no bonus; each additional mob adds `25%` of the base pressure gain, capped at `4x` gain.

## Configuration and Diagnostics

Server and modpack owners can configure pressure gain, maximum pressure, recovery, the full-XP Grace Range, the XP curve, minimum multiplier, and Crowding Acceleration. Crowding radius, ordinary-group allowance, per-mob bonus, and maximum acceleration are independently configurable.

Detailed server death logging is available for debugging and disabled by default. Authorized operators can inspect, set, or clear pressure with `/chunkxpfatigue`.

## Installation

- Install on the server for multiplayer use.
- Dedicated-server players do not need the mod.
- Install normally for single-player worlds.

The mod affects mob-death XP only. Mining, smelting, XP bottles, commands, spawning, loot tables, and item drops are unchanged.

## Source

Source code and issue tracking: <https://github.com/mahghuuuls/chunk-xp-fatigue>
