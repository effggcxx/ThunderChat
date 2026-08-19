# ThunderChat

A performance-focused Paper 1.21.11+ chat plugin for proxy networks. ThunderChat provides local gamemode chat, permission-gated global/staff/donator/admin/high-rank channels, moderation filters, private messages, ignore, local spy tools, mentions, per-channel mutes, MiniMessage Chat Color, and InteractiveChat-style interactive placeholders.

**Requirements:** Paper 1.21.11+, Java 21. LuckPerms and PlaceholderAPI are optional soft dependencies.

## Channels

ThunderChat has six channels:

- `local` — the default gamemode/backend chat.
- `global` — network-wide chat.
- `staff`
- `donator`
- `admin`
- `highrank`

Players start in `local`. `/channel <name>` switches the active channel; `/sc`, `/dc`, `/ac`, `/hc`, and `/gc` toggle their respective channels. Hiding a channel with `/chathide` only changes visibility and does not revoke access.

Channel permissions are configurable under `channels.<name>.permission`.

## Commands

| Command | Purpose |
|---|---|
| `/msg`, `/tell`, `/w`, `/whisper <player> <message>` | Private message; supports cross-server targets |
| `/reply`, `/r <message>` | Reply to the last PM, including cross-server replies |
| `/ignore <player>` | Ignore a player; synchronized across the network when MySQL is enabled |
| `/unignore <player>` | Remove an ignore |
| `/channel`, `/ch <channel>` | Switch active channel |
| `/clearchat`, `/cc [channel]` | Clear a specific channel; defaults to local |
| `/chat clear [channel]` | Same clear operation through the chat command |
| `/chat mute [channel] [player]` | Mute a channel globally or a player |
| `/chat unmute [channel] [player]` | Remove a mute |
| `/channelmutelist`, `/cml`, `/mutelist` | View mutes |
| `/chathide [channel\|all]` | Hide a channel for yourself |
| `/staffchat`, `/sc` | Toggle staff chat |
| `/donatorchat`, `/dc` | Toggle donator chat |
| `/adminchat`, `/ac` | Toggle admin chat |
| `/highrankchat`, `/hc` | Toggle high-rank chat |
| `/gc`, `/globalchat` | Toggle global chat |
| `/spy <on\|off\|toggle\|status>` | Control local spy sections |
| `/chatcolor`, `/ccolor` | Open Chat Color GUI |
| `/chatcolor clear` | Reset formatting |
| `/thunderchat reload` | Reload configuration |
| `/thunderchat help [page]` | Multi-page command help |

## Permissions

Core permissions include:

- `thunderchat.admin`
- `thunderchat.msg`
- `thunderchat.ignore`
- `thunderchat.mention`
- `thunderchat.channel.global`
- `thunderchat.channel.staff`
- `thunderchat.channel.donator`
- `thunderchat.channel.admin`
- `thunderchat.channel.highrank`

Clear-chat permissions are granular:

- `thunderchat.clearchat.*`
- `thunderchat.clearchat.<channel>`
- `thunderchat.bypass.clearchat.*`
- `thunderchat.bypass.clearchat.<channel>`

Mute bypass permissions are:

- `thunderchat.bypass.mute`
- `thunderchat.bypass.mute.<channel>`

Filter bypass permissions are separate:

- `thunderchat.bypass.filter`
- `thunderchat.bypass.spam`
- `thunderchat.bypass.flood`
- `thunderchat.bypass.caps`
- `thunderchat.bypass.swear`
- `thunderchat.bypass.advertisement`

Filter alerts use:

- `thunderchat.alert.*`
- `thunderchat.alert.spam`
- `thunderchat.alert.flood`
- `thunderchat.alert.caps`
- `thunderchat.alert.swear`
- `thunderchat.alert.advertisement`

Spy permissions include `thunderchat.command.spy`, `thunderchat.spy.*`, the individual section permissions, `thunderchat.spy.autoenable`, and `thunderchat.bypass.spy`.

Chat Color permissions are dynamically registered for each color, gradient and style, for example `thunderchat.chatcolor.color.red`, `thunderchat.chatcolor.gradient.rainbow`, and `thunderchat.chatcolor.style.bold`. Wildcards such as `thunderchat.chatcolor.color.*` are also supported.

Interactive permissions include `thunderchat.interactive.item`, `.inventory`, `.ender`, `.player`, `.commands`, `.position`, `.ping`, and `.custom`.

## Moderation

The normal chat filter pipeline checks spam, flood, swear words and advertisements. PMs can use the same pipeline through `filter.private-messages.enabled`.

Caps are normalized rather than blocked: all-caps messages are converted to normal case and the sender is warned. Every blocking filter can generate an alert. Alerts can be enabled/disabled individually and can be broadcast over the network.

