/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ua.borukva.outofsightoutofmind;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.borukva.outofsightoutofmind.command.ModCommands;
import ua.borukva.outofsightoutofmind.config.IgnoreConfig;
import ua.borukva.outofsightoutofmind.config.MessagesConfig;

public class ModInit implements ModInitializer {
  public static final String MOD_ID = "outofsightoutofmind";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    LOGGER.info(MOD_ID + " initializing...");

    ServerLifecycleEvents.SERVER_STARTING.register(
        server -> {
          MessagesConfig.getInstance().load();
          IgnoreConfig.getInstance().load();
        });

    ServerLifecycleEvents.SERVER_STOPPING.register(
        server -> {
          IgnoreConfig.getInstance().saveSync();
        });

    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> {
          ModCommands.register(dispatcher);
        });

    LOGGER.info(MOD_ID + " initialized!");
  }
}
