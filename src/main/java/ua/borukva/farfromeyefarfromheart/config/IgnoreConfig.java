package ua.borukva.farfromeyefarfromheart.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import net.fabricmc.loader.api.FabricLoader;
import ua.borukva.farfromeyefarfromheart.ModInit;

public class IgnoreConfig {
  private static final IgnoreConfig INSTANCE = new IgnoreConfig();
  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  private static final Type CONFIG_TYPE = new TypeToken<Map<String, Set<String>>>() {}.getType();

  private final Path configPath;
  private final ConcurrentHashMap<UUID, Set<UUID>> ignoreLists = new ConcurrentHashMap<>();

  private IgnoreConfig() {
    this.configPath =
        FabricLoader.getInstance().getConfigDir().resolve("farfromeyefarfromheart.json");
  }

  public static IgnoreConfig getInstance() {
    return INSTANCE;
  }

  public void load() {
    if (!Files.exists(configPath)) {
      ModInit.LOGGER.info("No ignore config found, starting with empty lists");
      return;
    }

    try (Reader reader = Files.newBufferedReader(configPath)) {
      Map<String, Set<String>> rawData = GSON.fromJson(reader, CONFIG_TYPE);
      if (rawData != null) {
        ignoreLists.clear();
        for (Map.Entry<String, Set<String>> entry : rawData.entrySet()) {
          UUID playerUuid = UUID.fromString(entry.getKey());
          Set<UUID> ignoredSet = new CopyOnWriteArraySet<>();
          for (String ignoredUuidStr : entry.getValue()) {
            ignoredSet.add(UUID.fromString(ignoredUuidStr));
          }
          ignoreLists.put(playerUuid, ignoredSet);
        }
        ModInit.LOGGER.info("Loaded ignore config with {} player entries", ignoreLists.size());
      }
    } catch (IOException e) {
      ModInit.LOGGER.error("Failed to load ignore config", e);
    } catch (Exception e) {
      ModInit.LOGGER.error("Invalid ignore config format, starting with empty lists", e);
    }
  }

  public void save() {
    CompletableFuture.runAsync(this::saveSync);
  }

  public void saveSync() {
    try {
      Files.createDirectories(configPath.getParent());
      Map<String, Set<String>> rawData = new HashMap<>();
      for (Map.Entry<UUID, Set<UUID>> entry : ignoreLists.entrySet()) {
        Set<String> ignoredStrings = new HashSet<>();
        for (UUID ignoredUuid : entry.getValue()) {
          ignoredStrings.add(ignoredUuid.toString());
        }
        rawData.put(entry.getKey().toString(), ignoredStrings);
      }

      try (Writer writer = Files.newBufferedWriter(configPath)) {
        GSON.toJson(rawData, writer);
      }
      ModInit.LOGGER.debug("Saved ignore config");
    } catch (IOException e) {
      ModInit.LOGGER.error("Failed to save ignore config", e);
    }
  }

  public boolean addIgnore(UUID player, UUID target) {
    Set<UUID> playerIgnores = ignoreLists.computeIfAbsent(player, k -> new CopyOnWriteArraySet<>());
    boolean added = playerIgnores.add(target);
    if (added) {
      save();
    }
    return added;
  }

  public boolean removeIgnore(UUID player, UUID target) {
    Set<UUID> playerIgnores = ignoreLists.get(player);
    if (playerIgnores == null) {
      return false;
    }
    boolean removed = playerIgnores.remove(target);
    if (removed) {
      ignoreLists.computeIfPresent(
          player,
          (k, v) -> {
            if (v.isEmpty()) {
              return null;
            }
            return v;
          });
      save();
    }
    return removed;
  }

  public boolean isIgnoring(UUID player, UUID target) {
    Set<UUID> playerIgnores = ignoreLists.get(player);
    return playerIgnores != null && playerIgnores.contains(target);
  }

  public Set<UUID> getIgnoredPlayers(UUID player) {
    Set<UUID> playerIgnores = ignoreLists.get(player);
    if (playerIgnores == null) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(playerIgnores);
  }
}
