package parallelmc.pz;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import parallelmc.pz.commands.*;
import parallelmc.pz.events.*;

import java.io.File;
import java.util.logging.Level;

public class ParallelZombies extends JavaPlugin {
    public static Level LOG_LEVEL = Level.INFO;
    public static GameManager gameManager;

    @Override
    public void onLoad() { }

    @Override
    public void onEnable() {
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

        this.getCommand("startgame").setExecutor(new StartGame());

        // TODO: support loading multiple maps
        World world = this.getServer().getWorld("world");
        if (world == null) {
            log(Level.SEVERE, "Could not find map world!");
            return;
        }

        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(new File(this.getDataFolder(), "map.yml"));
        } catch (Exception e) {
            ParallelZombies.log(Level.WARNING, "Failed to load map");
            ParallelZombies.log(Level.WARNING, e.toString());
            return;
        }
        ZombiesMap map = new ZombiesMap(
                "map",
                world,
                new Location(world, config.getDouble("players.x"), config.getDouble("players.y"), config.getDouble("players.z")),
                new Location(world, config.getDouble("zombies.x"), config.getDouble("zombies.y"), config.getDouble("zombies.z")));
        ParallelZombies.log(Level.WARNING, "Loaded map config");
        gameManager = new GameManager(this, map);

        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        // keep inv makes it easier to respawn zombies
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
    }

    @Override
    public void onDisable() { }

    public static void log(String message) { Bukkit.getLogger().log(LOG_LEVEL, "[ParallelZombies] " + message); }

    public static void log(Level level, String message) { Bukkit.getLogger().log(level, "[ParallelZombies] " + message); }

    public static void sendMessageTo(Player player, String message) {
        Component msg = Component.text("§3[§f§lZombies§3] §a" + message);
        player.sendMessage(msg);
    }

    public static void sendMessage(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendMessageTo(p, message);
        }
    }
}
