package parallelmc.pz.commands;

import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import parallelmc.pz.GameState;
import parallelmc.pz.ParallelZombies;

import java.util.List;

import static parallelmc.pz.utils.ZombieUtils.createMessage;

public class SetMode implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args) {
        if (commandSender.isOp() || commandSender instanceof ConsoleCommandSender) {
            if (args.length != 1)
                return false;
            if (ParallelZombies.gameManager.gameState == GameState.PREGAME) {
                if (ParallelZombies.gameManager.forceGameMode(args[0])) {
                    commandSender.sendMessage(createMessage("Gamemode forced successfully."));
                }
                else {
                    commandSender.sendMessage(createMessage("Failed to change the gamemode, ensure that the specified gamemode exists."));
                }
            }
            else {
                commandSender.sendMessage(createMessage("Cannot change the gamemode right now."));
            }
        }
        else {
            commandSender.sendMessage(createMessage("You do not have access to this command."));
        }
        return true;
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (command.getName().strip().equalsIgnoreCase("setmode")) {
            return ParallelZombies.gameManager.getGameModes();
        }
        return null;
    }
}
