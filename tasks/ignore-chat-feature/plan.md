# Implementation Plan: Server-Side Chat Ignore Feature

## Overview

This plan details the implementation of a server-side chat ignore system for a Minecraft Fabric mod. Players can maintain personal ignore lists, and the server filters chat messages on a per-recipient basis before broadcasting.

## File Structure

### New Files to Create

```
src/main/java/ua/borukva/farfromeyefarfromheart/
├── config/
│   └── IgnoreConfig.java          # Thread-safe config management
├── command/
│   └── IgnoreCommand.java         # Brigadier command registration
├── chat/
│   └── ChatFilterService.java     # Ignore list lookup service
└── mixin/
    └── PlayerManagerMixin.java    # Chat broadcast filtering mixin
```

### Files to Modify

```
src/main/java/ua/borukva/farfromeyefarfromheart/
└── ModInit.java                   # Initialize config and commands

src/main/resources/
└── farfromeyefarfromheart.mixins.json  # Register new mixin
```

### Config File (Runtime Generated)

```
config/
└── farfromeyefarfromheart.json   # Persistent ignore lists
```

---

## Step-by-Step Implementation

### Step 1: Create IgnoreConfig Class

**File:** `src/main/java/ua/borukva/farfromeyefarfromheart/config/IgnoreConfig.java`

**Purpose:** Thread-safe management of player ignore lists with JSON persistence.

**Implementation Details:**

```java
package ua.borukva.farfromeyefarfromheart.config;

// Key components:
// - ConcurrentHashMap<UUID, Set<UUID>> for thread-safe in-memory storage
// - Gson for JSON serialization/deserialization
// - Async file writes to avoid blocking game thread
// - Singleton pattern for global access
```

**Data Structure:**
```java
// In-memory representation
private final ConcurrentHashMap<UUID, Set<UUID>> ignoreLists = new ConcurrentHashMap<>();

// JSON format on disk:
// {
//   "550e8400-e29b-41d4-a716-446655440000": [
//     "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
//     "6ba7b811-9dad-11d1-80b4-00c04fd430c8"
//   ]
// }
```

**Methods:**
1. `load()` - Load config from JSON file on server start
2. `save()` - Async save to JSON file
3. `addIgnore(UUID player, UUID target)` - Add to ignore list, returns success
4. `removeIgnore(UUID player, UUID target)` - Remove from ignore list, returns success
5. `getIgnoredPlayers(UUID player)` - Get set of ignored UUIDs
6. `isIgnoring(UUID player, UUID target)` - Check if player ignores target

**Design Decisions:**
- **ConcurrentHashMap**: Server is multi-threaded; config may be accessed from command handlers and chat event handlers simultaneously
- **CopyOnWriteArraySet for inner sets**: Read-heavy workload (checking ignores on every chat), writes are rare
- **Async saves**: Use CompletableFuture to avoid blocking the main server thread on file I/O
- **UUID-based storage**: Handles player name changes correctly; names are resolved at command time

---

### Step 2: Create ChatFilterService Class

**File:** `src/main/java/ua/borukva/farfromeyefarfromheart/chat/ChatFilterService.java`

**Purpose:** Service layer for ignore list queries, used by the mixin.

**Implementation Details:**

```java
package ua.borukva.farfromeyefarfromheart.chat;

// Provides simple interface for mixin to query ignore status
// Decouples mixin from config implementation
```

**Methods:**
1. `shouldFilter(UUID sender, UUID recipient)` - Returns true if recipient should NOT receive sender's message
2. `createFilterPredicate(UUID sender)` - Returns Predicate<ServerPlayerEntity> for broadcast filtering

**Design Decision:**
- **Separate service class**: Keeps mixin code minimal and testable
- **Predicate factory**: Matches the signature needed by PlayerManager.broadcast()

---

### Step 3: Create PlayerManagerMixin

**File:** `src/main/java/ua/borukva/farfromeyefarfromheart/mixin/PlayerManagerMixin.java`

