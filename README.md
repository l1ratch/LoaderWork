# LoaderWork

`LoaderWork` is a Minecraft `1.21+` plugin for Spigot/Paper that turns simple block hauling into a configurable job system.

## What it does

1. A player selects a job manually or gets one automatically in the pickup area.
2. The player crouches and holds still to pick up an allowed block.
3. The block becomes a carried load with visual and movement effects.
4. The player brings it to the dropoff area.
5. The player crouches and holds still again to unload it.
6. The plugin gives rewards and restores the original block later.

## Features

- Job profiles with separate settings
- Pickup and dropoff cuboids stored in the plugin config
- Hold-to-pickup and hold-to-dropoff gameplay
- Visual carried block using `BlockDisplay`
- Weight effects through slowness and optional fatigue
- Navigation particles toward the dropoff area
- Rewards with money, experience, console commands and items
- Fully configurable messages
- Safe cleanup on cancel, disconnect, reload and job disable

## Requirements

- Minecraft server `1.21+`
- Spigot or Paper
- Java `21`
- Optional: `Vault` for money rewards

Without `Vault`, money rewards are skipped and the rest of the plugin still works.

## Installation

1. Build the plugin jar with Maven.
2. Place the jar into your server `plugins/` folder.
3. Start the server once so the default config is generated.
4. Edit `config.yml` or use the in-game admin commands.
5. Reload the plugin with `/loader reload` or restart the server.

## Commands

### Player commands

```text
/loader list
/loader job <id>
/loader info
/loader cancel
```

### Admin commands

```text
/loader job create <id>
/loader job delete <id>
/loader edit <job> ...
/loader region <job> ...
/loader config ...
/loader inspect <id>
/loader reload
```

Detailed setup guide: [GUIDE-RU.md](GUIDE-RU.md)

## Permissions

- `loader.use` - base gameplay access
- `loader.cancel` - cancel pickup or carrying
- `loader.admin` - access to admin commands

You can also add a per-job permission in the job profile:

```yaml
permission: "loader.job.quarry"
```

## Configuration overview

Main sections:

- `settings`
- `jobs`
- `regions`
- `messages`

### Settings

```yaml
settings:
  auto-select-job-by-region: true
  update-interval-ticks: 2
  hold-max-move-distance: 0.18
  carry-particle-interval-ticks: 10
```

### Job example

```yaml
jobs:
  quarry_worker:
    enabled: true
    display-name: "Quarry Worker"
    permission: ""
    pickup-region: "quarry_pickup"
    dropoff-region: "quarry_dropoff"
    pickup-hold-ticks: 40
    dropoff-hold-ticks: 30
    respawn-delay-ticks: 600

    carry:
      slowness-amplifier: 2
      fatigue-amplifier: 1
      navigation-interval-ticks: 20
      display-height: 0.35
      display-behind-offset: 0.95

    allowed-blocks:
      - STONE
      - COBBLESTONE
      - IRON_ORE

    rewards:
      default:
        money: 1.0
        experience: 1
        commands: []
        items: []
      by-block:
        IRON_ORE:
          money: 5.0
          experience: 3
```

### Region example

```yaml
regions:
  quarry_pickup:
    world: world
    pos1:
      x: 100
      y: 60
      z: 100
    pos2:
      x: 110
      y: 70
      z: 110
```

## Public notes

- Jobs can be enabled or disabled from in-game admin commands.
- If a job is disabled while someone is carrying a load, the session is stopped safely.
- If `allowed-blocks` is empty, any transportable block is allowed.
- Region names and job ids are restricted to safe identifiers.

## Project files

- Main plugin bootstrap: `src/main/java/ru/l1ratch/loaderwork/LoaderWork.java`
- Core gameplay controller: `src/main/java/ru/l1ratch/loaderwork/LoaderController.java`
- Default configuration: `src/main/resources/config.yml`
- Plugin metadata: `src/main/resources/plugin.yml`

## License

Add the license that fits your distribution model before publishing widely.
