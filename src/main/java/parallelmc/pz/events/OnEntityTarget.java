package parallelmc.pz.events;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import parallelmc.pz.ParallelZombies;

import java.util.logging.Level;

public class OnEntityTarget implements Listener {
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntityType() == EntityType.ZOMBIE) {
            if (event.getReason() != EntityTargetEvent.TargetReason.CUSTOM && event.getReason() != EntityTargetEvent.TargetReason.UNKNOWN) {
                Player target = ParallelZombies.gameManager.getRandomSurvivor();
                if (target == null) {
                    ParallelZombies.log(Level.WARNING, "Failed to find target for zombie spawn.");
                    return;
                }
                event.setTarget(target);
            }
        }
    }
}
