# Changelog

## [1.0.0] - 2026-07-14

### Added

- Shared pressure for each dimension and chunk, increased by qualifying mob deaths.
- Configurable XP curve with a full-XP Grace Range and minimum multiplier.
- Server-uptime recovery that continues while represented chunks are unloaded.
- Persistent pressure with automatic cleanup after recovery reaches zero.
- Crowding Acceleration for faster pressure gain around dense groups of live mobs.
- Operator commands to inspect, set, and clear chunk, dimension, or world pressure.
- Detailed server-side death diagnostics, disabled by default.

### Compatibility

- Runs on the server for dedicated multiplayer; players do not need to install it on their clients.
- Supports normal single-player installation through Minecraft's integrated server.
- Leaves spawning, item drops, loot tables, and non-mob XP sources unchanged.