**Purpose:** Intercept chat broadcast to filter messages per-recipient.

**Target Method:**
```java
// In net.minecraft.server.players.PlayerList (mapped name: PlayerManager in some mappings)
public void broadcast(
    SignedMessage message,
    Predicate<ServerPlayerEntity> shouldSendFiltered,
    @Nullable ServerPlayerEntity sender,
    MessageType.Parameters params
)
```

**Mixin Strategy:**

```java
@Mixin(PlayerList.class)  // or PlayerManager depending on mappings
public abstract class PlayerManagerMixin {

    @ModifyVariable(
        method = "broadcast(Lnet/minecraft/network/chat/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/MessageType$Parameters;)V",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private Predicate<ServerPlayer> wrapFilterPredicate(
        Predicate<ServerPlayer> original,
        SignedMessage message,
        Predicate<ServerPlayer> shouldSendFiltered,
        ServerPlayer sender,
        MessageType.Parameters params
    ) {
        if (sender == null) {
            // System message, don't filter
            return original;
        }

        UUID senderUUID = sender.getUUID();

        // Combine original predicate with our ignore filter
        return original.and(recipient ->
            !ChatFilterService.shouldFilter(senderUUID, recipient.getUUID())
        );
    }
}
```

**Design Decisions:**

1. **@ModifyVariable over @Redirect**:
   - More compatible with other mods (including StyledChat)
   - Doesn't replace the entire method, just wraps the predicate
   - StyledChat can still modify other aspects of the broadcast

2. **Predicate composition with `.and()`**:
   - Preserves StyledChat's filtering logic (if any)
   - Our filter is additive: "original conditions AND not ignored"
   - StyledChat may set custom predicates; we don't override them

3. **Null sender check**:
   - System messages (server announcements) have null sender
   - These should never be filtered by ignore lists

4. **Why this approach works with StyledChat**:
   - StyledChat formats the message content, we filter recipients
   - These are orthogonal concerns
   - StyledChat doesn't modify the predicate, it modifies the SignedMessage
   - Our mixin fires at HEAD, before any processing

---

### Step 4: Create IgnoreCommand Class

**File:** `src/main/java/ua/borukva/farfromeyefarfromheart/command/IgnoreCommand.java`

**Purpose:** Register and handle `/ignore` commands.

**Command Structure:**
```
/ignore add <player>    - Add player to ignore list
/ignore remove <player> - Remove player from ignore list
/ignore list           - List all ignored players
```

**Implementation Details:**

```java
package ua.borukva.farfromeyefarfromheart.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class IgnoreCommand {

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      Commands.literal("ignore")
        .then(Commands.literal("add")
          .then(Commands.argument("player", EntityArgument.player())
            .executes(IgnoreCommand::addIgnore)))
        .then(Commands.literal("remove")
          .then(Commands.argument("player", EntityArgument.player())
            .executes(IgnoreCommand::removeIgnore)))
        .then(Commands.literal("list")
          .executes(IgnoreCommand::listIgnored))
    );
  }

  private static int addIgnore(CommandContext<CommandSourceStack> context) {
    // Implementation
  }

  private static int removeIgnore(CommandContext<CommandSourceStack> context) {
    // Implementation
  }

  private static int listIgnored(CommandContext<CommandSourceStack> context) {
    // Implementation
  }
}
```

**Player Name Resolution:**
- `EntityArgument.player()` provides online player resolution
- Returns `ServerPlayer` which has `getUUID()` method
- Only works for online players (acceptable limitation)

**Feedback Messages:**
```
/ignore add <player>:
  Success: "Now ignoring <player>"
  Already ignored: "<player> is already on your ignore list"
  Self: "You cannot ignore yourself"

/ignore remove <player>:
  Success: "No longer ignoring <player>"
  Not ignored: "<player> is not on your ignore list"

/ignore list:
  Has ignored: "Ignored players: <player1>, <player2>, ..."
  Empty: "Your ignore list is empty"
```

