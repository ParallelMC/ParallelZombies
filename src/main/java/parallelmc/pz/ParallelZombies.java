package parallelmc.pz;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import parallelmc.pz.commands.*;
import parallelmc.pz.events.*;

import java.util.logging.Level;

import static parallelmc.pz.utils.ZombieUtils.createMessage;

public class ParallelZombies extends JavaPlugin {
    public static Level LOG_LEVEL = Level.FINER;//Level.INFO;
    public static GameManager gameManager;

    public static ParallelZombies instance;

    @Override
    public void onLoad() { }

    @Override
    public void onEnable() {
        instance = this;
        PluginManager manager = Bukkit.getPluginManager();

        manager.registerEvents(new OnBlockBreak(), this);
        manager.registerEvents(new OnPlaceBlock(), this);
        manager.registerEvents(new OnChat(), this);
        manager.registerEvents(new OnDamage(), this);
        manager.registerEvents(new OnDamageEntity(), this);
        manager.registerEvents(new OnInventoryClick(), this);
        manager.registerEvents(new OnPlayerJoin(), this);
        manager.registerEvents(new OnPlayerLeave(), this);
        manager.registerEvents(new OnRightClick(), this);
        manager.registerEvents(new OnPlayerManipulateArmorStand(), this);
        manager.registerEvents(new OnEntityTarget(), this);
        manager.registerEvents(new OnDropItem(), this);
        manager.registerEvents(new OnHangingBreak(), this);

        this.getCommand("startgame").setExecutor(new StartGame());
        this.getCommand("endgame").setExecutor(new EndGame());
        this.getCommand("volunteer").setExecutor(new Volunteer());
        this.getCommand("votestart").setExecutor(new VoteStart());
        this.getCommand("setmode").setExecutor(new SetMode());
        this.getCommand("votemode").setExecutor(new VoteMode());

        // todo: this should be dynamic
        World world = this.getServer().getWorld("parallel-zombies");
        if (world == null) {
            log(Level.SEVERE, "Could not find map parallel-zombies!");
            return;
        }

        ZombiesMap map = ZombiesMap.loadFromConfig("map.yml");

        if (map == null){
            log("An error occurred loading map.");
            return;
        }
        ParallelZombies.log(Level.WARNING, "Loaded map config");

        gameManager = new GameManager(this, map);

        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        // keep inv makes it easier to respawn zombies
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.DO_MOB_LOOT, false);
    }

    @Override
    public void onDisable() { }

    public static void log(String message) { Bukkit.getLogger().log(LOG_LEVEL, "[ParallelZombies] " + message); }

    public static void log(Level level, String message) { Bukkit.getLogger().log(level, "[ParallelZombies] " + message); }

    public static void sendMessageTo(Player player, String message) {
        player.sendMessage(createMessage(message));
    }

    public static void sendActionBarTo(Player player, String message){
        player.sendActionBar(createMessage(message));
    }

    public static void sendMessage(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendMessageTo(p, message);
        }
    }
}
