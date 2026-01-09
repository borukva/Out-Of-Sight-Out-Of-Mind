package ua.borukva.outofsightoutofmind;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.borukva.outofsightoutofmind.command.ModCommands;
import ua.borukva.outofsightoutofmind.config.IgnoreConfig;

public class ModInit implements ModInitializer {
  public static final String MOD_ID = "outofsightoutofmind";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    LOGGER.info("Far From Eye Far From Heart initializing...");

    ServerLifecycleEvents.SERVER_STARTING.register(
        server -> {
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

    LOGGER.info("Far From Eye Far From Heart initialized!");
  }
}
