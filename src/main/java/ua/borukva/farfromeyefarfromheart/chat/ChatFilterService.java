package ua.borukva.farfromeyefarfromheart.chat;

import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import ua.borukva.farfromeyefarfromheart.config.IgnoreConfig;

public class ChatFilterService {

  public static boolean shouldFilter(UUID senderUuid, UUID recipientUuid) {
    return IgnoreConfig.getInstance().isIgnoring(recipientUuid, senderUuid);
  }

  public static Predicate<ServerPlayer> createIgnoreFilter(UUID senderUuid) {
    return recipient -> !shouldFilter(senderUuid, recipient.getUUID());
  }
}
