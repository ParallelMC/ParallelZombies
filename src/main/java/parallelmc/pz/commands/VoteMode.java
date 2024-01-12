package parallelmc.pz.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import parallelmc.pz.GameState;
import parallelmc.pz.ParallelZombies;

import java.util.List;

public class VoteMode implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args) {
        if (commandSender instanceof Player player) {
            if (args.length != 1) {
                return false;
            }
            if (ParallelZombies.gameManager.gameState == GameState.PREGAME) {
                if (ParallelZombies.gameManager.hasGamemodeBeenForced()) {
                    ParallelZombies.sendMessageTo(player, "The gamemode has been forced by an admin. You cannot vote!");
                    return true;
                }
                if (ParallelZombies.gameManager.hasVotedForGamemode(player)) {
                    ParallelZombies.sendMessageTo(player, "You have already voted for a gamemode!");
                    return true;
                }
                ParallelZombies.gameManager.addVoteGamemode(player, args[0]);
            }
            else {
                ParallelZombies.sendMessageTo(player, "The game has already started!");
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (command.getName().strip().equalsIgnoreCase("votemode")) {
            return ParallelZombies.gameManager.getGameModes();
        }
        return null;
    }
}