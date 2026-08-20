# ThunderChat

A performance-focused Paper 1.21.11+ chat plugin for proxy networks.

ThunderChat provides local gamemode chat, permission-gated network channels, moderation filters, private messaging, ignore, local spy tools, mentions, per-channel mutes, MiniMessage Chat Color, and InteractiveChat-style interactive placeholders.

> **Development status:** ThunderChat is currently under active development. Back up your server data before upgrading development builds.

## Requirements

- Paper 1.21.11+
- Java 21
- MySQL 8+ or a compatible MySQL/MariaDB server when `storage.type: mysql` is selected
- LuckPerms and PlaceholderAPI are optional soft dependencies

## Quick start

1. Install ThunderChat in the Paper `plugins/` directory.
2. Start the server once so the default configuration files are generated.
3. Configure your database credentials in `config.yml`.
4. Make sure the MySQL account has permission to create the configured database/tables. ThunderChat creates its schema and tables automatically.
5. Restart the server after changing major storage/network settings.
6. Grant players the channel/filter/feature permissions they need.

For a proxy network, install ThunderChat on every backend and give each backend a unique `network.server-name` while using the same MySQL database.

## Channels

The built-in channels are:

| Channel | Purpose | Typical permission |
|---|---|---|
| `local` | Default gamemode/backend chat | configured local access |
| `global` | Network-wide chat | `thunderchat.channel.global` |
| `staff` | Staff-only chat | `thunderchat.channel.staff` |
| `donator` | Donator chat | `thunderchat.channel.donator` |
| `admin` | Admin chat | `thunderchat.channel.admin` |
| `highrank` | High-rank chat | `thunderchat.channel.highrank` |

Players start in `local`. `/channel <name>` switches the active channel. `/sc`, `/dc`, `/ac`, `/hc`, and `/gc` toggle their respective channels; disabling a toggle returns the player to local chat.

`/chathide` controls visibility only. Hiding a channel does **not** remove the player's permission to switch to it.

Channel permissions and formats are configurable. See `config.yml` and `messages.yml`.

## Commands

| Command | Purpose |
|---|---|
| `/msg`, `/tell`, `/w`, `/whisper <player> <message>` | Private message; supports cross-server targets |
| `/reply`, `/r <message>` | Reply to the last PM, including cross-server replies |
| `/ignore <player>` | Ignore a player; synchronized across the network when MySQL is enabled |
| `/unignore <player>` | Remove an ignore |
| `/channel`, `/ch <channel>` | Switch active channel |
| `/clearchat`, `/cc [channel]` | Clear one channel; defaults to local |
| `/chat clear [channel]` | Clear one channel through `/chat` |
| `/chat mute [channel] [player]` | Mute a channel globally or a player |
| `/chat unmute [channel] [player]` | Remove a mute |
| `/channelmutelist`, `/cml`, `/mutelist` | View active mutes |
| `/chathide [channel\|all]` | Hide a channel for yourself |
| `/staffchat`, `/sc` | Toggle staff chat |
| `/donatorchat`, `/dc` | Toggle donator chat |
| `/adminchat`, `/ac` | Toggle admin chat |
| `/highrankchat`, `/hc` | Toggle high-rank chat |
| `/gc`, `/globalchat` | Toggle global chat |
| `/spy <on\|off\|toggle\|status>` | Control local spy sections |
| `/chatcolor`, `/ccolor` | Open Chat Color GUI |
| `/chatcolor clear` | Reset formatting |
| `/thunderchat help [page]` | Show paginated help |
| `/thunderchat reload` | Reload supported configuration |

Run `/thunderchat help` in-game for the current server's complete command and permission help.

## Permissions

### Channels

- `thunderchat.channel.global`
- `thunderchat.channel.staff`
- `thunderchat.channel.donator`
- `thunderchat.channel.admin`
- `thunderchat.channel.highrank`

### Administration

- `thunderchat.admin`
- `thunderchat.msg`
- `thunderchat.ignore`
- `thunderchat.mention`

### Clear chat

- `thunderchat.clearchat.*`
- `thunderchat.clearchat.<channel>`
- `thunderchat.bypass.clearchat.*`
- `thunderchat.bypass.clearchat.<channel>`

### Mute bypass

- `thunderchat.bypass.mute`
- `thunderchat.bypass.mute.<channel>`

### Filters

