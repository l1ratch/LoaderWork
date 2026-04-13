# LoaderWork

`LoaderWork` is a Minecraft `1.21+` Spigot/Paper plugin that turns block hauling into a configurable job system.

## Features

- Job-based gameplay profiles
- Custom cuboid pickup and dropoff regions
- Hold-to-pickup and hold-to-dropoff mechanics
- Visual carried block with `BlockDisplay`
- Slowness and optional fatigue while carrying
- Reward support for money, experience, commands and items
- Fully configurable messages
- Safe cleanup on disconnect, reload and disable

## Requirements

- Minecraft server `1.21+`
- Spigot or Paper
- Java `21`
- Optional: `Vault` for money rewards

## Commands

### Player

```text
/loader list
/loader job <id>
/loader info
/loader cancel
```

### Admin

```text
/loader job create <id>
/loader job delete <id>
/loader edit <job> ...
/loader region <job> ...
/loader config ...
/loader inspect <id>
/loader reload
```

## Setup

1. Build the jar with Maven.
2. Put it into the server `plugins/` folder.
3. Start the server once.
4. Edit `config.yml` or use the in-game commands.
5. Reload with `/loader reload`.

## Main files

- Bootstrap: `src/main/java/ru/l1ratch/loaderwork/LoaderWork.java`
- Gameplay controller: `src/main/java/ru/l1ratch/loaderwork/LoaderController.java`
- Default config: `src/main/resources/config.yml`
- Plugin metadata: `src/main/resources/plugin.yml`

## License

Add the license you want to publish under before releasing the plugin publicly.
