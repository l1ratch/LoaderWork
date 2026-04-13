# LoaderWork

`LoaderWork` - плагин для Minecraft `1.21+` на Spigot/Paper, который превращает перенос блоков в настраиваемую систему работ.

## Что умеет

1. Игрок выбирает работу вручную или получает её автоматически в зоне подъёма.
2. Игрок приседает и стоит на месте, чтобы поднять разрешённый блок.
3. Блок становится переносимым грузом с визуальными эффектами.
4. Игрок несёт его в зону сдачи.
5. Игрок снова приседает и стоит на месте, чтобы сдать груз.
6. Плагин выдаёт награду и позже возвращает исходный блок в мир.

## Возможности

- Профили работ с отдельными настройками
- Pickup и dropoff кубоиды, хранящиеся в конфиге
- Геймплей с удержанием на месте при подъёме и сдаче
- Визуальный переносимый блок через `BlockDisplay`
- Замедление и опциональная усталость при переноске
- Навигационные частицы в сторону зоны сдачи
- Награды деньгами, опытом, командами консоли и предметами
- Полностью настраиваемые сообщения
- Безопасная очистка при отмене, выходе, перезагрузке и отключении работы

## Требования

- Сервер Minecraft `1.21+`
- Spigot или Paper
- Java `21`
- Опционально: `Vault` для денежных наград

Без `Vault` денежные награды пропускаются, остальные функции продолжают работать.

## Установка

1. Собери `jar` через Maven.
2. Положи файл в папку `plugins/` на сервере.
3. Один раз запусти сервер, чтобы сгенерировался конфиг.
4. Отредактируй `config.yml` или используй команды администратора в игре.
5. Перезагрузи плагин командой `/loader reload` или перезапусти сервер.

## Команды

### Команды игрока

```text
/loader list
/loader job <id>
/loader info
/loader cancel
```

### Команды администратора

```text
/loader job create <id>
/loader job delete <id>
/loader edit <job> ...
/loader region <job> ...
/loader config ...
/loader inspect <id>
/loader reload
```

Подробный гайд по настройке: [GUIDE-RU.md](GUIDE-RU.md)

## Права

- `loader.use` - базовый доступ к игровому процессу
- `loader.cancel` - отмена подъёма или переноски груза
- `loader.admin` - доступ к административным командам

Можно также задать отдельное право для каждой работы:

```yaml
permission: "loader.job.quarry"
```

## Кратко о конфиге

Основные разделы:

- `settings`
- `jobs`
- `regions`
- `messages`

### Пример настроек

```yaml
settings:
  auto-select-job-by-region: true
  update-interval-ticks: 2
  hold-max-move-distance: 0.18
  carry-particle-interval-ticks: 10
```

### Пример работы

```yaml
jobs:
  quarry_worker:
    enabled: true
    display-name: "Карьеровщик"
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

### Пример региона

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

## Коротко

- Работы можно включать и отключать прямо из игры.
- Если работу отключили во время переноски, сессия завершится безопасно.
- Если `allowed-blocks` пустой, можно переносить любой подходящий блок.
- Идентификаторы работ и регионов ограничены безопасным форматом.

## Файлы проекта

- Точка входа плагина: `src/main/java/ru/l1ratch/loaderwork/LoaderWork.java`
- Основная логика: `src/main/java/ru/l1ratch/loaderwork/LoaderController.java`
- Конфиг по умолчанию: `src/main/resources/config.yml`
- Метаданные плагина: `src/main/resources/plugin.yml`

## Лицензия

Перед публикацией добавь лицензию, которая подходит твоей модели распространения.
