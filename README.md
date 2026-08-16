# ThunderChat

A configurable Paper chat plugin for Velocity/Bungee networks with local gamemode chat, global and permission-based network channels, moderation filters, private messages, chat visibility controls, mute/clear tools, mentions, filter alerts, and local staff spying.

## Channels

Players start in local gamemode chat. Available channels are local, global, staff, donator, admin, and highrank. Active channel selection is separate from visibility: players can hide channels without changing what they can speak in.

## Moderation

- Spam detection with configurable scoring, repeat limits, and cooldowns
- Anti-flood for repeated consecutive characters
- Anti-caps normalization with bypass permission
- Configurable English/Persian swear filter
- Advertisement detection for IPs, domains, and configured server names
- Configurable blocked-word filter
- Network-wide filter alerts with per-alert permissions

## Messaging

- `/msg`, `/reply` and aliases
- Configurable private-message filtering
- Ignore system with optional public-chat suppression
- Local command/private-message spy channels
- Configurable mentions with sound and highlighting

## Administration

- `/chat clear|mute|unmute`
- `/clearchat` / `/cc`
- `/channelmutelist`
- `/chathide`
- `/thunderchat reload|info`
- Per-channel permissions and bypass permissions
- YAML persistence for ignores, spy settings, and mutes

## Configuration

Channel formats and spy/alert formats support ThunderChat placeholders plus PlaceholderAPI placeholders when PlaceholderAPI is installed.

See `src/main/resources/config.yml` for all options and defaults.
