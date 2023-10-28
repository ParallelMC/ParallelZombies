package parallelmc.pz.events;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import parallelmc.pz.ParallelZombies;

import java.util.logging.Level;

public class OnEntityTarget implements Listener {
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntityType() == EntityType.ZOMBIE) {
            EntityTargetEvent.TargetReason reason = event.getReason();
            // if a zombie forgets their target, choose a different survivor
            if (reason == EntityTargetEvent.TargetReason.FORGOT_TARGET) {
                Zombie zombie = (Zombie)event.getEntity();
                zombie.setTarget(ParallelZombies.gameManager.getRandomSurvivorByDistance(zombie));
                return;
            }
            if (reason != EntityTargetEvent.TargetReason.CUSTOM
                    && reason != EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY
                    && reason != EntityTargetEvent.TargetReason.TARGET_ATTACKED_NEARBY_ENTITY
                    && reason != EntityTargetEvent.TargetReason.UNKNOWN) {
                event.setCancelled(true);
            }
        }
    }
}
