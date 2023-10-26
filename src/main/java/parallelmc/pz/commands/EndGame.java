package parallelmc.pz.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;
import parallelmc.pz.GameEndReason;
import parallelmc.pz.GameState;
import parallelmc.pz.ParallelZombies;

import static parallelmc.pz.utils.ZombieUtils.createMessage;

public class EndGame implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args) {
        if (commandSender.isOp() || commandSender instanceof ConsoleCommandSender) {
            if (ParallelZombies.gameManager.gameState == GameState.PLAY) {
                ParallelZombies.gameManager.endGame(GameEndReason.COMMAND);
                commandSender.sendMessage(createMessage("Ending game..."));
            }
            else {
                commandSender.sendMessage(createMessage("Cannot end the game right now."));
            }
        }
        else {
            commandSender.sendMessage(createMessage("You do not have access to this command."));
        }
        return true;
    }
}
