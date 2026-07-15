# Chunk XP Fatigue

Chunk XP Fatigue reduces mob XP through accumulated, slowly recovering chunk pressure. It does not affect mob spawning or item drops.

- [Player-facing mod page copy](MOD-PAGE.md)
- [Changelog](CHANGELOG.md)
- [License](LICENSE)
- [Third-party notices](THIRD-PARTY-NOTICES.md)

## Project Details

- Mod ID: `chunkxpfatigue`
- Minecraft: 1.12.2
- Loader: Forge
- Java target: 8
- Source: <https://github.com/mahghuuuls/chunk-xp-fatigue>

The mod is required on the logical server. For a dedicated server, clients may connect without installing it; the mod has no client-only feature. Single-player users install the mod normally because their world runs an integrated server.

## Behavior

Each qualifying mob death adds pressure to the dimension and chunk where the mob dies. Existing pressure determines that death's XP multiplier before the new pressure is added. Pressure is shared by all players and mob types in the chunk, persists across restarts, and recovers only while the server or single-player world is running.

Crowding Acceleration can increase the pressure contribution when more than the configured allowance of nearby live mobs is detected. It changes only subsequent pressure; it does not apply a second multiplier to the current death.

The mod does not change spawning, item drops, loot tables, or XP from mining, smelting, bottles, or commands.

## Configuration

Forge generates `config/chunkxpfatigue.cfg`. Gameplay settings are read at startup and require a world or server restart after editing.

Core pressure and XP settings:

- `pressureGain = 1.0`
- `maximumPressure = 100.0`
- `recoveryMinutesPerPressure = 3.0` (zero disables recovery)
- `graceThresholdPercent = 20.0`
- `minimumMultiplierPercent = 10.0`
- `curvePoints = 20:100, 100:10`

Crowding settings:

- `crowdingEnabled = true`
- `crowdingRadius = 3.0`
- `crowdingAllowance = 4`
- `crowdingBonusPerExcessMob = 0.25`
- `maximumCrowdingMultiplier = 4.0`

Diagnostics:

- `debugLoggingEnabled = false`

XP is rounded down to a whole number after scaling. Consequently, a low XP reward can become zero at high pressure even when the configured multiplier is above zero.

## Operator Commands

The `/chunkxpfatigue` command requires permission level 2.

- `inspect [dimension chunkX chunkZ]`
- `set <pressure> [dimension chunkX chunkZ]`
- `clear chunk [dimension chunkX chunkZ]`
- `clear dimension [dimension] confirm`
- `clear world confirm`

Players may omit bracketed locations to target their current chunk or dimension. The server console must provide explicit location arguments where applicable.

## License and Attribution

Chunk XP Fatigue is available under the [MIT License](LICENSE). Template attribution is recorded in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
