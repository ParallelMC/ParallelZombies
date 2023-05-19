package parallelmc.pz.events;

import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import parallelmc.pz.ParallelZombies;

public class OnPlayerJoin implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // TODO: handle joining during a game
        Player p = event.getPlayer();
        p.setGameMode(GameMode.SURVIVAL);
        p.getInventory().clear();
        for (PotionEffect e : p.getActivePotionEffects()) {
            p.removePotionEffect(e.getType());
        }
        p.setHealth(20D);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 0));
        AttributeInstance instance = p.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (instance != null) {
            instance.setBaseValue(15.9);
        }
        ParallelZombies.gameManager.addPlayer(event.getPlayer());
    }
}
