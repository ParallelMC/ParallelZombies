package parallelmc.pz.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import parallelmc.pz.GameState;
import parallelmc.pz.ParallelZombies;
import parallelmc.pz.Team;
import parallelmc.pz.ZombiesPlayer;

public class OnDamage implements Listener {
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (ParallelZombies.gameManager.gameState != GameState.PLAY) {
                event.setCancelled(true);
                return;
            }

            ZombiesPlayer pl = ParallelZombies.gameManager.getPlayer(player);
            if (pl.getTeam() == Team.SPECTATOR) {
                event.setCancelled(true);
                return;
            }

            if (event.getCause() == EntityDamageEvent.DamageCause.FALL && pl.getTeam() == Team.ZOMBIE) {
                event.setCancelled(true);
                return;
            }

            if (player.getHealth() - event.getFinalDamage() <= 0D) {
                event.setCancelled(true);
                pl.handleDeath();
                ParallelZombies.sendMessageTo(player, "You died.");
            }
        }
    }
}
