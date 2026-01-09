/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package ua.borukva.outofsightoutofmind.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Set;
import java.util.UUID;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserNameToIdResolver;
import ua.borukva.outofsightoutofmind.ModInit;
import ua.borukva.outofsightoutofmind.config.IgnoreConfig;
import ua.borukva.outofsightoutofmind.config.MessagesConfig;

public class ModCommands {

  private static final String reloadPermission = ModInit.MOD_ID + ".ignore";

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("ignore")
            .then(
                Commands.literal("add")
                    .then(
                        Commands.argument("player", EntityArgument.player())
                            .executes(ModCommands::addIgnore)))
            .then(
                Commands.literal("remove")
                    .then(
                        Commands.argument("player", EntityArgument.player())
                            .executes(ModCommands::removeIgnore)))
            .then(Commands.literal("list").executes(ModCommands::listIgnored))
            .then(
                Commands.literal("reload")
                    .requires(
                        source ->
                            source.hasPermission(2) || Permissions.check(source, reloadPermission))
                    .executes(ModCommands::reloadConfig)));
  }

  private static int addIgnore(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ServerPlayer executor = context.getSource().getPlayerOrException();
    ServerPlayer target = EntityArgument.getPlayer(context, "player");
    MessagesConfig.Messages msg = MessagesConfig.getInstance().get();

    if (executor.getUUID().equals(target.getUUID())) {
      context
          .getSource()
          .sendFailure(Component.literal(msg.cannotIgnoreSelf).withStyle(ChatFormatting.RED));
      return 0;
    }

    boolean added = IgnoreConfig.getInstance().addIgnore(executor.getUUID(), target.getUUID());
    String targetName = target.getName().getString();

    if (added) {
      context
          .getSource()
          .sendSuccess(
              () ->
                  Component.literal(String.format(msg.nowIgnoring, targetName))
                      .withStyle(ChatFormatting.GREEN),
              false);
      return 1;
    } else {
      context
          .getSource()
          .sendFailure(
              Component.literal(String.format(msg.alreadyIgnoring, targetName))
                  .withStyle(ChatFormatting.YELLOW));
      return 0;
    }
  }

  private static int removeIgnore(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ServerPlayer executor = context.getSource().getPlayerOrException();
    ServerPlayer target = EntityArgument.getPlayer(context, "player");
    MessagesConfig.Messages msg = MessagesConfig.getInstance().get();

    boolean removed = IgnoreConfig.getInstance().removeIgnore(executor.getUUID(), target.getUUID());
    String targetName = target.getName().getString();

    if (removed) {
      context
          .getSource()
          .sendSuccess(
              () ->
                  Component.literal(String.format(msg.noLongerIgnoring, targetName))
                      .withStyle(ChatFormatting.GREEN),
              false);
      return 1;
    } else {
      context
          .getSource()
          .sendFailure(
              Component.literal(String.format(msg.notIgnoring, targetName))
                  .withStyle(ChatFormatting.YELLOW));
      return 0;
    }
  }

  private static int listIgnored(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ServerPlayer executor = context.getSource().getPlayerOrException();
    Set<UUID> ignoredPlayers = IgnoreConfig.getInstance().getIgnoredPlayers(executor.getUUID());
    MessagesConfig.Messages msg = MessagesConfig.getInstance().get();

    if (ignoredPlayers.isEmpty()) {
      context
          .getSource()
          .sendSuccess(
              () -> Component.literal(msg.ignoreListEmpty).withStyle(ChatFormatting.YELLOW), false);
      return 0;
    }

    StringBuilder names = new StringBuilder();
    UserNameToIdResolver profileCache = context.getSource().getServer().services().nameToIdCache();

    for (UUID ignoredUuid : ignoredPlayers) {
      if (!names.isEmpty()) {
        names.append(", ");
      }

      ServerPlayer onlinePlayer =
          context.getSource().getServer().getPlayerList().getPlayer(ignoredUuid);
      if (onlinePlayer != null) {
        names.append(onlinePlayer.getName().getString());
      } else if (profileCache.get(ignoredUuid).isPresent()) {
        names.append(
            profileCache.get(ignoredUuid).map(NameAndId::name).orElse(ignoredUuid.toString()));
      } else {
        names.append(ignoredUuid.toString());
      }
    }

    String finalNames = names.toString();
    context
        .getSource()
        .sendSuccess(() -> Component.literal(String.format(msg.ignoredPlayers, finalNames)), false);
    return ignoredPlayers.size();
  }

  private static int reloadConfig(CommandContext<CommandSourceStack> context) {
    MessagesConfig.getInstance().load();
    IgnoreConfig.getInstance().load();
    MessagesConfig.Messages msg = MessagesConfig.getInstance().get();

    context
        .getSource()
        .sendSuccess(
            () -> Component.literal(msg.configReloaded).withStyle(ChatFormatting.GREEN), false);
    return 1;
  }
}
