package parallelmc.pz.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class OnDropItem implements Listener {

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (!event.getPlayer().isOp())
            event.setCancelled(true);
    }
}