- `thunderchat.bypass.filter`
- `thunderchat.bypass.spam`
- `thunderchat.bypass.flood`
- `thunderchat.bypass.caps`
- `thunderchat.bypass.swear`
- `thunderchat.bypass.advertisement`

### Filter alerts

- `thunderchat.alert.*`
- `thunderchat.alert.spam`
- `thunderchat.alert.flood`
- `thunderchat.alert.caps`
- `thunderchat.alert.swear`
- `thunderchat.alert.advertisement`

### Spy

Spy uses `thunderchat.command.spy`, section permissions, `thunderchat.spy.*`, `thunderchat.spy.autoenable`, and `thunderchat.bypass.spy`.

Spy is intentionally local-only. A player never receives their own spy output.

### Chat Color

Chat Color permissions are dynamically registered per option, for example:

- `thunderchat.chatcolor.color.red`
- `thunderchat.chatcolor.color.*`
- `thunderchat.chatcolor.gradient.rainbow`
- `thunderchat.chatcolor.gradient.*`
- `thunderchat.chatcolor.style.bold`
- `thunderchat.chatcolor.style.*`
- custom-format permission configured by the plugin

### Interactive chat

Interactive permissions include:

- `thunderchat.interactive.item`
- `thunderchat.interactive.inventory`
- `thunderchat.interactive.ender`
- `thunderchat.interactive.player`
- `thunderchat.interactive.commands`
- `thunderchat.interactive.position`
- `thunderchat.interactive.ping`
- `thunderchat.interactive.custom`

## Moderation pipeline

Normal chat can pass through multiple inexpensive checks before expensive similarity work. The filter system supports spam, flood, caps normalization, swear words and advertisements.

Private messages can use the same filter pipeline through `filter.private-messages.enabled`.

Caps are normalized rather than hard-blocked: an all-caps message is converted to normal case and the sender is warned. Blocking filters can generate staff alerts.

The swear list is configurable under `filter.swear.words` and contains English and Persian defaults. There is intentionally no separate blocked-words system; word-based moderation belongs in the swear section.

Filter bypass permissions are independent, so `thunderchat.bypass.spam` does not automatically bypass flood, swear or advertisement checks.

## Spy

Spy is deliberately **local-only**.

Supported spy sections include:

- commands
- private messages
- anvils
- signs
- books

Spy output is styled separately from normal chat so staff can distinguish it easily. `thunderchat.bypass.spy` prevents a player's protected commands/PMs from being exposed.

## Chat Color

`/chatcolor` opens a MiniMessage-based GUI for players with the required permission.

Players can select colors, gradients and stackable styles. A selected color and gradient replace each other; styles can be combined. `Clear` restores the default chat formatting.

Custom formatting is one-shot: the next message is consumed as the requested MiniMessage format and is not delivered, filtered or spied. Invalid or obfuscated formats are rejected.

The implementation uses Adventure/MiniMessage internally while preserving the configured visual appearance of the plugin.

## Interactive chat

ThunderChat includes an InteractiveChat-style subsystem. Current placeholders/features include:

| Placeholder | Purpose |
|---|---|
| `[item]`, `[i]` | Interactive held-item preview |
| `[inv]` | Read-only inventory snapshot |
| `[ender]`, `[e]` | Ender chest preview |
| `[pos]` | Player position |
| `[ping]` | Player ping |
| Player names | Hover/click interaction |
| Command placeholders | Click/suggest commands |
| Configured custom placeholders | Regex + PlaceholderAPI-backed replacements |

Inventory previews are strictly read-only: click and drag interactions cannot take items from the snapshot.

Interactive expansion is bounded by the configured placeholder limit to keep unusually large messages from becoming expensive.

## MySQL persistence

ThunderChat uses MySQL with HikariCP connection pooling.

The persistence path is:

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

Gameplay operations do not synchronously write YAML files. Dirty data is written asynchronously in batches and flushed during shutdown.

Persisted state includes channel state, mutes, ignores, spy settings and Chat Color settings.

### Database setup

ThunderChat creates its configured database/schema and tables automatically when the MySQL account has sufficient privileges. You do **not** need to manually create ThunderChat tables.

Example:

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

For existing development installations, missing MySQL records can be populated from the corresponding legacy YAML data during migration.

### Important

All backend servers should use the same database when they are part of the same network. Use separate `network.server-name` values for each backend.

