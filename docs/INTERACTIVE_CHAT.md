# ThunderChat Interactive Chat

ThunderChat now provides a Paper-native interactive-chat subsystem inspired by the feature set of LOOHP InteractiveChat.

## Built-in placeholders

| Placeholder | Permission | Result |
|---|---|---|
| `[item]`, `[i]` | `thunderchat.interactive.item` | Interactive main-hand item with hover details and optional item preview click. |
| `[inv]`, `[inventory]` | `thunderchat.interactive.inventory` | Read-only inventory snapshot. |
| `[ender]`, `[e]` | `thunderchat.interactive.ender` | Read-only Ender Chest snapshot. |
| `[pos]` | `thunderchat.interactive.position` | Current world and block coordinates. |
| `[ping]` | `thunderchat.interactive.ping` | Current player ping. |

`thunderchat.interactive.*` grants the complete built-in feature set.

## Player-name interaction

With `thunderchat.interactive.player`, matching online player names become interactive. Hovering shows configurable world, ping and health information; clicking suggests `/msg <player>`.

The behavior is intentionally limited to online players on the current backend. This avoids pretending that a backend-local Bukkit Player object exists for a remote server.

## Command interaction

With `thunderchat.interactive.commands`, slash commands appearing in chat can be rendered as clickable/suggestable command components. Configure the visible format and hover text under `interactive.commands` in `config.yml`.

## Custom placeholders

Custom placeholders are configured under `interactive.custom-placeholders`:

```yaml
interactive:
  custom-placeholders:
    server:
      keyword: "(?i)\\[server\\]"
      replace: "<aqua>%server_name%</aqua>"
      cooldown: 0
```

- `keyword` is a Java regular expression.
- `replace` can use PlaceholderAPI when PlaceholderAPI is installed.
- `cooldown` is per-player and measured in seconds.
- Use `thunderchat.interactive.custom.<id>` for a specific custom placeholder.
- `thunderchat.interactive.custom.*` grants all configured custom placeholders.

## Limits and cooldowns

`interactive.max-placeholders` prevents messages from expanding an unreasonable number of built-in placeholders. Set it to `-1` to disable the limit.

The individual item/inventory/ender cooldown settings are reserved for per-placeholder throttling and are deliberately configurable rather than hardcoded. `interactive.universal-cooldown` is the global configuration point for future universal placeholder throttling.

## Inventory safety

Inventory, Ender Chest and item previews are **read-only snapshots**. ThunderChat registers a dedicated inventory holder and cancels both click and drag events for those views. The displayed items are clones, so clicking an `[inv]` link cannot steal or modify the source player's items.

## Network behavior

The interactive component is attached to the already-rendered chat component. Network channels serialize that component using Adventure's Gson component serializer, so hover/click metadata survives network forwarding.

Viewer actions that open a Bukkit inventory still require the referenced player to be online on the backend handling the click. Cross-server inventory access requires a future proxy-side implementation; the current system intentionally fails safely instead of exposing arbitrary inventories.

## Configuration

All feature switches and behavior belong in `config.yml`; player-facing text belongs in `messages.yml`. This follows ThunderChat's general rule that user-visible output should not be hardcoded.

## Attribution

The feature set and several implementation patterns are adapted from LOOHP's InteractiveChat project. See `THIRD_PARTY_NOTICES.md` and the project license for attribution and licensing information.
