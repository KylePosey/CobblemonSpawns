package com.bloodfall.cobblemonspawns;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

public class RandomUtils {
    private static final SecureRandom secureRandom = new SecureRandom();

    public static boolean shouldSpawnSecure(double chance) {
        if (chance < 0 || chance > 1) {
            throw new IllegalArgumentException("Chance must be between 0.0 and 1.0");
        }
        double roll = secureRandom.nextDouble();
        //System.out.println("SecureRandom roll: " + roll + ", Chance: " + chance); // Debug
        return roll < chance;
    }
}