## Network architecture

ThunderChat uses a unified versioned `ThunderChat` network dispatcher rather than separate incoming listeners in individual managers.

Network packets cover chat, clear, mute, alerts, ignore and private messages. Packet identifiers prevent duplicate delivery when a live packet and its queued fallback both exist.

### Empty backends

Bungee-style backend messaging traditionally needs an online player to carry a plugin message. ThunderChat uses a persistent MySQL queue as a fallback for configured backend servers.

Example:

```yaml
network:
  enabled: true
  server-name: "skymine"
  servers:
    - "skymine"
    - "bedwars"
  queue-retention-minutes: 1440
```

If a backend is empty, applicable packets remain queued until a player joins. This prevents ordinary chat/PM/mute/clear operations from being silently lost because a backend temporarily has zero players.

A future proxy-side implementation could remove the remaining dependency on backend carrier messaging entirely.

## Cross-server private messaging

`/msg` and `/reply` can target players on another backend. PMs use the network dispatcher and can be queued when the target backend is temporarily empty.

Ignore state is synchronized across the network when MySQL/network storage is enabled.

Spy remains local-only by design. A remote PM received on a backend can therefore be visible to spies **on that backend**, subject to the local spy permissions and bypass rules.

## Configuration files

### `config.yml`

Controls behavior and infrastructure:

- channels and permissions
- network settings
- MySQL/storage
- filter thresholds and word lists
- filter alerts
- mentions
- interactive-chat limits
- spy behavior
- feature toggles

### `messages.yml`

Controls player-facing text and presentation wherever supported:

- channel formats
- alert formats
- spy formats
- command responses
- Chat Color messages
- interactive-chat text
- help pages

This separation is intentional: change behavior in `config.yml`; change wording/presentation in `messages.yml`.

## Performance considerations

ThunderChat keeps the gameplay hot path primarily in memory:

- concurrent filter/channel state
- asynchronous batched persistence
- HikariCP connection pooling
- centralized network packet parsing
- duplicate packet suppression
- bounded interactive placeholder expansion
- cheap moderation checks before similarity calculations

PlaceholderAPI and MiniMessage evaluation can still become expensive when formats contain many complex placeholders or when a large network generates very high chat volume. Keep formats reasonably simple and use the interactive placeholder limit to prevent pathological messages.

## Troubleshooting

### MySQL connection fails

Check:

1. MySQL/MariaDB is running.
2. Host and port are reachable from the Paper server.
3. Username/password are correct.
4. The account has permission to create the configured database/tables.
5. Every backend uses the intended shared database.

### Cross-server chat is not arriving

Check:

1. `network.enabled: true`.
2. Every backend has a unique `network.server-name`.
3. Every backend appears in `network.servers`.
4. All backends use the same MySQL database.
5. The proxy/plugin-message setup is correct.

### A player cannot use a channel

Check the corresponding `thunderchat.channel.<channel>` permission and the channel's configured permission node.

### A player can see a channel but cannot speak in it

Visibility and access are separate. `/chathide` only hides a channel from that player; it does not grant or remove channel permissions.

### Chat Color is not applying

Check the relevant color/gradient/style/custom permission and confirm that `/chatcolor clear` has not reset the player's selection.

### `[inv]` cannot be interacted with

That is intentional. Inventory previews are read-only and clicks/drags are blocked to prevent item theft.

## Building

Requires Java 21.

```bash
./gradlew build
```

The plugin JAR is generated in `build/libs/`.

## Credits and third-party notices

ThunderChat uses/adapts ideas and source from open-source projects where their licenses permit it. See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for detailed attribution and license information.

In particular:

- **InteractiveChat** — LOOHP / InteractiveChat
- **CleanStaffChat** — credited for inspiration used for staff-list presentation

ThunderChat is GPLv3. Consult the repository license and third-party notices before redistributing modified builds.

## Development notes

ThunderChat is designed as a modular Paper plugin with separate managers for chat, filtering, network messaging, persistence, spy, interactive chat and Chat Color.

When adding a feature, prefer:

1. configuration in `config.yml`;
2. player-facing text in `messages.yml`;
3. Adventure Components/MiniMessage for formatting;
4. asynchronous I/O for persistent storage;
5. the unified network dispatcher for cross-backend state/messages.

Keep the main gameplay path lightweight and avoid blocking database or file operations from chat/event threads.
