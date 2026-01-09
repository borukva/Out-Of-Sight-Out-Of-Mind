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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ua.borukva.outofsightoutofmind.config.IgnoreConfig;

public class ModCommands {

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
            .then(Commands.literal("list").executes(ModCommands::listIgnored)));
  }

  private static int addIgnore(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ServerPlayer executor = context.getSource().getPlayerOrException();
    ServerPlayer target = EntityArgument.getPlayer(context, "player");

    if (executor.getUUID().equals(target.getUUID())) {
      context
          .getSource()
          .sendFailure(
              Component.literal("You cannot ignore yourself").withStyle(ChatFormatting.RED));
      return 0;
    }

    boolean added = IgnoreConfig.getInstance().addIgnore(executor.getUUID(), target.getUUID());

    if (added) {
      context
          .getSource()
          .sendSuccess(
              () ->
                  Component.literal("Now ignoring " + target.getName().getString())
                      .withStyle(ChatFormatting.GREEN),
              false);
      return 1;
    } else {
      context
          .getSource()
          .sendFailure(
              Component.literal(target.getName().getString() + " is already on your ignore list")
                  .withStyle(ChatFormatting.YELLOW));
      return 0;
    }
  }

  private static int removeIgnore(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ServerPlayer executor = context.getSource().getPlayerOrException();
    ServerPlayer target = EntityArgument.getPlayer(context, "player");

    boolean removed = IgnoreConfig.getInstance().removeIgnore(executor.getUUID(), target.getUUID());

    if (removed) {
      context
          .getSource()
          .sendSuccess(
              () ->
                  Component.literal("No longer ignoring " + target.getName().getString())
                      .withStyle(ChatFormatting.GREEN),
              false);
      return 1;
    } else {
      context
          .getSource()
          .sendFailure(
              Component.literal(target.getName().getString() + " is not on your ignore list")
                  .withStyle(ChatFormatting.YELLOW));
      return 0;
    }
  }

  private static int listIgnored(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ServerPlayer executor = context.getSource().getPlayerOrException();
    Set<UUID> ignoredPlayers = IgnoreConfig.getInstance().getIgnoredPlayers(executor.getUUID());

    if (ignoredPlayers.isEmpty()) {
      context
          .getSource()
          .sendSuccess(
              () -> Component.literal("Your ignore list is empty").withStyle(ChatFormatting.YELLOW),
              false);
      return 0;
    }

    StringBuilder names = new StringBuilder();
    for (UUID ignoredUuid : ignoredPlayers) {
      ServerPlayer ignoredPlayer =
          context.getSource().getServer().getPlayerList().getPlayer(ignoredUuid);
      if (!names.isEmpty()) {
        names.append(", ");
      }
      if (ignoredPlayer != null) {
        names.append(ignoredPlayer.getName().getString());
      } else {
        names.append(ignoredUuid.toString());
      }
    }

    String finalNames = names.toString();
    context
        .getSource()
        .sendSuccess(() -> Component.literal("Ignored players: " + finalNames), false);
    return ignoredPlayers.size();
  }
}
