package com.bloodfall.cobblemonspawns;

public class CobblemonSpawnsConfig {
    private String cobblemonName;
    private double spawnRate; // Probability of spawning (0.0 - 1.0)
    private int minLevel;
    private int maxLevel;

    public CobblemonSpawnsConfig(String cobblemonName, double spawnRate, int minLevel, int maxLevel) {
        this.cobblemonName = cobblemonName;
        this.spawnRate = spawnRate;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    // Getters and Setters
    public String getCobblemonName() {
        return cobblemonName;
    }

    public void setCobblemonName(String cobblemonName) {
        this.cobblemonName = cobblemonName;
    }

    public double getSpawnRate() {
        return spawnRate;
    }

    public void setSpawnRate(double spawnRate) {
        this.spawnRate = spawnRate;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }
}
