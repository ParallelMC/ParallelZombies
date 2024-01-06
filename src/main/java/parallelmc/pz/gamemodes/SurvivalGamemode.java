package parallelmc.pz.gamemodes;

import org.bukkit.plugin.Plugin;
import parallelmc.pz.GameState;
import parallelmc.pz.ParallelZombies;

import java.util.logging.Level;

public class SurvivalGamemode extends ZombiesGamemode {
    private int secondsLeft;

    public SurvivalGamemode(Plugin plugin) {
        super(plugin, "Survival", "Survive until the timer ends!", "Eliminate the survivors before the timer ends!\nSurvivor kills add 1 minute to the timer!");
        secondsLeft = 300;
    }

    public void addTime(int seconds) {
        secondsLeft += seconds;
    }

    @Override
    public void doGame() {
        this.plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (ParallelZombies.gameManager.gameState != GameState.PLAY) {
                ParallelZombies.log(Level.SEVERE, "Game loop running during " + ParallelZombies.gameManager.gameState + ". This shouldn't be happening!");
                return;
            }
            secondsLeft--;
            ParallelZombies.gameManager.runForEachPlayer(z -> {
                int minutes = secondsLeft / 60;
                int seconds = secondsLeft % 60;
                z.updateBoard("",
                        "§aSurvivors",
                        "§e" + ParallelZombies.gameManager.getSurvivorsLeft(),
                        "",
                        "§cZombies",
                        "§e" + ParallelZombies.gameManager.getZombiesLeft(),
                        "",
                        "§6Time Left",
                        "§e" + minutes + ":" + (seconds < 10 ? "0" + seconds : seconds));
                z.getMcPlayer().setFoodLevel(23);
            });

        }, 0L, 20L);
    }

    @Override
    public boolean winCondition() {
        return secondsLeft <= 0 || ParallelZombies.gameManager.getSurvivorsLeft() == 0;
    }

    @Override
    public void endGame() {
        if (ParallelZombies.gameManager.getSurvivorsLeft() == 0) {
            ParallelZombies.sendMessage("The Zombies win!");
        }
        else {
            ParallelZombies.sendMessage("The remaining Survivors win!");
        }
    }
}
