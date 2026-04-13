# LoaderWork: подробный гайд по настройке

Этот гайд написан для людей, которые не хотят разбираться в коде и YAML вручную. Почти всё можно настроить прямо в игре командами.

## Что делает плагин

Игрок:

1. выбирает работу;
2. приходит в зону взятия;
3. приседает и удерживает блок;
4. несёт блок в зону сдачи;
5. снова приседает и удерживает позицию;
6. получает награду.

## Что нужно заранее

- сервер Minecraft `1.21+`;
- Spigot или Paper;
- Java `21`;
- `Vault`, если нужны денежные награды;
- право `loader.admin`, если вы будете настраивать плагин.

## Быстрый старт

Если хотите создать простую работу с нуля:

```text
/loader job create quarry
/loader edit quarry set name Quarry Worker
/loader region quarry pickup pos1
/loader region quarry pickup pos2
/loader region quarry dropoff pos1
/loader region quarry dropoff pos2
/loader edit quarry blocks add stone cobblestone iron_ore
/loader edit quarry reward default set money 1
/loader edit quarry reward default set experience 1
/loader inspect quarry
/loader reload
```

## Команды администратора

### Создать и удалить работу

```text
/loader job create <id>
/loader job delete <id>
```

Пример:

```text
/loader job create quarry
```

### Посмотреть работу

```text
/loader inspect <id>
```

Пример:

```text
/loader inspect quarry
```

### Редактировать работу

```text
/loader edit <job> show
/loader edit <job> set <field> <value>
/loader edit <job> blocks <list|add|remove|clear>
/loader edit <job> reward <default|block>
```

### Редактировать регионы

```text
/loader region <job> pickup pos1
/loader region <job> pickup pos2
/loader region <job> pickup show
/loader region <job> pickup clear

/loader region <job> dropoff pos1
/loader region <job> dropoff pos2
/loader region <job> dropoff show
/loader region <job> dropoff clear
```

### Глобальные настройки

```text
/loader config show
/loader config set auto-select-job-by-region true
/loader config set update-interval-ticks 2
/loader config set hold-max-move-distance 0.18
/loader config set carry-particle-interval-ticks 10
```

## Как задавать зоны

Зона состоит из двух точек:

- `pos1`
- `pos2`

Это обычный кубоид: плагин сам понимает, где низ и верх.

### Как поставить точки

Встаньте в первый угол зоны и выполните:

```text
/loader region quarry pickup pos1
```

Потом встаньте в противоположный угол:

```text
/loader region quarry pickup pos2
```

То же самое для зоны сдачи:

```text
/loader region quarry dropoff pos1
/loader region quarry dropoff pos2
```

### Как проверить зону

```text
/loader region quarry pickup show
/loader region quarry dropoff show
```

### Как очистить зону

```text
/loader region quarry pickup clear
/loader region quarry dropoff clear
```

## Как настроить работу

### Название

```text
/loader edit quarry set name Quarry Worker
```

### Право доступа

```text
/loader edit quarry set permission loader.job.quarry
```

Если право не нужно:

```text
/loader edit quarry set permission clear
```

### Включить или выключить работу

```text
/loader edit quarry set enabled true
/loader edit quarry set enabled false
```

### Время взятия и сдачи

```text
/loader edit quarry set pickup-hold 40
/loader edit quarry set dropoff-hold 30
/loader edit quarry set respawn 600
```

Здесь:

- `pickup-hold` - сколько тиков нужно стоять при взятии;
- `dropoff-hold` - сколько тиков нужно стоять при сдаче;
- `respawn` - через сколько тиков блок вернётся в мир.

### Настроить перенос

```text
/loader edit quarry set carry-slow 2
/loader edit quarry set carry-fatigue 1
/loader edit quarry set carry-nav 20
/loader edit quarry set carry-height 0.35
/loader edit quarry set carry-offset 0.95
```

Здесь:

- `carry-slow` - насколько сильно замедлять игрока;
- `carry-fatigue` - включать ли усталость;
- `carry-nav` - как часто показывать навигационные частицы;
- `carry-height` - высота визуального блока;
- `carry-offset` - насколько далеко блок идёт за игроком.

## Как настроить блоки

### Посмотреть список

```text
/loader edit quarry blocks list
```

### Добавить блоки

```text
/loader edit quarry blocks add stone cobblestone iron_ore
```

### Убрать блоки

```text
/loader edit quarry blocks remove dirt gravel
```

### Очистить список

```text
/loader edit quarry blocks clear
```

Если список пустой, разрешены все переносимые блоки.

## Как настроить награды

Есть два уровня наград:

- `default` - награда по умолчанию;
- `block` - отдельная награда для конкретного блока.

### Награда по умолчанию

Посмотреть:

```text
/loader edit quarry reward default show
```

Деньги:

```text
/loader edit quarry reward default set money 1.5
```

Опыт:

```text
/loader edit quarry reward default set experience 2
```

Команда:

```text
/loader edit quarry reward default addcmd say %player% сдал груз
```

Удалить команду:

```text
/loader edit quarry reward default delcmd 1
```

Предмет:

```text
/loader edit quarry reward default additem torch 4
```

Удалить предмет:

```text
/loader edit quarry reward default delitem 1
```

### Награда для конкретного блока

Посмотреть:

```text
/loader edit quarry reward block iron_ore show
```

Деньги:

```text
/loader edit quarry reward block iron_ore set money 5
```

Опыт:

```text
/loader edit quarry reward block iron_ore set experience 3
```

Команда:

```text
/loader edit quarry reward block iron_ore addcmd effect give %player% minecraft:haste 20 0 true
```

Предмет:

```text
/loader edit quarry reward block iron_ore additem torch 4
```

Очистить награду блока:

```text
/loader edit quarry reward block iron_ore clear
```

## Глобальные настройки

### Показать текущие значения

```text
/loader config show
```

### Что означают настройки

- `auto-select-job-by-region` - автоматически выбирать работу, если игрок вошёл в её зону взятия;
- `update-interval-ticks` - как часто обновляется логика плагина;
- `hold-max-move-distance` - насколько игрок может сдвинуться и всё ещё считаться стоящим на месте;
- `carry-particle-interval-ticks` - как часто показывать частицы при переноске.

## Как это должно настраиваться по шагам

1. Создайте работу.
2. Поставьте зоны взятия и сдачи.
3. Добавьте разрешённые блоки.
4. Настройте награды.
5. Проверьте работу через `/loader inspect`.
6. Только потом включайте её для игроков.

## Полезные примечания

- Если игрок отменяет перенос, блок возвращается сразу.
- Если плагин перезагружается во время переноски, состояние очищается безопасно.
- Имена работ и регионов лучше делать простыми: латиница, цифры, `_` и `-`.
- Для обычных серверов лучше начинать с простых значений времени: 30-40 тиков на взятие и 20-30 тиков на сдачу.

## Типичный пример

```text
/loader job create quarry
/loader edit quarry set name Quarry Worker
/loader region quarry pickup pos1
/loader region quarry pickup pos2
/loader region quarry dropoff pos1
/loader region quarry dropoff pos2
/loader edit quarry blocks add stone cobblestone iron_ore coal_ore gravel
/loader edit quarry reward default set money 1
/loader edit quarry reward default set experience 1
/loader inspect quarry
/loader reload
```
