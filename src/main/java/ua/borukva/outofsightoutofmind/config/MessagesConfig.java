/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ua.borukva.outofsightoutofmind.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import ua.borukva.outofsightoutofmind.ModInit;

public class MessagesConfig {
  private static final MessagesConfig INSTANCE = new MessagesConfig();
  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  private final Path configPath;
  private Messages messages = new Messages();

  private MessagesConfig() {
    this.configPath =
        FabricLoader.getInstance().getConfigDir().resolve(ModInit.MOD_ID).resolve("messages.json");
  }

  public static MessagesConfig getInstance() {
    return INSTANCE;
  }

  public void load() {
    if (!Files.exists(configPath)) {
      ModInit.LOGGER.info("No messages config found, creating default");
      save();
      return;
    }

    try (Reader reader = Files.newBufferedReader(configPath)) {
      Messages loaded = GSON.fromJson(reader, Messages.class);
      if (loaded != null) {
        this.messages = loaded;
        ModInit.LOGGER.info("Loaded messages config");
      }
    } catch (IOException e) {
      ModInit.LOGGER.error("Failed to load messages config", e);
    } catch (Exception e) {
      ModInit.LOGGER.error("Invalid messages config format, using defaults", e);
    }
  }

  public void save() {
    try {
      Files.createDirectories(configPath.getParent());
      try (Writer writer = Files.newBufferedWriter(configPath)) {
        GSON.toJson(messages, writer);
      }
      ModInit.LOGGER.debug("Saved messages config");
    } catch (IOException e) {
      ModInit.LOGGER.error("Failed to save messages config", e);
    }
  }

  public Messages get() {
    return messages;
  }

  public static class Messages {
    public String cannotIgnoreSelf = "You cannot ignore yourself";
    public String nowIgnoring = "Now ignoring %s";
    public String alreadyIgnoring = "%s is already on your ignore list";
    public String noLongerIgnoring = "No longer ignoring %s";
    public String notIgnoring = "%s is not on your ignore list";
    public String ignoreListEmpty = "Your ignore list is empty";
    public String ignoredPlayers = "Ignored players: %s";
    public String configReloaded = "Configuration reloaded";
  }
}
