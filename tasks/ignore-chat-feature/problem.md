# Problem Statement

## Overview

Build a **server-side** Minecraft Fabric mod that allows players to ignore other players' chat messages. The mod operates on the server, filtering chat messages on a per-recipient basis before broadcasting them. The mod must work seamlessly with StyledChat (a server-side chat formatting mod by Patbox) and store ignore lists persistently in JSON format.

## Requirements

### Core Functionality

1. **Ignore System**: Players can maintain a personal list of ignored players whose chat messages will be hidden from their view
2. **Persistent Storage**: Ignore lists stored in JSON format at `config/farfromeyefarfromheart.json` in the server's config directory (not mod folder)
3. **Storage Format**: JSON structure as `"uuid_of_ignoring_person": [<list of uuid of ignored players>]`
4. **Server-Side Operation**: Mod operates on the server, intercepting chat message broadcast and filtering per-recipient

### Commands

Implement three server commands under the `/ignore` namespace:

1. `/ignore add <PLAYER_NAME>` - Add a player to the ignore list (no longer see their messages)
2. `/ignore remove <PLAYER_NAME>` - Remove a player from the ignore list
3. `/ignore list` - Display all currently ignored players

### Integration Requirements

4. **StyledChat Compatibility**: Must work with StyledChat mod (https://github.com/Patbox/StyledChat)
   - StyledChat is installed on the server (server-side)
   - Need to investigate whether direct intervention with StyledChat is required
   - Both mods run on the server - integration is MORE important than in client-side scenario
   - If direct StyledChat integration is needed, add it as a dependency

## Technical Considerations

### Architecture

1. **Chat Message Filtering (Server-Side)**:
   - Intercept chat message broadcast on the SERVER before messages are sent to individual players
   - Filter messages per-recipient based on each player's ignore list
   - Must work AFTER StyledChat has formatted messages (or integrate with StyledChat's event system)
   - Likely need to:
     - Hook into Fabric's server-side chat events (`ServerMessageEvents.CHAT_MESSAGE` or similar)
     - OR integrate with StyledChat's API/events if available
     - OR mixin into server chat broadcast logic (`PlayerManager.broadcast()` or chat packet sending)

2. **Configuration Management**:
   - JSON serialization/deserialization for ignore list
   - File I/O to server's config directory (not client!)
   - UUID-based storage (not player names, to handle name changes)
   - Thread-safe access (server is multi-threaded)

3. **Command Registration**:
   - Fabric API server command registration system (`CommandRegistrationCallback.EVENT`)
   - Register commands on the dedicated server environment
   - Player name to UUID resolution (server has access to all online players)
   - Feedback messages sent to command executor

4. **StyledChat Integration Investigation**:
   - Since both mods run server-side, need to determine:
     - Does StyledChat expose events we can listen to?
     - Do we need to hook into StyledChat's message formatting pipeline?
     - Or can we use Fabric's standard chat events?
   - Check StyledChat's GitHub for API/integration points
   - If StyledChat has custom chat message handling, may need to:
     - Add StyledChat as a dependency
     - Use their events/API instead of vanilla Fabric events

### Technical Stack

- Minecraft 1.21.10
- Java 21
- Fabric Loader 0.17.2
- Fabric API 0.138.3+1.21.10
- Polymer 0.14.3+1.21.10
- Gson (likely bundled with Minecraft) for JSON handling
- StyledChat (potentially as dependency - to be determined during investigation)

### Key Classes/APIs to Use

- `CommandRegistrationCallback.EVENT` for server command registration
- `ServerMessageEvents.CHAT_MESSAGE` or similar Fabric chat events
- StyledChat API/events (if available and needed)
- Mixin into server chat broadcasting if needed (e.g., `PlayerManager`, `ServerPlayNetworkHandler`)
- `FabricLoader.getInstance().getConfigDir()` for server config directory
- UUID handling for player identification
- `ServerPlayerEntity` for server-side player references
- JSON serialization (Gson)

## Ambiguities and Assumptions

### Ambiguities

1. **StyledChat Integration**: Unclear what integration approach is needed
   - **Investigation Required**:
     - Does StyledChat provide events or API for message filtering?
     - Where in the pipeline does StyledChat format messages?
     - Can we filter AFTER StyledChat or must we integrate WITH it?
   - **Assumption**: We will first try Fabric's standard server chat events. If that doesn't work with StyledChat, we'll investigate StyledChat's API and add it as a dependency.

2. **Message Filtering Point**: Where exactly to intercept messages?
   - **Assumption**: Hook into server's chat message event AFTER formatting but BEFORE sending to individual players. This allows per-recipient filtering.
   - **Alternative**: If StyledChat bypasses standard events, may need to mixin into StyledChat's code or use their events

3. **Player Name Resolution**: How to resolve player names to UUIDs?
   - **Assumption**: Use server's player list for online players. Server has authoritative player data.
   - **Edge Case**: If player is offline, reject the command or use server's cached player data (usercache.json)

4. **Config Location Specification**: "config (located inside minecraft folder, not mod folder)"
   - **Assumption**: Use standard Fabric config directory on the server
   - Path: `FabricLoader.getInstance().getConfigDir().resolve("farfromeyefarfromheart.json")`
   - On server, this is typically `server_directory/config/farfromeyefarfromheart.json`

5. **Message Filtering Scope**: What types of messages to filter?
   - **Assumption**: Filter chat messages from ignored players (regular chat). May also need to filter:
     - `/me` commands
     - Whispers/direct messages (if applicable)
   - **Exclude**: System messages (join/leave/death) unless they're specifically from the ignored player's actions

6. **Command Permissions**: Who can use these commands?
   - **Assumption**: Any player on the server can use these commands (personal preference, no special permissions needed)
   - Commands modify only the executing player's ignore list

7. **Thread Safety**: Server is multi-threaded
   - **Assumption**: Need thread-safe config access (synchronized or use concurrent collections)
   - Config writes should be async to avoid blocking chat events

### Defaults Chosen

- Store by UUID for robustness against name changes
- Use standard Fabric server config directory
- Start without StyledChat dependency, add if investigation reveals it's needed
- Commands available to all players (personal preference)
- Filter messages at broadcast time, per-recipient
- Thread-safe config management
- Async config persistence to avoid blocking

## Complexity Classification

**Classification: MEDIUM**

### Justification

**Why not Small:**
- Requires multiple distinct components: command system, config management, server-side chat filtering
- Server-side filtering is more complex than client-side (per-recipient logic, thread safety)
- Needs investigation and potential integration with StyledChat (third-party mod)
- Must handle server-specific concerns: thread safety, async operations, server player management
- UUID resolution on server side with proper player lookup
- Configuration persistence with thread-safe access
- Estimated 300-500 lines of code across 5-8 files

**Why not Large:**
- Relatively focused feature scope (just ignore functionality)
- No complex UI (text commands only)
- Well-defined requirements with clear boundaries
- Standard Fabric server patterns apply (commands, events, config)
- No database or complex data structures needed
- No networking protocol changes

**Why Server-Side is More Complex than Client-Side:**
- Must handle per-recipient filtering (each player has different ignore list)
- Thread safety requirements (server is multi-threaded)
- Integration with StyledChat is more critical (both server-side)
- Server performance considerations (filtering must be efficient for all players)

**Breakdown:**
- Config manager class (thread-safe): ~100 lines
- Command registration class: ~120 lines
- Chat message filter (server event handler or mixin): ~80 lines
- StyledChat integration layer (if needed): ~60 lines
- Main initialization: ~50 lines
- Utility classes: ~60 lines
- **Total estimate: 400-500 lines**

**Requires:**
- Architectural decisions (where to intercept chat, how to filter per-recipient)
- StyledChat integration investigation
- Thread-safe config implementation
- Multiple file changes across different concerns
- Planning phase to structure components properly
