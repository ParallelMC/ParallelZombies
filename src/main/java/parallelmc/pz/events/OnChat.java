package parallelmc.pz.events;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class OnChat implements Listener {
    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) -> sourceDisplayName
                .append(Component.text(" > ", NamedTextColor.DARK_GRAY))
                .append(message.color(TextColor.color(255, 255, 255)))));
        event.message(event.message());
    }
}
