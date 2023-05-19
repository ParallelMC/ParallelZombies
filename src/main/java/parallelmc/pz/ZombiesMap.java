package parallelmc.pz;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Random;

public class ZombiesMap {
    public String name;
    // TODO: make these areas instead of a single point
    public final Location playerSpawn;
    public final Location zombieSpawn;
    public final World world;

    public ZombiesMap(String name, World world, Location playerSpawn, Location zombieSpawn) {
        this.name = name;
        this.world = world;
        this.playerSpawn = playerSpawn;
        this.zombieSpawn = zombieSpawn;
    }

    public Location getPlayerSpawnPoint() {
        return this.playerSpawn;
    }

    public Location getZombieSpawnPoint() {
        return this.zombieSpawn;
    }

}
