package parallelmc.pz.utils;

import com.mojang.datafixers.util.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

public class ZombieUtils {
    /**
     * Weighted Randomizer
     * @param values An {@link ArrayList} of {@link Pair Pairs} of {@link T} & it's {@link Integer weight}
     * @return Weighted choice of {@link T}
     * @param <T> The type of object we are randomizing
     */
    public static <T> T weightedChoice(ArrayList<Pair<T, Integer>> values){
        TreeMap<Integer, T> map = new TreeMap<>();
        Random rng = new Random();
        int t = 0;

        for (Pair<T, Integer> _pair: values) {
            t +=_pair.getSecond();
            map.put(t, _pair.getFirst());
        }
        return map.ceilingEntry(rng.nextInt(t) + 1).getValue();
    }

    public static TextComponent messagePrefix = Component.text('[')
            .color(NamedTextColor.AQUA)
            .append(Component.text("Zombies")
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, true)
            )
            .append(Component.text(']'))
            .append(Component.text(' '));

    public static TextComponent createMessage(String message){
        return messagePrefix
                .append(Component.text(message).color(NamedTextColor.GREEN));
    }
}