**Design Decisions:**
- **EntityArgument.player()**: Built-in argument type with tab completion
- **Online players only**: Simplifies implementation; offline player lookup adds complexity
- **No permission requirements**: Personal preference feature, not moderation

---

### Step 5: Update ModInit

**File:** `src/main/java/ua/borukva/farfromeyefarfromheart/ModInit.java`

**Changes:**

```java
package ua.borukva.farfromeyefarfromheart;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.borukva.farfromeyefarfromheart.command.ModCommands;
import ua.borukva.farfromeyefarfromheart.config.IgnoreConfig;

public class ModInit implements ModInitializer {
  public static final String MOD_ID = "farfromeyefarfromheart";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    LOGGER.info("Far From Eye Far From Heart initializing...");

    // Load config on server start
    ServerLifecycleEvents.SERVER_STARTING.register(server -> {
      IgnoreConfig.getInstance().load();
    });

    // Save config on server stop
    ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
      IgnoreConfig.getInstance().saveSync();
    });

    // Register commands
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      IgnoreCommand.register(dispatcher);
    });

    LOGGER.info("Far From Eye Far From Heart initialized!");
  }
}
```

---

### Step 6: Update Mixin Configuration

**File:** `src/main/resources/farfromeyefarfromheart.mixins.json`

**Changes:**

```json
{
  "required": true,
  "package": "ua.borukva.farfromeyefarfromheart.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": [
    "ExampleMixin"
  ],
  "server": [
    "ServerMixin",
    "PlayerManagerMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  },
  "overwrites": {
    "requireAnnotations": true
  }
}
```

**Note:** PlayerManagerMixin is in `server` array because chat broadcast only happens on server.

---

## Code Architecture

### Class Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                           ModInit                                 │
│  - Initializes config, registers commands                        │
│  - Entry point for mod                                           │
└─────────────────────────┬────────────────────────────────────────┘
                          │ initializes
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                      IgnoreConfig                                │
│  - ConcurrentHashMap<UUID, Set<UUID>> ignoreLists               │
│  - load(), save(), saveSync()                                   │
│  - addIgnore(), removeIgnore(), isIgnoring()                    │
│  - getIgnoredPlayers()                                          │
│  - Singleton: getInstance()                                      │
└─────────────────────────┬───────────────────────────────────────┘
                          │ used by
          ┌───────────────┴────────────────┐
          ▼                                ▼
┌─────────────────────────┐   ┌─────────────────────────────────┐
│    IgnoreCommand        │   │      ChatFilterService          │
│  - register()           │   │  - shouldFilter(sender, recip)  │
│  - addIgnore()          │   │  - createFilterPredicate()      │
│  - removeIgnore()       │   └───────────────┬─────────────────┘
│  - listIgnored()        │                   │ used by
└─────────────────────────┘                   ▼
                              ┌───────────────────────────────────┐
                              │      PlayerManagerMixin           │
                              │  @ModifyVariable on broadcast()   │
                              │  - Wraps predicate with ignore    │
                              │    filter                         │
                              └───────────────────────────────────┘
```

### Data Flow

```
Player A sends chat message
         │
         ▼
┌─────────────────────────────────────┐
│  Minecraft processes chat message    │
│  (StyledChat formats it here)        │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│  PlayerManager.broadcast() called    │
│  with SignedMessage + Predicate      │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│  PlayerManagerMixin intercepts       │
│  @ModifyVariable wraps predicate     │
│                                      │
│  New predicate:                      │
│  original.and(recipient ->           │
│    !isIgnoring(recipient, sender))   │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│  For each online player:             │
│  - Original predicate evaluated      │
│  - Our ignore check evaluated        │
│  - Message sent only if both pass    │
└─────────────────────────────────────┘
```

---

## Config Schema

### JSON Format

**File:** `config/farfromeyefarfromheart.json`

```json
{
  "ignoreVersion": 1,
  "ignoreLists": {
    "550e8400-e29b-41d4-a716-446655440000": [
      "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "6ba7b811-9dad-11d1-80b4-00c04fd430c8"
    ],
    "f47ac10b-58cc-4372-a567-0e02b2c3d479": [
      "550e8400-e29b-41d4-a716-446655440000"
    ]
  }
}
```

**Fields:**
- `ignoreVersion`: Schema version for future migrations (integer)
- `ignoreLists`: Map of player UUID to list of ignored player UUIDs

**Type Adapter:**
```java
// Custom Gson TypeAdapter for UUID
// Serialize UUID as string: "550e8400-e29b-41d4-a716-446655440000"
// Deserialize string back to UUID
```

### File Location

```java
Path configPath = FabricLoader.getInstance()
    .getConfigDir()
    .resolve("farfromeyefarfromheart.json");

