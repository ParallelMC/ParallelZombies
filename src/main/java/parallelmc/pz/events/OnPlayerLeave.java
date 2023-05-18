package parallelmc.pz.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import parallelmc.pz.ParallelZombies;

public class OnPlayerLeave implements Listener {
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        ParallelZombies.gameManager.removePlayer(event.getPlayer());
    }
}