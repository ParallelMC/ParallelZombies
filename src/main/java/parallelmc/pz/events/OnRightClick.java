package parallelmc.pz.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import parallelmc.pz.ParallelZombies;
import parallelmc.pz.ZombiesPlayer;

public class OnRightClick implements Listener {
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            if (player.getInventory().getItemInMainHand().getType() == Material.STONE_AXE) {
                event.setCancelled(true);
                ZombiesPlayer pl = ParallelZombies.gameManager.getPlayer(player);
                if (pl.isLeapCooldown()) {
                    ParallelZombies.sendMessageTo(player, "Your Leap is on cooldown!");
                    return;
                }
                if (player.getInventory().getItemInMainHand().getType() == Material.STONE_AXE) {
                    player.setVelocity(player.getLocation().getDirection().normalize().multiply(1.5f));
                    ParallelZombies.sendMessageTo(player, "You used Leap!");
                    pl.startLeapCooldown();
                }
            }
        }
    }
}
