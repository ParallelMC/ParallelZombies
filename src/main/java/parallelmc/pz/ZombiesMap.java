package parallelmc.pz;

import com.mojang.datafixers.util.Pair;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import parallelmc.pz.utils.ZombieUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import static parallelmc.pz.ParallelZombies.log;
import static parallelmc.pz.utils.ZombieUtils.getRandomLocationInRadius;

public class ZombiesMap {
    public String name;
    public final ArrayList<Pair<Location, Double>> playerSpawns;
    public final ArrayList<Pair<Location, Double>> zombieSpawns;
    public final World world;



    public ZombiesMap(String name, World world, ArrayList<Pair<Location, Double>> playerSpawns, ArrayList<Pair<Location, Double>> zombieSpawns) {
        this.name = name;
        this.world = world;
        this.playerSpawns = playerSpawns;
        this.zombieSpawns = zombieSpawns;
    }

    /**
     * Get a random player spawn point
     */
    public Location getPlayerSpawnPoint() {
        // First, select a random spawn 'zone' from the list
        Pair<Location, Double> spawn = playerSpawns.get(ZombieUtils.rng.nextInt(playerSpawns.size()-1));

        // if the radius is 0, just use the exact spot
        if(spawn.getSecond() == 0d) return spawn.getFirst();

        // otherwise, get a random spot in that radius
        return getRandomLocationInRadius(spawn.getFirst(), spawn.getSecond());
    }

    /**
     * Get a random zombie spawn point
     */
    public Location getZombieSpawnPoint() {
        // First, select a random spawn 'zone' from the list
        Pair<Location, Double> spawn = zombieSpawns.get(ZombieUtils.rng.nextInt(zombieSpawns.size()-1));

        // if the radius is 0, just use the exact spot
        if(spawn.getSecond() == 0d) return spawn.getFirst();

        // otherwise, get a random spot in that radius
        return getRandomLocationInRadius(spawn.getFirst(), spawn.getSecond());
    }

    /**
     * Loads map from the specified config file name
     * @param fileName The yml {@link String filename} (found in ~/plugins/ParallelZombies/)
     * @return New instance of {@link ZombiesMap}
     */
    public static ZombiesMap loadFromConfig(String fileName){
        // todo: this should be dynamic
        World world = ParallelZombies.instance.getServer().getWorld("world");
        if (world == null) {
            log(Level.SEVERE, "Could not find map world!");
            return null;
        }

        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(new File(ParallelZombies.instance.getDataFolder(), fileName));
        } catch (Exception e) {
            ParallelZombies.log(Level.WARNING, "Failed to load map");
            ParallelZombies.log(Level.WARNING, e.toString());
            return null;
        }

        ArrayList<HashMap<String, Double>> _playerSpawns = (ArrayList<HashMap<String, Double>>) config.getList("players");
        ArrayList<HashMap<String, Double>> _zombieSpawns = (ArrayList<HashMap<String, Double>>) config.getList("zombies");

        ArrayList<Pair<Location, Double>> playerSpawns = new ArrayList<>();
        ArrayList<Pair<Location, Double>> zombieSpawns = new ArrayList<>();

        if(_playerSpawns == null || _zombieSpawns == null){
            log(Level.SEVERE, "Error loading map. Make sure you have player and zombie spawns specified.");
            return null;
        }

        try {
            for (HashMap<String, Double> spawn : _playerSpawns) {
                playerSpawns.add(new Pair<>(
                        new Location(world, spawn.get("x"), spawn.get("y"), spawn.get("z")),
                        spawn.getOrDefault("radius", 0d)));
            }

            for (HashMap<String, Double> spawn : _zombieSpawns) {
                zombieSpawns.add(new Pair<>(
                        new Location(world, spawn.get("x"), spawn.get("y"), spawn.get("z")),
                        spawn.getOrDefault("radius", 0d)));
            }
        }
        catch (Exception e){
            log(Level.SEVERE, "Error loading map. " + e.getLocalizedMessage());
            return null;
        }

        return new ZombiesMap("map", world, playerSpawns, zombieSpawns);
    }
}
