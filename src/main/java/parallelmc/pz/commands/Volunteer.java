package parallelmc.pz.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import parallelmc.pz.GameState;
import parallelmc.pz.ParallelZombies;

public class Volunteer implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args) {
        if (commandSender instanceof Player player) {
            if (ParallelZombies.gameManager.gameState != GameState.PREGAME) {
                ParallelZombies.sendMessageTo(player, "A game is already running, you cannot volunteer to be zombie!");
                return true;
            }
            else {
                if (ParallelZombies.gameManager.hasVolunteered(player)) {
                    ParallelZombies.sendMessageTo(player, "You are no longer volunteering to start as a zombie.");
                    ParallelZombies.gameManager.removeVolunteer(player);
                }
                else {
                    ParallelZombies.sendMessageTo(player, "You are now volunteering for a chance to start as a zombie.");
                    ParallelZombies.gameManager.addVolunteer(player);
                }
            }
        }
        return true;
    }
}
