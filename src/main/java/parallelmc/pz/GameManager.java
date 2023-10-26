package parallelmc.pz;

import com.comphenix.protocol.wrappers.Pair;
import me.libraryaddict.disguise.DisguiseAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import parallelmc.pz.utils.ZombieUtils;

import java.util.*;
import java.util.logging.Level;

import static parallelmc.pz.utils.ZombieUtils.weightedChoice;

public class GameManager {
    private final Plugin plugin;
    private final HashMap<UUID, ZombiesPlayer> players = new HashMap<>();
    public GameState gameState;
    public ZombiesMap map;

    private final HashSet<UUID> volunteerPool = new HashSet<>();

    private final HashSet<UUID> voteStart = new HashSet<>();

    public GameManager(Plugin plugin, ZombiesMap map) {
        this.plugin = plugin;
        this.gameState = GameState.PREGAME;
        this.map = map;
        doPregame();
    }

    private void doPregame() {
        this.plugin.getServer().getWorld("parallel_zombies").getEntities().stream().filter(x -> x.getType() == EntityType.ZOMBIE).forEach(Entity::remove);

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
                z.updateLobbyBoard(voteStart.size(), Math.max(players.size() - 1, 3));
                player.setFoodLevel(23);
            });

            if (voteStart.size() >= players.size() - 1) {
                ParallelZombies.sendMessage("Vote passed! Starting in 15 seconds...");
                voteStart.clear();
                startGame();
            }
        }, 0L, 20L);
    }

    public void startGame() {
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
                players.forEach((p, z) -> z.updateStartingBoard(countdown));
                if (countdown <= 0) {
                    // choose random player to become a zombie
                    ZombiesPlayer target;
                    if (volunteerPool.size() > 0) {
                        target = players.get((UUID)volunteerPool.toArray()[ZombieUtils.rng.nextInt(volunteerPool.size())]);
                    }
                    else {
                        // if no one volunteers then pick someone at random
                       target = players.values().stream().skip((int) (players.size() * Math.random())).findFirst().orElse(null);
                    }
                    volunteerPool.clear();
                    if (target == null) {
                        ParallelZombies.log(Level.SEVERE, "Failed to select a player to be a zombie!");
                        return;
                    }
                    target.makeZombie(true);
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
                z.getMcPlayer().setFoodLevel(23);
            });

        }, 0L, 20L);

        this.plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (getSurvivorsLeft() == 1) {
                endGame(GameEndReason.NORMAL);
            }
        }, 0L, 1L);

        // TODO: try and scale for number of players
        this.plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (int i = 0; i < ZombieUtils.rng.nextInt(1, 5); i++)
                spawnZombie();
        }, 0L, 200L);
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

    public void endGame(GameEndReason reason) {
        this.plugin.getServer().getScheduler().cancelTasks(plugin);
        this.gameState = GameState.ENDING;
        if (reason == GameEndReason.NORMAL) {
            ZombiesPlayer winner = players.values().stream().filter(x -> x.getTeam() == Team.SURVIVOR).findFirst().orElse(null);
            if (winner == null) {
                ParallelZombies.log(Level.SEVERE, "Failed to retrieve the winning player!");
                return;
            }
            ParallelZombies.sendMessage(winner.getMcPlayer().getName() + " is the winner!");
        }
        else if (reason == GameEndReason.NOT_ENOUGH_PLAYERS) {
            ParallelZombies.sendMessage("Ending the game early as there are not enough players to continue.");
        }
        else if (reason == GameEndReason.COMMAND) {
            ParallelZombies.sendMessage("Ending the game early as requested by an operator.");
        }
        else if (reason == GameEndReason.ERROR) {
            ParallelZombies.sendMessage("Ending the game early due to an error.");
        }
        else {
            ParallelZombies.log(Level.SEVERE, "Unhandled game end reason: " + reason);
        }
        new BukkitRunnable() {
            int countdown = 10;
            @Override
            public void run() {
                players.forEach((p, z) -> z.updateEndingBoard(countdown));
                if (countdown <= 0) {
                    players.forEach((p, z) -> {
                        z.resetPlayer();
                        z.getMcPlayer().teleport(map.lobby);
                    });
                    gameState = GameState.PREGAME;
                    doPregame();
                    this.cancel();
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void addPlayer(Player player) {
        ZombiesPlayer pl = new ZombiesPlayer(player);
        players.put(player.getUniqueId(), pl);
        player.displayName(Component.text(player.getName(), NamedTextColor.GREEN));
        player.playerListName(Component.text(player.getName(), NamedTextColor.GREEN));
        if (gameState != GameState.PREGAME) {
            pl.equipSpectator();
            player.teleport(map.getPlayerSpawnPoint());
        }
        else {
            player.teleport(map.lobby);
        }
    }

    public void removePlayer(Player player) {
        ZombiesPlayer pl = getPlayer(player);
        pl.deleteBoard();
        players.remove(player.getUniqueId());

        // edge cases
        if (getSurvivorsLeft() > 0 && getZombiesLeft() == 0) {
            if (players.size() == 2) {
                endGame(GameEndReason.NOT_ENOUGH_PLAYERS);
                return;
            }
            ZombiesPlayer target = getRandomSurvivor();
            if (target == null) {
                endGame(GameEndReason.ERROR);
                ParallelZombies.log(Level.SEVERE, "Failed to find replacement zombie!");
                return;
            }
            ParallelZombies.sendMessage("No more zombies remaining! Choosing another at random...");
            target.makeZombie(true);
        }
    }

    public ZombiesPlayer getRandomSurvivor() {
        ZombiesPlayer target = players.values().stream().filter(x -> x.getTeam() == Team.SURVIVOR).skip((int) (players.size() * Math.random())).findFirst().orElse(null);
        if (target == null) {
            ParallelZombies.log(Level.WARNING, "Failed to find a survivor");
            return null;
        }
        return target;
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

    public void addVolunteer(Player player) {
        volunteerPool.add(player.getUniqueId());
    }

    public void removeVolunteer(Player player) {
        volunteerPool.remove(player.getUniqueId());
    }

    public boolean hasVolunteered(Player player) {
        return volunteerPool.contains(player.getUniqueId());
    }

    public void addVoteStart(Player player) {
        voteStart.add(player.getUniqueId());
    }

    public boolean hasVotedToStart(Player player) {
        return voteStart.contains(player.getUniqueId());
    }

    public int currentVotesToStart() { return voteStart.size(); }

    public ZombiesPlayer getPlayer(Player player) { return players.get(player.getUniqueId()); }

    public int getSurvivorsLeft() {
        return (int)players.values().stream().filter(x -> x.getTeam() == Team.SURVIVOR).count();
    }

    public int getZombiesLeft() {
        return (int)players.values().stream().filter(x -> x.getTeam() == Team.ZOMBIE).count();
    }

    public int playerCount() { return players.size(); }

    public Plugin getPlugin() { return this.plugin; }

}
