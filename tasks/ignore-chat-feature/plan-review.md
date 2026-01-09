# Plan Review: Server-Side Chat Ignore Feature

**Reviewed:** 2026-01-09
**Plan Path:** `/mnt/c/Users/mryur/projects/outofsightoutofmind/tasks/ignore-chat-feature/plan.md`

---

## Overall Assessment: APPROVED

The plan is thorough, well-structured, and demonstrates deep understanding of both Minecraft's architecture and mixin best practices. The technical approach is sound and the implementation details are clear enough to follow directly.

---

## Strengths

### 1. Excellent Technical Approach
- Using `@ModifyVariable` instead of `@Redirect` is the correct choice for mod compatibility
- Predicate composition with `.and()` properly preserves existing filter logic from other mods
- Thread-safe design using `ConcurrentHashMap` and `CopyOnWriteArraySet` is appropriate

### 2. Comprehensive Coverage
- All three required commands are specified: `/ignore add`, `/ignore remove`, `/ignore list`
- JSON storage format matches the requirement exactly
- Per-recipient filtering is correctly achieved through the broadcast() mixin

### 3. Strong Documentation
- Clear class diagrams and data flow visualizations
- Thorough error handling table covering all edge cases
- Well-defined user feedback messages with appropriate color coding

### 4. Risk Mitigation
- Correctly identifies that StyledChat compatibility is achieved through orthogonal concerns
- Async saves prevent main thread blocking
- Graceful fallback for corrupted config files

### 5. Follows Existing Patterns
- Mixin registration matches current project structure (`server` array in mixins.json)
- Package structure aligns with existing code organization
- Uses Mojang mappings which match the project's `loom.officialMojangMappings()` configuration

---

## Issues and Concerns

### Issue 1: Mapping Names Need Verification (Medium Priority)

**Location:** Step 3, PlayerManagerMixin

The plan correctly uses Mojang mapping names (`PlayerList`, `ServerPlayer`) but the method signature for `broadcast()` should be verified against MC 1.21.10 specifically. Minecraft's chat system has changed significantly in recent versions.

**Concern:** The exact broadcast method signature may differ in 1.21.10. The plan shows:
```java
broadcast(SignedMessage, Predicate<ServerPlayer>, ServerPlayer, MessageType.Parameters)
```

**Recommendation:** Before implementation, verify the exact method signature in `PlayerList.class` by:
1. Running `./gradlew genSources` to decompile
2. Checking `net.minecraft.server.players.PlayerList` for the broadcast method

### Issue 2: ChatFilterService Initialization (Low Priority)

**Location:** Step 2, ChatFilterService

The plan shows `ChatFilterService.shouldFilter()` as a static method but doesn't clarify how it accesses `IgnoreConfig.getInstance()`.

**Recommendation:** Explicitly note that ChatFilterService is a stateless utility class that delegates to IgnoreConfig singleton.

### Issue 3: Config Directory Creation (Low Priority)

**Location:** Step 1, IgnoreConfig

The plan doesn't explicitly mention creating the config directory if it doesn't exist.

**Recommendation:** Add a note in `save()` method to ensure parent directories exist:
```java
Files.createDirectories(configPath.getParent());
```

### Issue 4: List Command Player Name Resolution (Low Priority)

**Location:** Step 4, IgnoreCommand

The plan mentions displaying player names for `/ignore list` but only stores UUIDs. For offline players, their names won't be available.

**Current behavior:** Would show UUIDs for offline players
**Recommendation:** Either:
- Accept showing UUIDs for offline players (simplest)
- Cache player names when adding to ignore list
- Note this limitation explicitly in the plan

---

## Recommendations

### 1. Add Gson Dependency Note
Gson is bundled with Minecraft, but it would be helpful to note this in the plan to avoid confusion about missing dependencies.

### 2. Consider Periodic Auto-Save
The plan saves on every ignore/unignore action (async) and on server shutdown (sync). Consider adding a periodic auto-save (e.g., every 5 minutes) as additional safety against crashes.

### 3. Add Logging
Consider adding debug-level logging for:
- When a message is filtered (helps troubleshooting)
- When config is loaded/saved successfully

### 4. Command Feedback Improvement
For `/ignore list`, consider including a count: "Ignored players (3): player1, player2, player3"

---

## Line-by-Line Feedback

### Line 125-128 (Mixin Target Comment)
```java
// In net.minecraft.server.players.PlayerList (mapped name: PlayerManager in some mappings)
```
The comment is helpful but could be clearer. Since the project uses Mojang mappings, it should simply be `PlayerList`. The "PlayerManager" name is from Yarn mappings which this project doesn't use.

### Line 141 (Method Signature)
```java
method = "broadcast(Lnet/minecraft/network/chat/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/MessageType$Parameters;)V"
```
This is a descriptor-style method reference. This approach is correct and unambiguous, but requires verification against the actual decompiled source for MC 1.21.10.

### Line 414-415 (Data Flow Comment)
```java
│    !isIgnoring(recipient, sender))   │
```
The comment shows `isIgnoring(recipient, sender)` but the actual logic should be checking if the **recipient** ignores the **sender** (i.e., `isIgnoring(recipientUUID, senderUUID)`). The comment appears to have the parameters in the correct order but it's worded slightly confusingly. The actual implementation in the code block above (line 162) shows the correct order.

### Line 333-340 (Mixin JSON)
The plan shows both `ExampleMixin` and `ServerMixin` plus the new `PlayerManagerMixin`. The existing project only has these two mixins, and it would be worth noting that `ExampleMixin` might be removed if it's just boilerplate.

---

## Verification Checklist for Implementation

Before implementing, verify:
- [ ] `PlayerList.broadcast()` method signature in MC 1.21.10 matches the plan
- [ ] Fabric API includes `CommandRegistrationCallback` (it does in 0.138.3)
- [ ] Fabric API includes `ServerLifecycleEvents` (it does in 0.138.3)
- [ ] No conflicts with existing ServerMixin or ExampleMixin

---

## Conclusion

This is a well-designed plan that correctly addresses all requirements. The technical approach using `@ModifyVariable` on the broadcast predicate is elegant and minimizes compatibility issues. The thread-safety considerations are appropriate.

The only action item before implementation is to verify the exact method signature for `PlayerList.broadcast()` in Minecraft 1.21.10 using `./gradlew genSources`.

**Verdict: APPROVED for implementation with the minor caveat of verifying the mixin target method signature.**
