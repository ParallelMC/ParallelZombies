package parallelmc.pz.gamemodes;

import org.bukkit.plugin.Plugin;
import parallelmc.pz.GameState;
import parallelmc.pz.ParallelZombies;
import parallelmc.pz.Team;
import parallelmc.pz.ZombiesPlayer;

import java.util.logging.Level;

public class LastSurvivorGamemode extends ZombiesGamemode {

    public LastSurvivorGamemode(Plugin plugin) {
        super(plugin,"Last Survivor Standing", "Be the last survivor standing!", "Eliminate the survivors!");
    }

    @Override
    public void doGame() {
        this.plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (ParallelZombies.gameManager.gameState != GameState.PLAY) {
                ParallelZombies.log(Level.SEVERE, "Game loop running during " + ParallelZombies.gameManager.gameState + ". This shouldn't be happening!");
                return;
            }
            ParallelZombies.gameManager.runForEachPlayer(z -> {
                z.updateBoard( "",
                        "§aSurvivors",
                        "§e" + ParallelZombies.gameManager.getSurvivorsLeft(),
                        "",
                        "§cZombies",
                        "§e" + ParallelZombies.gameManager.getZombiesLeft());
                z.getMcPlayer().setFoodLevel(23);
            });

        }, 0L, 20L);
    }

    @Override
    public boolean winCondition() {
        return ParallelZombies.gameManager.getSurvivorsLeft() == 1;
    }

    @Override
    public void endGame() {
        ZombiesPlayer winner = ParallelZombies.gameManager.filterPlayers(x -> x.getTeam() == Team.SURVIVOR).findFirst().orElse(null);
        if (winner == null) {
            ParallelZombies.log(Level.SEVERE, "Failed to retrieve the winning player!");
            return;
        }
        ParallelZombies.sendMessage(winner.getMcPlayer().getName() + " is the winner!");
    }
}
