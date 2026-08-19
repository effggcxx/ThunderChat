# ThunderChat — Full Documentation

A Paper chat plugin with local gamemode chat, permission-gated network
channels (BungeeCord/Velocity legacy forwarding), moderation filters,
private messaging with ignore/spy, per-channel mutes, mentions,
InteractiveChat-style clickable/hoverable chat placeholders, and a
MiniMessage-based Chat Color menu. Requires Paper 1.21.11+ and Java 21.
LuckPerms and PlaceholderAPI are optional soft-dependencies — both are
only used for prefix/placeholder resolution in chat formats.

## Channels

Six channels exist: `local`, `global`, `staff`, `donator`, `admin`, and
`highrank`. `local` is server-only and always available; the other five
are network channels — messages in them are forwarded to every other
server on the proxy, gated behind a permission
(`thunderchat.channel.<name>`, configurable per-channel in `config.yml`
under `channels:`).

Every player has one **active** channel (what typing in chat sends to)
and any number of **hidden** channels (what they don't want to see),
tracked independently — hiding a channel doesn't change what you can
speak in, and switching your active channel doesn't un-hide anything.
If your active channel becomes unusable (permission revoked, or you hid
it) your next message silently falls back to `local`. Both your active
channel and your hidden set persist across restarts in
`channel-state.yml` (as long as `storage.type: yaml`, the only mode
currently implemented).

## Commands

| Command | Description |
|---|---|
| `/msg \| /tell \| /w \| /whisper <player> <message>` | Send a private message |
| `/reply \| /r <message>` | Reply to the last player who messaged you |
| `/ignore <player>` | Toggle ignoring a player |
| `/unignore <player>` | Explicitly stop ignoring a player |
| `/channel \| /ch [name]` | Show or switch your active channel |
| `/clearchat \| /cc [channel]` | Clear a channel's chat (defaults to local) |
| `/chathide [channel\|all]` | Toggle visibility of a channel for yourself |
| `/staffchat \| /sc` | Toggle staff chat as your active channel |
| `/donatorchat \| /dc` | Toggle donator chat |
| `/adminchat \| /ac` | Toggle admin chat |
| `/highrankchat \| /hc` | Toggle high-rank chat |
| `/gc` | Toggle global chat |
| `/chat <clear\|mute\|unmute> [channel] [player]` | Clear or mute a channel, server-wide or per-player |
| `/channelmutelist \| /cml \| /mutelist [page]` | List every muted channel and muted player, paginated |
| `/spy <on\|off\|status\|toggle> [commands\|private-messages]` | Locally monitor commands and/or private messages |
| `/chatcolor \| /ccolor [clear]` | Open the Chat Color menu, or clear your formatting |
| `/thunderchat \| /tc <reload\|info>` | Reload config + mute data, or show plugin status |

All of the above tab-complete channel names, online player names, and
subcommands where relevant.

## Permissions

### Core

| Permission | Default | Covers |
|---|---|---|
| `thunderchat.admin` | op | `/thunderchat reload\|info`, managing chat mutes via `/chat mute\|unmute` |
| `thunderchat.msg` | true | Sending and receiving private messages |
| `thunderchat.ignore` | true | Using `/ignore` and `/unignore` |
| `thunderchat.mention` | true | Triggering `@player` highlights/sounds when you send them |
| `thunderchat.channelmutelist` | op | Viewing `/channelmutelist` |

### Channel access

| Permission | Default | Covers |
|---|---|---|
| `thunderchat.channel.global` | op | Access to global chat |
| `thunderchat.channel.staff` | op | Access to staff chat |
| `thunderchat.channel.donator` | false | Access to donator chat |
| `thunderchat.channel.admin` | op | Access to admin chat |
| `thunderchat.channel.highrank` | false | Access to high-rank chat |

`local` needs no permission. Each of these is remappable per-channel via
`channels.<name>.permission` in `config.yml` if you'd rather use your
own node names.

### Clearing chat

| Permission | Default | Covers |
|---|---|---|
| `thunderchat.clearchat.*` | op | All six `clearchat.<channel>` permissions below |
| `thunderchat.clearchat.local\|global\|staff\|donator\|admin\|highrank` | op | Clearing that specific channel |
| `thunderchat.bypass.clearchat.*` | op | All six bypass permissions below |
| `thunderchat.bypass.clearchat.local\|global\|staff\|donator\|admin\|highrank` | op | Keeps your chat history visible when that channel is cleared |

### Mute bypass

| Permission | Default | Covers |
|---|---|---|
| `thunderchat.bypass.mute` | op | Bypasses every mute, in every channel |
| `thunderchat.bypass.mute.local\|global\|staff\|donator\|admin\|highrank` | op | Bypasses a mute in one specific channel |

### Filters

| Permission | Default | Covers |
|---|---|---|
| `thunderchat.bypass.filter` | op | Bypasses spam, flood, swear, and advertisement filters entirely |
| `thunderchat.bypass.spam` | op | Bypasses spam detection only |
| `thunderchat.bypass.flood` | op | Bypasses the anti-flood filter |
| `thunderchat.bypass.caps` | op | Bypasses caps normalization |
| `thunderchat.bypass.swear` | op | Bypasses the swear-word filter |
| `thunderchat.bypass.advertisement` | op | Bypasses the anti-advertisement filter |

### Filter alerts

| Permission | Default | Covers |
|---|---|---|
| `thunderchat.alert.*` | op | Receive every alert type below |
| `thunderchat.alert.spam\|flood\|caps\|swear\|advertisement` | op | Receive that specific alert type when a player trips the filter |

### Chat visibility (`/chathide`)

| Permission | Default | Covers |
|---|---|---|
| `thunderchat.chathide.*` | op | All seven permissions below |
| `thunderchat.chathide.local\|global\|staff\|donator\|admin\|highrank` | true | Hiding that specific channel for yourself |
| `thunderchat.chathide.all` | true | `/chathide all` — hide every channel you can see at once |

### Spy

| Permission | Default | Covers |
|---|---|---|
| `thunderchat.command.spy` | op | Using `/spy` at all |
| `thunderchat.spy.autoenable` | op | Both spy sections auto-enable the first time you join |
| `thunderchat.bypass.spy` | false | Your commands and private messages are never shown to spies |

### Chat Color

| Permission | Default | Covers |
|---|---|---|
| `thunderchat.chatcolor` | false | Opening `/chatcolor` and applying any formatting at all |
| `thunderchat.chatcolor.color.*` | false | Every solid color below |
| `thunderchat.chatcolor.color.<name>` | false | One specific color — see the list under [Chat Color](#chat-color) |
| `thunderchat.chatcolor.gradient.*` | false | Every gradient below |
| `thunderchat.chatcolor.gradient.<name>` | false | One specific gradient |
| `thunderchat.chatcolor.style.*` | false | Every style below |
| `thunderchat.chatcolor.style.<name>` | false | One specific style |
| `thunderchat.chatcolor.custom.*` | false | Custom MiniMessage formatting (implies `thunderchat.chatcolor.custom`) |
| `thunderchat.chatcolor.custom` | false | Typing your own MiniMessage format instead of picking a preset |

The per-color/gradient/style permissions aren't in `plugin.yml` — they're
registered at startup from the same lists that drive the GUI, so adding
a new gradient in code automatically gets a matching permission node.

### Interactive chat

| Permission | Default | Covers |
|---|---|---|
| `thunderchat.interactive.item` | true | Using the `[item]`/`[i]` placeholder |
| `thunderchat.interactive.inventory` | true | Using the `[inv]` placeholder, and opening an inventory it links to |

## Moderation filters

All of these run through `FilterManager` in one pipeline, checked in
order (first match blocks the message): spam → flood → swear →
advertisement. `thunderchat.bypass.filter` skips all of them; the
per-filter bypass permissions each skip just their one check.
Caps handling is separate — see below.

**Spam** scores a message against your previous one: higher similarity,
a shorter gap since your last message, and a streak of near-identical
messages each add to the score. It blocks once the score clears
`filter.spam.score-threshold`, or once your repeat streak alone hits
`filter.spam.max-repeated-messages` — whichever comes first.
`filter.spam.cooldown-seconds` is the window comparisons happen in; outside
it, your streak resets and the new message is compared against nothing.

**Flood** blocks a message once the same character repeats
`filter.flood.max-consecutive-characters` times in a row (`"heyyyy"`,
`"!!!!!!"`).

**Caps** isn't a block — `CapsManager` lowercases an all-caps message
(100% of its letters uppercase, at least `filter.caps.min-length-to-check`
letters long) and warns the sender, rather than rejecting it outright.
`thunderchat.bypass.caps` skips this.

**Swear** matches the configurable word list in `filter.swear.words`,
which ships with English and Persian entries. This is the single
word-based moderation list; add or remove terms there as needed.

**Advertisement** blocks IPv4 addresses, domain-shaped text
(`something.tld`), and any name in `filter.advertisement.server-names` —
each independently toggleable.

`filter.private-messages.enabled` runs this same pipeline against `/msg`
and `/reply` content too (default on) — a blocked PM never reaches the
recipient or gets logged.

Every block that isn't a caps-normalize also fires `AlertManager`, which
messages every player holding `thunderchat.alert.<type>` (or `.alert.*`)
both locally and, if `alerts.broadcast-network` is on, across every
other server on the proxy.

## Private messaging

`/msg`/`/tell`/`/w`/`/whisper` and `/reply`/`/r` both set each other as
your reply target, so `/r` always points at whoever you last exchanged
messages with, in either direction. `/ignore` blocks a player's PMs from
reaching you (and tells them so); set `ignore.public-chat.enabled: true`
in `config.yml` to also hide their messages in the channels you share.
`private-messages.log-to-console` optionally logs every PM to the
server console.

`/spy on|off|toggle commands|private-messages` lets staff (with
`thunderchat.command.spy`) watch commands and/or PMs run by everyone
else on this server — locally only, never forwarded across the network.
`thunderchat.bypass.spy` exempts a specific player from ever being
shown, on either side of a PM.

## Mentions

Typing `@PlayerName` in a channel message (if you hold
`thunderchat.mention`) highlights their name in
`mentions.highlight-color` for everyone who sees the message, and plays
`mentions.sound` for the mentioned player specifically. Toggle the whole
feature off with `mentions.enabled: false`.

## Interactive chat

Lightweight, InteractiveChat-style placeholders that any player holding
the right permission can type directly into chat — no external plugin
required. `InteractiveChatManager` expands them after the message is
otherwise formatted, so they work in both local and network channels.

| Placeholder | Permission | Shows | On click |
|---|---|---|---|
| `[item]` / `[i]` | `thunderchat.interactive.item` | The item in the sender's main hand (name + hover with amount); `[No item]` if empty-handed | — |
| `[inv]` | `thunderchat.interactive.inventory` | An `[Inventory]` tag, hover-labeled with the sender's name | Opens a read-only 54-slot snapshot of the sender's inventory (hotbar/main/armor/off-hand) for the clicker |

Both permissions default to `true`. All of the visible text, hover
text, and error messages above are configurable under `interactive:`
in `messages.yml`.

Clicking `[inv]` runs `/thunderchat inventory <uuid>` behind the
scenes — this isn't meant to be typed manually, it exists only as the
click-event target, and it no-ops if the target player has since gone
offline or the link is malformed.

This feature is intentionally minimal today — a placeholder for the
sender's own item/inventory, not arbitrary item-in-hand-of-anyone
lookups, particle/durability detail, or slot-by-slot armor previews.
More placeholders are planned; see [Credits](#credits).

## Chat Color

`/chatcolor` opens a GUI: a solid color or gradient, one or more text
styles (bold/italic/underlined/strikethrough, stackable), or a custom
MiniMessage format — each gated by its own permission, so you can sell
specific colors/gradients as perks without an all-or-nothing switch.
Locked options still show in the menu (barrier icon, "No permission")
rather than being hidden, so players know what exists to unlock.

**Custom formatting**: picking "Custom" and confirming puts you into a
one-shot capture — your next chat message is read as a MiniMessage
template instead of being sent as chat (not filtered, not delivered).
`<obfuscated>`/`<obf>` tags are rejected outright; anything else invalid
MiniMessage is rejected with an error and nothing is changed. Your
actual message content is escaped before being substituted in, so a
custom-format player can't use their chat messages to inject arbitrary
extra tags around what they type.

Selecting a color/gradient/style/custom format is mutually exclusive —
picking a gradient clears any solid color and custom format you had,
and vice versa (styles are the exception: they stack with a color or
gradient, but are cleared if you switch to a custom format). Everything
persists to `chat-colors.yml`; `/chatcolor clear` wipes it back to the
plugin's default chat format.

Colors: `black`, `dark_blue`, `dark_green`, `dark_aqua`, `dark_red`,
`dark_purple`, `gold`, `gray`, `dark_gray`, `blue`, `green`, `aqua`,
`red`, `light_purple`, `yellow`, `white`.
Gradients: `sunset`, `ocean`, `forest`, `fire`, `candy`, `aurora`,
`rainbow`.
Styles: `bold`, `italic`, `underlined`, `strikethrough`.

## Network sync

Chat forwarding, remote clears, mute state, and filter alerts for
network channels all travel over a single `ThunderChat` plugin-message
subchannel riding on `BungeeCord` (Velocity's legacy forwarding is
compatible with this). `network.enabled` and `network.server-name`
in `config.yml` control whether forwarding happens and how this server
identifies itself in formats. One real limitation of Bungee-style
plugin messaging: sending *requires* at least one online player to
carry the packet, so a mute/clear/chat action taken while a server has
zero players online won't propagate to the rest of the network until
someone reconnects.

## Persistence

With `storage.type: yaml` (the only implemented mode), these live under
the plugin's data folder and reload independently of `config.yml`:

| File | Holds |
|---|---|
| `channel-state.yml` | Each player's active channel + hidden channels |
| `mutes.yml` | Global and per-player mutes, every channel including local |
| `ignores.yml` | Who's ignoring whom |
| `spy.yml` | Each player's spy on/off state per section |
| `chat-colors.yml` | Each player's chosen color/gradient/styles/custom format |

`/thunderchat reload` reloads `config.yml` and re-reads `mutes.yml`;
the other four files are only read at startup and written on change.

## Building from source

Requires Java 21.

```bash
./gradlew build
```

The built jar is output to `build/libs/`.

## Credits

The [Interactive chat](#interactive-chat) placeholders are a small,
original-code reimplementation inspired by
[InteractiveChat](https://github.com/Loohp/InteractiveChat) by
[Loohp](https://github.com/Loohp) — the plugin that popularized
clickable/hoverable item, inventory, and location placeholders in chat.
Current placeholder coverage in ThunderChat (`[item]`/`[i]`, `[inv]`) is
intentionally a small subset of what InteractiveChat offers.

ThunderChat's own interactive-chat code will start drawing more directly
on InteractiveChat's source as the placeholder set grows, so expect this
section to expand alongside it. All credit for the original concept and
implementation approach goes to Loohp and the InteractiveChat
contributors; go check out the real thing at the link above.
