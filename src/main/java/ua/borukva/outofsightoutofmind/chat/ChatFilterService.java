/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ua.borukva.outofsightoutofmind.chat;

import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import ua.borukva.outofsightoutofmind.config.IgnoreConfig;

public class ChatFilterService {

  public static boolean shouldFilter(UUID senderUuid, UUID recipientUuid) {
    return IgnoreConfig.getInstance().isIgnoring(recipientUuid, senderUuid);
  }

  public static Predicate<ServerPlayer> createIgnoreFilter(UUID senderUuid) {
    return recipient -> !shouldFilter(senderUuid, recipient.getUUID());
  }
}
