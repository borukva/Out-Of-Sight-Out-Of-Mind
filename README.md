# Out Of Sight, Out Of Mind

A server-side Minecraft Fabric mod that allows players to ignore other players' chat messages.

## Features

- `/ignore add <player>` - Add a player to your ignore list
- `/ignore remove <player>` - Remove a player from your ignore list
- `/ignore list` - View all players you're currently ignoring
- `/ignore reload` - Reload configuration files (operator or 'outofsightoutofmind.reload' permission)

Messages from ignored players are filtered server-side before being sent to you, so they won't appear in your chat at all.

## Compatibility

This mod is compatible with:
- **Vanilla Minecraft** - Works out of the box
- **[StyledChat](https://modrinth.com/mod/styled-chat)** - Fully compatible, messages are filtered at the network level

## How It Works

The mod intercepts chat messages at the network handler level (`ServerGamePacketListenerImpl.sendPlayerChatMessage`) before they are sent to each player. If the recipient has the sender on their ignore list, the message is cancelled for that specific player.

Ignore lists are stored persistently in `config/outofsightoutofmind/ignores.json`. Saving occurs during server shutdown.

All user-facing messages can be customized in `config/outofsightoutofmind/messages.json` for localization or personalization.
