# LoaderWork Current Functionality

This document is a concise public summary of the current plugin behavior.

## Core flow

1. A player selects a job manually or through auto-selection in the pickup region.
2. The player crouches and holds still to pick up a block.
3. The block becomes a carried visual object.
4. The player walks it to the dropoff region.
5. The player crouches and holds still again to complete delivery.
6. The plugin grants rewards and restores the original block later.

## Current systems

- Job profiles are stored in `config.yml`.
- Pickup and dropoff areas use custom cuboid regions.
- Jobs can be enabled or disabled from in-game commands.
- Rewards can include money, experience, console commands and items.
- Messages are fully configurable.
- The plugin cleans up sessions on cancel, disconnect, reload and disable.

## Notable constraints

- Job ids and region ids should use safe identifiers only.
- `Vault` is optional and only needed for money rewards.
- Empty `allowed-blocks` means any transportable block is allowed.

