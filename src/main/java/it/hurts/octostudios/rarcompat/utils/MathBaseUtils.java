package it.hurts.octostudios.rarcompat.utils;

import net.minecraft.util.RandomSource;

public class MathBaseUtils {

    public static int multicast(RandomSource random, double chance, double chanceMultiplier) {
        return random.nextDouble() <= chance ? multicast(random, chance * chanceMultiplier, chanceMultiplier) + 1 : 0;
    }

    public static int multicast(RandomSource random, double chance, int maxIterations) {
        int count = 0;

        while (count < maxIterations && random.nextDouble() <= chance)
            count++;

        return count;
    }

    public static int multicast(RandomSource random, double chance) {
        return multicast(random, chance, 100);
    }
}

