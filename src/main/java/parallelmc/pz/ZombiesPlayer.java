package parallelmc.pz;

import fr.mrmicky.fastboard.FastBoard;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class ZombiesPlayer {
    private final Player player;
    private final FastBoard board;
    private Team team;
    private boolean isAlpha = false;
    private final BossBar bossBar = BossBar.bossBar(Component.text("Leap Cooldown"), 1, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
    private boolean leapCooldown;

    public ZombiesPlayer(Player player) {
        this.player = player;
        this.board = new FastBoard(this.player);
        this.board.updateTitle("§lParallel§l§cZombies");
        // everyone stars as a survivor until the game begins
        this.team = Team.SURVIVOR;
        this.leapCooldown = false;
    }

    public void updateLobbyBoard() {
        this.board.updateLines(
                "",
                "§eWaiting to start..."
        );
    }

    public void updateStartingBoard(int countdown) {
        this.board.updateLines(
                "",
                "§eStarting in",
                "§e" + countdown + " seconds"
        );
    }

    public void updateBoard(int survivorsLeft, int zombiesLeft) {
        this.board.updateLines(
                "",
                "§aSurvivors",
                "§e" + survivorsLeft,
                "",
                "§cZombies",
                "§e" + zombiesLeft
        );
    }

    public void updateEndingBoard(int countdown) {
        this.board.updateLines(
                "",
                "Returning to lobby in:",
                countdown + " seconds"
        );
    }

    public void handleDeath() {
        Player p = this.player;
        p.setHealth(20d);
        if (this.team == Team.ZOMBIE) {
            // spawn a fake wither skeleton to simulate death
            WitherSkeleton sk = (WitherSkeleton)p.getWorld().spawnEntity(p.getLocation(), EntityType.WITHER_SKELETON);
            sk.setAI(false);
            sk.setGravity(false);
            sk.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_AXE));
            sk.damage(20D);
            player.teleport(ParallelZombies.gameManager.map.getZombieSpawnPoint());
        }
        else {
            this.makeZombie(false);
        }
    }

    public void resetPlayer() {
        player.clearActivePotionEffects();
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        isAlpha = false;
        team = Team.SURVIVOR;
    }

    public void makeZombie(boolean alphaZombie) {
        this.isAlpha = alphaZombie;
        MobDisguise disguise = isAlpha ? new MobDisguise(DisguiseType.WITHER_SKELETON) : new MobDisguise(DisguiseType.SKELETON);
        disguise.setViewSelfDisguise(false);
        disguise.setEntity(player);
        LivingWatcher watcher = disguise.getWatcher();
        watcher.setItemInMainHand(new ItemStack(Material.STONE_AXE));
        watcher.setCustomName("§c" + player.getName());
        watcher.setCustomNameVisible(true);
        disguise.startDisguise();
        equipZombie();
        this.team = Team.ZOMBIE;
        player.teleport(ParallelZombies.gameManager.map.getZombieSpawnPoint());
        ParallelZombies.sendMessage(player.getName() + " has turned into a zombie!");
    }

    public void equipSurvivor() {
        PlayerInventory inv = player.getInventory();
        for (PotionEffect e : player.getActivePotionEffects()) {
            player.removePotionEffect(e.getType());
        }
        inv.clear();
        inv.setItem(0, unbreakableItem(Material.IRON_SWORD));
        inv.setArmorContents(new ItemStack[] {
                unbreakableItem(Material.IRON_BOOTS),
                unbreakableItem(Material.IRON_LEGGINGS),
                unbreakableItem(Material.IRON_CHESTPLATE),
                unbreakableItem(Material.IRON_HELMET)
        });
    }

    public void equipZombie() {
        PlayerInventory inv = player.getInventory();
        for (PotionEffect e : player.getActivePotionEffects()) {
            player.removePotionEffect(e.getType());
        }
        inv.clear();
        inv.setItem(0, unbreakableItem(Material.STONE_AXE, "Right-click to use your Leap!"));
        if (isAlpha) {
            inv.setArmorContents(new ItemStack[]{
                    new ItemStack(Material.AIR),
                    new ItemStack(Material.AIR),
                    new ItemStack(Material.AIR),
                    new ItemStack(Material.WITHER_SKELETON_SKULL)
            });
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, 1));
        }
        else {
            inv.setArmorContents(new ItemStack[]{
                    new ItemStack(Material.AIR),
                    new ItemStack(Material.AIR),
                    new ItemStack(Material.AIR),
                    new ItemStack(Material.SKELETON_SKULL)
            });
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, PotionEffect.INFINITE_DURATION, 0));
        }
    }

    public void equipSpectator() {
        team = Team.SPECTATOR;
        PlayerInventory inv = player.getInventory();
        for (PotionEffect e : player.getActivePotionEffects()) {
            player.removePotionEffect(e.getType());
        }
        inv.clear();
        player.setGameMode(GameMode.SPECTATOR);
        ParallelZombies.sendMessageTo(player, "You have joined a game in progress, you can spectate until the game ends.");
    }

    public void startLeapCooldown() {
        leapCooldown = true;
        bossBar.progress(1f);
        player.showBossBar(bossBar);
        new BukkitRunnable() {
            int cooldown = 80;
            @Override
            public void run() {
                bossBar.progress(cooldown / 80f);
                if (cooldown <= 0) {
                    leapCooldown = false;
                    player.hideBossBar(bossBar);
                    this.cancel();
                }
                cooldown--;
            }
        }.runTaskTimer(ParallelZombies.gameManager.getPlugin(), 0L, 2L);
    }

    private ItemStack unbreakableItem(Material material) {
        ItemStack i = new ItemStack(material);
        ItemMeta meta = i.getItemMeta();
        meta.setUnbreakable(true);
        i.setItemMeta(meta);
        return i;
    }

    private ItemStack unbreakableItem(Material material, String lore) {
        ItemStack i = new ItemStack(material);
        ItemMeta meta = i.getItemMeta();
        meta.setUnbreakable(true);
        meta.lore(List.of(Component.text(lore, NamedTextColor.GRAY)));
        i.setItemMeta(meta);
        return i;
    }

    public Player getMcPlayer() { return this.player; }
    public Team getTeam() { return this.team; }
    public boolean isLeapCooldown() { return this.leapCooldown; }

    public void deleteBoard() { this.board.delete(); }
}
