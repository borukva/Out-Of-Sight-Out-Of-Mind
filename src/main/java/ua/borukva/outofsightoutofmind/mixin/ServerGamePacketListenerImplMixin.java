/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ua.borukva.outofsightoutofmind.mixin;

import java.util.UUID;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ua.borukva.outofsightoutofmind.chat.ChatFilterService;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
  @Shadow public ServerPlayer player;

  @Inject(
      method =
          "sendPlayerChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/network/chat/ChatType$Bound;)V",
      at = @At("HEAD"),
      cancellable = true)
  private void outofsightoutofmind$filterIgnoredMessages(
      PlayerChatMessage message, ChatType.Bound boundChatType, CallbackInfo ci) {
    UUID senderUuid = message.link().sender();

    if (ChatFilterService.shouldFilter(senderUuid, this.player.getUUID())) {
      ci.cancel();
    }
  }
}
