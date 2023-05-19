package parallelmc.pz.utils;

import com.mojang.datafixers.util.Pair;

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
}