// Typical path on server:
// server_directory/config/farfromeyefarfromheart.json
```

---

## Error Handling

### Edge Cases

| Scenario | Handling |
|----------|----------|
| Config file doesn't exist | Create empty config, log info message |
| Config file is corrupted/invalid JSON | Log error, use empty config, don't overwrite |
| Player tries to ignore themselves | Reject with feedback: "You cannot ignore yourself" |
| Player tries to ignore already-ignored player | Feedback: "<player> is already on your ignore list" |
| Player tries to remove non-ignored player | Feedback: "<player> is not on your ignore list" |
| Target player is offline | EntityArgument.player() rejects automatically with built-in message |
| Command run from console | Reject: "This command can only be run by a player" |
| Concurrent modification | ConcurrentHashMap handles thread safety |
| File write fails | Log error, continue with in-memory state |
| Server crashes before save | Data since last auto-save lost (acceptable) |

### User Feedback Messages

```java
// Success messages (green/success color)
Component.literal("Now ignoring " + playerName).withStyle(ChatFormatting.GREEN)
Component.literal("No longer ignoring " + playerName).withStyle(ChatFormatting.GREEN)

// Info messages (yellow/warning color)
Component.literal(playerName + " is already on your ignore list").withStyle(ChatFormatting.YELLOW)
Component.literal(playerName + " is not on your ignore list").withStyle(ChatFormatting.YELLOW)
Component.literal("Your ignore list is empty").withStyle(ChatFormatting.YELLOW)

// Error messages (red/error color)
Component.literal("You cannot ignore yourself").withStyle(ChatFormatting.RED)

