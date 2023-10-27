package parallelmc.pz.events;

import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import parallelmc.pz.GameState;
import parallelmc.pz.ParallelZombies;
import parallelmc.pz.Team;
import parallelmc.pz.ZombiesPlayer;

public class OnDamageEntity implements Listener {
    @EventHandler
    public void onDamageEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player victim) {
            if (ParallelZombies.gameManager.gameState != GameState.PLAY) {
                event.setCancelled(true);
                return;
            }
            ZombiesPlayer pla = ParallelZombies.gameManager.getPlayer(attacker);
            ZombiesPlayer plv = ParallelZombies.gameManager.getPlayer(victim);
            if (pla.getTeam() == Team.SPECTATOR || plv.getTeam() == Team.SPECTATOR) {
                event.setCancelled(true);
                return;
            }

            if (pla.getTeam() == plv.getTeam()) {
                event.setCancelled(true);
                return;
            }

            if (victim.getHealth() - event.getFinalDamage() <= 0D) {
                event.setCancelled(true);
                plv.handleDeath();
                ParallelZombies.sendMessageTo(attacker, "You killed " + victim.getName());
                ParallelZombies.sendMessageTo(victim, "You were killed by " + attacker.getName());
            }
        }
        else if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Zombie zombie) {
            ZombiesPlayer plv = ParallelZombies.gameManager.getPlayer(victim);
            if (plv.getTeam() != Team.SURVIVOR) {
                event.setCancelled(true);
                zombie.setTarget(ParallelZombies.gameManager.getRandomSurvivorByDistance(zombie));
            }
        }
        else if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Zombie) {
            ZombiesPlayer pla = ParallelZombies.gameManager.getPlayer(attacker);
            if (pla.getTeam() == Team.ZOMBIE)
                event.setCancelled(true);
        }
    }
}
