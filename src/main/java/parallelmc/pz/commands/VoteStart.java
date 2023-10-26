package parallelmc.pz.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import parallelmc.pz.GameState;
import parallelmc.pz.ParallelZombies;

public class VoteStart implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args) {
        if (commandSender instanceof Player player) {
            if (ParallelZombies.gameManager.gameState == GameState.PREGAME) {
                if (ParallelZombies.gameManager.hasVotedToStart(player)) {
                    ParallelZombies.sendMessageTo(player, "You have already voted to start!");
                    return true;
                }
                if (ParallelZombies.gameManager.playerCount() < 3) {
                    ParallelZombies.sendMessageTo(player, "There must be at least 3 players to start the game!");
                    return true;
                }
                ParallelZombies.gameManager.addVoteStart(player);
                ParallelZombies.sendMessage(player.getName() + " has voted the start the game. (" +
                        ParallelZombies.gameManager.currentVotesToStart() + "/" + (ParallelZombies.gameManager.playerCount() - 1) + " votes needed)");

            }
            else {
                ParallelZombies.sendMessageTo(player, "The game has already started!");
            }
        }
        return true;
    }
}
