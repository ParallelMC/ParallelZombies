package parallelmc.pz.gamemodes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.plugin.Plugin;

public abstract class ZombiesGamemode {
    private final String name;
    private final String survivorDescription;
    private final String zombieDescription;
    protected final Plugin plugin;

    public ZombiesGamemode(Plugin plugin, String name, String survivorDescription, String zombieDescription) {
        this.plugin = plugin;
        this.name = name;
        this.survivorDescription = survivorDescription;
        this.zombieDescription  = zombieDescription;
    }

    public abstract void doGame();

    public abstract boolean winCondition();

    public abstract void endGame();

    public Title getSurvivorTitle() {
        return Title.title(Component.text(name, NamedTextColor.GREEN), Component.text(survivorDescription, NamedTextColor.WHITE));
    }

    public Title getZombieTitle() {
        return Title.title(Component.text(name, NamedTextColor.RED), Component.text(zombieDescription, NamedTextColor.WHITE));
    }

    public String getName() {
        return name;
    }

    public String getSurvivorDescription() {
        return survivorDescription;
    }

    public String getZombieDescription() {
        return zombieDescription;
    }
}
