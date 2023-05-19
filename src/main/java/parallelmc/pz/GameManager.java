package parallelmc.pz;

import me.libraryaddict.disguise.DisguiseAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

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

    public ZombiesPlayer getPlayer(Player player) { return players.get(player.getUniqueId()); }

    public int getSurvivorsLeft() {
        return (int)players.values().stream().filter(x -> x.getTeam() == Team.SURVIVOR).count();
    }

    public int getZombiesLeft() {
        return (int)players.values().stream().filter(x -> x.getTeam() == Team.ZOMBIE).count();
    }

    public Plugin getPlugin() { return this.plugin; }

}