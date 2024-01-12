package parallelmc.pz;

import com.comphenix.protocol.wrappers.Pair;
import me.libraryaddict.disguise.DisguiseAPI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import parallelmc.pz.gamemodes.LastSurvivorGamemode;
import parallelmc.pz.gamemodes.SurvivalGamemode;
import parallelmc.pz.gamemodes.ZombiesGamemode;
import parallelmc.pz.utils.ZombieUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Stream;

import static parallelmc.pz.utils.ZombieUtils.weightedChoice;

public class GameManager {
    private final Plugin plugin;
    private final HashMap<UUID, ZombiesPlayer> players = new HashMap<>();
    public GameState gameState;
    public ZombiesMap map;
    private final HashMap<String, ZombiesGamemode> gameModes = new HashMap<>();
    public ZombiesGamemode currentGamemode;
    private boolean isGamemodeForced;

    private final HashSet<UUID> volunteerPool = new HashSet<>();
    private final HashSet<UUID> voteStart = new HashSet<>();
    private final HashSet<UUID> voteGamemode = new HashSet<>();
    private final HashMap<String, Integer> gamemodeVotes = new HashMap<>();


    public GameManager(Plugin plugin, ZombiesMap map) {
        this.plugin = plugin;
        this.gameState = GameState.PREGAME;
        this.map = map;
        this.gameModes.put("last_survivor_standing", new LastSurvivorGamemode(plugin));
        this.gameModes.put("survival", new SurvivalGamemode(plugin));
        this.currentGamemode = gameModes.get("last_survivor_standing");
        this.isGamemodeForced = false;
        gameModes.forEach((n, m) -> gamemodeVotes.put(n, 0));
        doPregame();
    }

