package parallelmc.pz;

import com.comphenix.protocol.wrappers.Pair;
import me.libraryaddict.disguise.DisguiseAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static parallelmc.pz.utils.ZombieUtils.weightedChoice;

public class GameManager {
    private final Plugin plugin;
    private final HashMap<UUID, ZombiesPlayer> players = new HashMap<>();
    public GameState gameState;
    public ZombiesMap map;

    public GameManager(Plugin plugin, ZombiesMap map) {
        this.plugin = plugin;
        this.gameState = GameState.PREGAME;
        this.map = map;
        doPregame();
    }

    private void doPregame() {
        this.plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (gameState != GameState.PREGAME) {
                ParallelZombies.log(Level.SEVERE, "PreGame loop running during " + gameState + ". This shouldn't be happening!");
                return;
            }
            players.forEach((p, z) -> {
                Player player = plugin.getServer().getPlayer(p);
                if (player == null) {
                    ParallelZombies.log(Level.WARNING, "Couldn't find player with UUID of " + p);
                    return;
                }
                if (DisguiseAPI.isDisguised(player)) {
                    DisguiseAPI.undisguiseToAll(player);
                }
                // TODO: This currently does nothing, but handle needed players, etc. in the future
                z.updateLobbyBoard();
            });
        }, 0L, 20L);
    }

    public void startGame() {
        // TODO: teleport players to map
        this.plugin.getServer().getScheduler().cancelTasks(plugin);
        players.forEach((p, z) -> {
            z.equipSurvivor();
            z.getMcPlayer().teleport(map.getPlayerSpawnPoint());
        });
        this.gameState = GameState.STARTING;
        new BukkitRunnable() {
            int countdown = 15;
            @Override
            public void run() {
                players.forEach((p, z) -> {
                    z.updateStartingBoard(countdown);
                });
                if (countdown <= 0) {
                    // choose random player to become a zombie
                    ZombiesPlayer target = players.values().stream().skip((int) (players.size() * Math.random())).findFirst().orElse(null);
                    if (target == null) {
                        ParallelZombies.log(Level.SEVERE, "Failed to select a player to be a zombie!");
                        return;
                    }
                    target.makeZombie();
                    gameState = GameState.PLAY;
                    doGame();
                    this.cancel();
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

    }

    private void doGame() {
        this.plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (gameState != GameState.PLAY) {
                ParallelZombies.log(Level.SEVERE, "Game loop running during " + gameState + ". This shouldn't be happening!");
                return;
            }
            players.forEach((p, z) -> {
                z.updateBoard(getSurvivorsLeft(), getZombiesLeft());
            });

        }, 0L, 20L);

        this.plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (getSurvivorsLeft() == 1) {
                endGame();
            }
        }, 0L, 1L);

        this.plugin.getServer().getScheduler().runTaskTimer(plugin, this::spawnZombie, 0L, 200L);
    }

    private void spawnZombie() {
        Location spawnPos = map.getZombieSpawnPoint();
        Zombie zombie = (Zombie)map.world.spawnEntity(spawnPos, EntityType.ZOMBIE);
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, PotionEffect.INFINITE_DURATION, 0));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, 0));
        zombie.setShouldBurnInDay(false);
        zombie.setTarget(getRandomSurvivorByDistance(zombie));
    }

    private void endGame() {
        this.plugin.getServer().getScheduler().cancelTasks(plugin);
        this.gameState = GameState.ENDING;
        ZombiesPlayer winner = players.values().stream().filter(x -> x.getTeam() == Team.SURVIVOR).findFirst().orElse(null);
        if (winner == null) {
            ParallelZombies.log(Level.SEVERE, "Failed to retrieve the winning player!");
            return;
        }
        ParallelZombies.sendMessage(winner.getMcPlayer().getName() + " is the winner!");
        // give the winner some time to celebrate
        new BukkitRunnable() {
            int countdown = 10;
            @Override
            public void run() {
                players.forEach((p, z) -> {
                    z.updateEndingBoard(countdown);
                });
                if (countdown <= 0) {
                    // TODO: teleport people back to the lobby
                    gameState = GameState.PREGAME;
                    doPregame();
                    this.cancel();
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void addPlayer(Player player) {
        players.put(player.getUniqueId(), new ZombiesPlayer(player));
        player.displayName(Component.text(player.getName(), NamedTextColor.GREEN));
        player.playerListName(Component.text(player.getName(), NamedTextColor.GREEN));
    }

    public void removePlayer(Player player) {
        // TODO: handle edge cases (player leaving as 1st zombie, player leaving as last survivor, etc.)
        ZombiesPlayer pl = getPlayer(player);
        pl.deleteBoard();
        players.remove(player.getUniqueId());
    }

    public Player getRandomSurvivor() {
        ZombiesPlayer target = players.values().stream().filter(x -> x.getTeam() == Team.SURVIVOR).skip((int) (players.size() * Math.random())).findFirst().orElse(null);
        if (target == null) {
            ParallelZombies.log(Level.WARNING, "Failed to find target for zombie spawn.");
            return null;
        }
        return target.getMcPlayer();
    }

    /**
     * Find a 'random' {@link Player survivor}, but prioritize close {@link Player survivors}.
     * <p>
     * <i>This is to include some element of randomness for the {@link Zombie zombies'} targeting,
     * but to not make it absurd where the {@link Zombie zombies} will keep targeting {@link Player survivors}
     * across the map</i>
     */
    public Player getRandomSurvivorByDistance(Zombie zombie){
        List<ZombiesPlayer> targets = players.values().stream().filter(x -> x.getTeam() == Team.SURVIVOR).toList();
        ArrayList<Pair<Player, Integer>> arr = new ArrayList<>();

        for (ZombiesPlayer player: targets) {
            double distance = zombie.getLocation().distance(player.getMcPlayer().getLocation());
            // don't include targets over a certain distance away (100 for testing)
            if(distance < 100){
                // convert shorter distances to be higher weights
                int weight = (int) ((1./distance) * 100);

                arr.add(new Pair<>(player.getMcPlayer(), Math.min(weight, 500)));
            }
        }

        if (arr.size() == 0){
            ParallelZombies.log(Level.WARNING, String.format("Failed to find a target for zombie %s spawn.", zombie.getUniqueId()));
            return null;
        }

        ParallelZombies.log(Level.FINER,
                String.format("Zombie %s Targets: %s", zombie.getUniqueId(), arr)
                );

        return weightedChoice(arr);

    }

    public ZombiesPlayer getPlayer(Player player) { return players.get(player.getUniqueId()); }

    public int getSurvivorsLeft() {
        return (int)players.values().stream().filter(x -> x.getTeam() == Team.SURVIVOR).count();
    }

    public int getZombiesLeft() {
        return (int)players.values().stream().filter(x -> x.getTeam() == Team.ZOMBIE).count();
    }

    public Plugin getPlugin() { return this.plugin; }

}
