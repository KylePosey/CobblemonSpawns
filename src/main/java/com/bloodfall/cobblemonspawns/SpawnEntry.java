package com.bloodfall.cobblemonspawns;

public class SpawnEntry {
    private final String pokemonName;
    private final int minLevel;
    private final int maxLevel;
    private final double spawnRate;

    public SpawnEntry(String pokemonName, int minLevel, int maxLevel, double spawnRate) {
        this.pokemonName = pokemonName;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.spawnRate = spawnRate;
    }

    public String getPokemonName() {
        return pokemonName;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public double getSpawnRate() {
        return spawnRate;
    }
}