// List message (white/default)
Component.literal("Ignored players: " + String.join(", ", names))
```

---

## Design Decisions Summary

### 1. Mixin vs Fabric Events

**Decision:** Use Mixin with @ModifyVariable

**Reasoning:**
- Fabric's `ServerMessageEvents.CHAT_MESSAGE` doesn't support per-recipient filtering
- The event fires once per message, not once per recipient
- We need to modify WHO receives the message, not IF the message is sent
- @ModifyVariable on the predicate parameter is surgical and compatible

### 2. StyledChat Compatibility

**Decision:** No direct StyledChat dependency needed

**Reasoning:**
- StyledChat modifies message CONTENT (formatting, placeholders)
- We modify message RECIPIENTS (filtering)
- These are orthogonal operations
- Our mixin fires on broadcast(), after StyledChat has formatted
- The predicate wrapping preserves any StyledChat predicate logic

### 3. Thread Safety Approach

**Decision:** ConcurrentHashMap + CopyOnWriteArraySet

**Reasoning:**
- Server processes commands and chat on different threads
- ConcurrentHashMap provides thread-safe key-value operations
- CopyOnWriteArraySet for inner sets (read-heavy, write-light workload)
- No need for explicit synchronized blocks

### 4. Async Config Saves

**Decision:** Save asynchronously with CompletableFuture

**Reasoning:**
- File I/O can block for milliseconds
- Chat handling should be as fast as possible
- Config changes (ignore/unignore) are rare
- Async saves keep the main thread responsive
- Synchronous save only on server shutdown

### 5. UUID-Based Storage

**Decision:** Store UUIDs, not player names

**Reasoning:**
- Player names can change
- UUID is permanent identifier
- Resolve name -> UUID at command time
- Display name fetched at list time (from online players or cache)

### 6. Online Players Only

**Decision:** Only allow ignoring currently online players

**Reasoning:**
- Simplifies implementation significantly
- EntityArgument.player() provides tab completion
- Offline player lookup requires usercache.json parsing
- Edge case: player can still be ignored while offline (UUID persists)
- Common use case is ignoring annoying player who is currently online

### 7. No Permission System

**Decision:** All players can use /ignore commands

**Reasoning:**
- This is a personal preference feature
- Similar to client-side mute in other games
- No moderation implications (doesn't silence the sender)
- Simplifies implementation (no permission checks)

---

## Testing Strategy

### Manual Testing Checklist

1. **Basic Commands**
   - [ ] `/ignore add <player>` adds player to list
   - [ ] `/ignore remove <player>` removes player from list
   - [ ] `/ignore list` shows correct players
   - [ ] Tab completion works for player names

2. **Chat Filtering**
   - [ ] Ignored player's messages don't appear
   - [ ] Non-ignored player's messages appear normally
   - [ ] System messages still appear
   - [ ] Unignoring restores message visibility

3. **Persistence**
   - [ ] Config saves to correct location
   - [ ] Config survives server restart
   - [ ] Config format is valid JSON

4. **Edge Cases**
   - [ ] Cannot ignore yourself
   - [ ] Cannot ignore offline players
   - [ ] Duplicate ignore handled gracefully
   - [ ] Remove non-ignored handled gracefully

5. **StyledChat Compatibility**
   - [ ] Install StyledChat alongside mod
   - [ ] Verify formatted messages are filtered correctly
   - [ ] Verify StyledChat features still work

### Integration Test (Manual)

```
1. Start server with mod + StyledChat
2. Join with Player A and Player B
3. Player A: /ignore add PlayerB
4. Player B: send chat message
5. Verify: Player A does NOT see message
6. Verify: Other players DO see message (with StyledChat formatting)
7. Player A: /ignore remove PlayerB
8. Player B: send chat message
9. Verify: Player A now sees message
10. Restart server
11. Verify: ignore list persisted
```

---

## Implementation Order

1. **IgnoreConfig.java** - Foundation for all other components
2. **ChatFilterService.java** - Service layer before mixin
3. **PlayerManagerMixin.java** - Core filtering logic
4. **Update farfromeyefarfromheart.mixins.json** - Register mixin
5. **IgnoreCommand.java** - User-facing commands
6. **Update ModInit.java** - Wire everything together
7. **Testing** - Verify all functionality

---

## Estimated Lines of Code

| File | Estimated Lines |
|------|-----------------|
| IgnoreConfig.java | 120-150 |
| ChatFilterService.java | 30-40 |
| PlayerManagerMixin.java | 40-50 |
| IgnoreCommand.java | 100-120 |
| ModInit.java changes | 20-30 |
| **Total** | **310-390** |

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Mixin conflicts with other mods | Low | Medium | Using @ModifyVariable not @Redirect |
| StyledChat incompatibility | Low | High | Predicate wrapping preserves existing logic |
| Thread safety issues | Low | High | Using concurrent collections throughout |
| Config corruption | Low | Low | Graceful fallback to empty config |
| Performance impact | Very Low | Low | O(1) hashmap lookup per recipient |
| Mapping name changes in MC updates | Medium | Medium | Document exact method signatures |

---

## Future Enhancements (Out of Scope)

These are NOT part of current implementation but noted for future reference:

1. **Offline player ignore** - Parse usercache.json for name->UUID
2. **Mutual ignore notification** - "You are being ignored by <player>"
3. **Admin commands** - `/ignore admin list <player>` for moderation
4. **Import/export** - Allow players to backup their ignore list
5. **Whisper filtering** - Block DMs from ignored players
6. **Join/leave filtering** - Hide join/leave messages from ignored players
