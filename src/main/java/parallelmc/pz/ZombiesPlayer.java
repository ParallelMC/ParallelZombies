package parallelmc.pz;

import fr.mrmicky.fastboard.FastBoard;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ZombiesPlayer {
    private final Player player;
    private final FastBoard board;
    private Team team;

    public ZombiesPlayer(Player player) {
        this.player = player;
        this.board = new FastBoard(this.player);
        this.board.updateTitle("§lParallel§cZombies");
        // everyone stars as a survivor until the game begins
        this.team = Team.SURVIVOR;
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
                "§eStarting in:",
                countdown + " seconds"
        );
    }

    public void updateBoard(int survivorsLeft, int zombiesLeft) {
        this.board.updateLines(
                "",
                "§aSurvivors:",
                "§e" + survivorsLeft,
                "",
                "§cZombies:",
                "§e" + zombiesLeft
        );
    }

    public void updateEndingBoard(int countdown) {
        this.board.updateLines(
                "",
                "§Returning to lobby in:",
                countdown + " seconds"
        );
    }

    public void makeZombie() {
        MobDisguise disguise = new MobDisguise(DisguiseType.WITHER_SKELETON);
        disguise.setViewSelfDisguise(false);
        disguise.setEntity(player);
        LivingWatcher watcher = disguise.getWatcher();
        watcher.setItemInMainHand(new ItemStack(Material.STONE_AXE));
        disguise.startDisguise();
        equipZombie();
        this.team = Team.ZOMBIE;
        ParallelZombies.sendMessage(player.getName() + " has turned into a zombie!");
    }

    public void equipSurvivor() {
        PlayerInventory inv = player.getInventory();
        player.getActivePotionEffects().clear();
        inv.clear();
        // TODO: add compass
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
        inv.clear();
        inv.setItem(0, unbreakableItem(Material.STONE_AXE));
        inv.setArmorContents(new ItemStack[] {
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.WITHER_SKELETON_SKULL)
        });
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 1));
    }

    private ItemStack unbreakableItem(Material material) {
        ItemStack i = new ItemStack(material);
        ItemMeta meta = i.getItemMeta();
        meta.setUnbreakable(true);
        i.setItemMeta(meta);
        return i;
    }

    public Player getMcPlayer() { return this.player; }
    public Team getTeam() { return this.team; }

    public void deleteBoard() { this.board.delete(); }
}