    private void doPregame() {
        this.plugin.getServer().getWorld("parallel-zombies").getEntities().stream().filter(x -> x.getType() == EntityType.ZOMBIE).forEach(Entity::remove);

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
                if (isGamemodeForced) {
                    z.updateLobbyBoard(voteStart.size(), Math.max(players.size() - 1, 3), currentGamemode.getName(), -1);
                }
                else {
                    String winningMode = "";
                    int winningVotes = -1;
                    for (Map.Entry<String, Integer> mode : gamemodeVotes.entrySet()) {
                        if (mode.getValue() > winningVotes) {
                            winningMode = mode.getKey();
                            winningVotes = mode.getValue();
                        }
                    }
                    if (winningVotes == -1) {
                        ParallelZombies.log(Level.SEVERE, "gamemodeVotes set is empty when calculating vote winner!");
                    }
                    else {
                        currentGamemode = gameModes.get(winningMode);
                        z.updateLobbyBoard(voteStart.size(), Math.max(players.size() - 1, 3), currentGamemode.getName(), winningVotes);
                    }
                }
                player.setFoodLevel(23);
                if (player.getLocation().getBlockY() < -64) {
                    player.teleport(map.lobby);
                }
                if (volunteerPool.contains(player.getUniqueId())) {
                    ParallelZombies.sendActionBarTo(player, String.format("You are volunteering to be the Alpha Zombie! (%.1f%% chance)", 100f / volunteerPool.size()));
                }
            });

            if (players.size() > 2 && voteStart.size() >= players.size() - 1) {
                ParallelZombies.sendMessage("Vote passed! Starting in 15 seconds...");
                startGame();
            }
        }, 0L, 20L);
    }

    public void startGame() {
        this.plugin.getServer().getScheduler().cancelTasks(plugin);
        voteStart.clear();
        voteGamemode.clear();
        for (Map.Entry<String, Integer> m : gamemodeVotes.entrySet()) {
            m.setValue(0);
        }
        isGamemodeForced = false;
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
                    if (countdown < 5) {
                        z.getMcPlayer().playSound(Sound.sound(Key.key("block.note_block.hat"), Sound.Source.MASTER, 0.5f, 1f));
                    }
                });
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
                    players.forEach((p, z) -> {
                        z.getMcPlayer().playSound(Sound.sound(Key.key("entity.ender_dragon.growl"), Sound.Source.MASTER, 0.5f, 0.9f));
                        if (z.getTeam() == Team.ZOMBIE) {
                            z.getMcPlayer().showTitle(currentGamemode.getZombieTitle());
                        }
                        else {
                            z.getMcPlayer().showTitle(currentGamemode.getSurvivorTitle());
                        }
                    });
                    this.cancel();
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

    }

    private void doGame() {
        this.currentGamemode.doGame();

        this.plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (currentGamemode.winCondition()) {
                endGame(GameEndReason.NORMAL);
            }
        }, 0L, 1L);

        // TODO: try and scale for number of players
        this.plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Location loc : ParallelZombies.gameManager.map.getAllZombieSpawnPoints())
                for (int i = 0; i < ZombieUtils.rng.nextInt(1, 4); i++)
                    ParallelZombies.gameManager.spawnZombie(loc);
        }, 0L, 240L);
    }

    public void spawnZombie(Location spawn) {
        Zombie zombie = (Zombie)map.world.spawnEntity(spawn, EntityType.ZOMBIE);
        zombie.setAdult();
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, PotionEffect.INFINITE_DURATION, 0));
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, 0));
        zombie.setShouldBurnInDay(false);
        zombie.setConversionTime(-1);
        zombie.setTarget(getRandomSurvivorByDistance(zombie));
        if (zombie.getVehicle() != null) {
            zombie.getVehicle().remove();
        }
    }

    public void endGame(GameEndReason reason) {
        this.plugin.getServer().getScheduler().cancelTasks(plugin);
        this.gameState = GameState.ENDING;
        if (reason == GameEndReason.NORMAL) {
            currentGamemode.endGame();
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

        if (gameState == GameState.PLAY) {
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
            // don't include targets over a certain distance away
            if(distance < 50){
                // convert shorter distances to be higher weights
                int weight = (int) ((1./distance) * 100);

                arr.add(new Pair<>(player.getMcPlayer(), Math.min(weight, 500)));
            }
        }

        if (arr.size() == 0){
            ParallelZombies.log(Level.WARNING, String.format("Failed to find a target for zombie %s spawn.", zombie.getUniqueId()));
            return null;
        }


        return weightedChoice(arr);

    }

    public void runForEachPlayer(Consumer<ZombiesPlayer> action) {
        players.values().forEach(action);
    }

    public Stream<ZombiesPlayer> filterPlayers(Predicate<ZombiesPlayer> predicate) {
        return players.values().stream().filter(predicate);
    }

    public boolean forceGameMode(String modeID) {
        ZombiesGamemode mode = gameModes.get(modeID);
        if (mode == null)
            return false;
        isGamemodeForced = true;
        currentGamemode = mode;
        ParallelZombies.sendMessage("The gamemode has been forced to " + currentGamemode.getName() + "!");
        return true;
    }

    public List<String> getGameModes() {
        return gameModes.keySet().stream().toList();
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

    public void addVoteGamemode(Player player, String modeID) {
        if (!gameModes.containsKey(modeID)) {
            ParallelZombies.log(Level.WARNING, "Player tried to vote for unknown mode " + modeID);
            return;
        }
        voteGamemode.add(player.getUniqueId());
        int votes = gamemodeVotes.get(modeID);
        gamemodeVotes.put(modeID, votes + 1);
        ParallelZombies.sendMessage(player.getName() + " voted for the " + gameModes.get(modeID).getName() + " gamemode! (" + (votes + 1) + " votes)");
    }

    public boolean hasVotedForGamemode(Player player) {
        return voteGamemode.contains(player.getUniqueId());
    }

    public boolean hasGamemodeBeenForced() {
        return isGamemodeForced;
    }

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
