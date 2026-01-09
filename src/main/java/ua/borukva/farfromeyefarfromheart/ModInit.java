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
