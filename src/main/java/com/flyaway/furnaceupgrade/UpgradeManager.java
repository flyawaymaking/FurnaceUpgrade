package com.flyaway.furnaceupgrade;

import org.bukkit.configuration.ConfigurationSection;

public class UpgradeManager {
    private final FurnaceUpgrade plugin;

    public UpgradeManager(FurnaceUpgrade plugin) {
        this.plugin = plugin;
    }

    public UpgradeInfo getUpgradeInfo(int level) {
        ConfigurationSection upgradesSection = plugin.getConfig().getConfigurationSection("upgrades");
        if (upgradesSection == null) return null;

        ConfigurationSection levelSection = upgradesSection.getConfigurationSection(String.valueOf(level));
        if (levelSection == null) return null;

        String name = levelSection.getString("name", "null");
        int cost = levelSection.getInt("cost", plugin.getEconomyManager().getUpgradeCost(level));

        return new UpgradeInfo(name, cost);
    }

    public String getUpgradeName(int level) {
        UpgradeInfo info = getUpgradeInfo(level);
        return info != null && info.getName() != null ? info.getName() : "lvl " + level;
    }

    public double getSpeedMultiplier(int level) {
        double baseMultiplier = plugin.getConfig().getDouble("base-speed-multiplier", 1.0);
        double speedMultiplierPerLevel = plugin.getConfig().getDouble("speed-multiplier-per-level", 0.5);
        return baseMultiplier + (level) * speedMultiplierPerLevel;
    }

    public double getFuelMultiplier(int level) {
        double baseMultiplier = plugin.getConfig().getDouble("base-fuel-consumption-multiplier", 1.0);
        double fuelMultiplierPerLevel = plugin.getConfig().getDouble("fuel-consumption-multiplier-per-level", 0.25);
        return baseMultiplier + (level) * fuelMultiplierPerLevel;
    }

    public int getMaxLevel() {
        ConfigurationSection upgradesSection = plugin.getConfig().getConfigurationSection("upgrades");
        return upgradesSection != null ? upgradesSection.getKeys(false).size() - 1 : 0;
    }
}