The swear list is configurable under `filter.swear.words` and contains both English and Persian defaults. There is intentionally no separate "blocked words" system; word-based moderation belongs in the swear section.

## Spy

Spy is intentionally **local-only**. It never becomes network-wide.

Available sections:

- commands
- private messages
- anvils
- signs
- books

The person being spied on never receives their own spy output. `thunderchat.bypass.spy` prevents a player's commands/PMs from being exposed.

## Chat Color

`/chatcolor` uses MiniMessage. Players without the permission cannot change their chat formatting.

Available presets include solid colors, gradients and stackable styles. Custom formatting is one-shot: the next message is consumed as the format and is not delivered, filtered or spied. Invalid/obfuscated formats are rejected.

## Interactive chat

ThunderChat includes an InteractiveChat-style subsystem. Current built-in placeholders include:

| Placeholder | Purpose |
|---|---|
| `[item]`, `[i]` | Interactive held-item preview |
| `[inv]` | Read-only inventory snapshot |
| `[ender]`, `[e]` | Ender chest preview |
| `[pos]` / position placeholders | Player position |
| `[ping]` | Player ping |
| Player names | Hover/click interaction |
| Command placeholders | Click/suggest commands |
| Configured custom placeholders | Regex + PlaceholderAPI-backed replacements |

Inventory previews are strictly read-only: click and drag interactions cannot take items from the snapshot.

## MySQL persistence

ThunderChat now uses MySQL as its persistent storage backend with HikariCP connection pooling.

The storage architecture is:

```text
Gameplay state
     ↓
in-memory cache
     ↓
dirty-state batching
     ↓
async JDBC/HikariCP
     ↓
MySQL
```

Gameplay operations do not synchronously write YAML files. Writes are debounced and flushed in batches; the storage layer is also flushed during shutdown.

Persistent state migrated to the MySQL-backed store includes:

- active/hidden channel state
- mutes
- ignores
- spy settings
- Chat Color settings

If a MySQL record does not exist, the corresponding legacy YAML file is imported automatically. This gives existing development installations a migration path without manually recreating their data.

### MySQL configuration

Configure this in `config.yml`:

```yaml
storage:
  type: mysql
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "thunderchat"
    username: "root"
    password: "change-me"
    pool-size: 4
```

Create the database before first startup and give the configured MySQL user permission to create tables.

## Network architecture

ThunderChat uses one unified BungeeCord-compatible `ThunderChat` dispatcher instead of separate incoming listeners in individual managers.

The protocol is versioned and routes chat, clear, mute, alert, ignore and PM packets.

### Empty-backend reliability

Bungee-style plugin messaging needs an online player to originate a packet. ThunderChat therefore has a MySQL fallback queue for configured backend servers.

Configure every backend here:

```yaml
network:
  enabled: true
  server-name: "skymine"
  servers:
    - "skymine"
    - "bedwars"
  queue-retention-minutes: 1440
```

When a backend is empty, queued packets remain available until a player joins. Packet IDs prevent a live packet and its queued fallback from being delivered twice.

This is still a backend-plugin solution. A future Velocity-side implementation can remove the remaining dependency on Bungee-style carrier messaging entirely.

## Cross-server private messages

`/msg` and `/reply` can now target players on another backend. PM packets are forwarded through the same network dispatcher and queued for configured backends when necessary.

Ignore state is synchronized across the network as well. Spy remains local-only by design, but PMs delivered on another backend can be seen by spies on that backend according to their local spy permissions.

## Configuration and messages

Player-facing text belongs in `messages.yml` wherever practical. Channel formats, alert formats, spy formats, interactive text, help pages and Chat Color messages are configurable.

`config.yml` controls behavior: permissions, filter rules, channels, network settings, MySQL, storage and feature toggles.

## Performance notes

ThunderChat is designed to keep the hot chat path in memory:

- filter state uses concurrent collections
- channel state uses concurrent collections
- persistence is asynchronous and batched
- HikariCP pools database connections
- network packet parsing is centralized
- duplicate network packets are suppressed by packet IDs
- interactive expansion is bounded by `interactive.max-placeholders`

The biggest remaining performance-sensitive area is PlaceholderAPI/MiniMessage processing on very large networks; avoid unnecessarily complex per-message formats and placeholder chains.

## Building

Requires Java 21.

```bash
./gradlew build
```

The resulting jar is placed in `build/libs/`.

## Credits and third-party notices

ThunderChat uses/adapts ideas and source from open-source projects where their licenses permit it. See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for the detailed attribution and license information.

In particular:

- InteractiveChat — LOOHP / InteractiveChat
- CleanStaffChat — credited for inspiration used for the staff-list presentation

ThunderChat is GPLv3. Consult the repository license and third-party notices before redistributing modified builds.
