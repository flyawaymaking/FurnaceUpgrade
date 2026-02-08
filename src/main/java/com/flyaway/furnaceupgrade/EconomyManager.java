package com.flyaway.furnaceupgrade;

import org.bukkit.entity.Player;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

public class EconomyManager {
    private final FurnaceUpgrade plugin;
    private final String currencyName;
    private Currency currency;

    public EconomyManager(FurnaceUpgrade plugin) {
        this.plugin = plugin;
        this.currencyName = plugin.getConfig().getString("economy.currency", "money");
        this.currency = CoinsEngineAPI.getCurrency(currencyName);

        if (isEconomyAvailable()) {
            plugin.getLogger().info("Успешно подключена валюта: " + currency.getName());
        } else {
            plugin.getLogger().warning("Валюта '" + currencyName + "' не найдена в CoinsEngine!");
        }
    }

    public String getCurrencyName() {
        return isEconomyAvailable() ? currency.getName() : currencyName;
    }

    public boolean hasEnoughMoney(Player player, double amount) {
        if (!isEconomyAvailable()) {
            plugin.sendMessage(player, plugin.getMessage("economy-disable").replace("{currency}", currencyName));
            return false;
        }

        try {
            double balance = CoinsEngineAPI.getBalance(player, currency);
            return balance >= amount;
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при проверке баланса: " + e.getMessage());
            return false;
        }
    }

    public boolean withdrawMoney(Player player, double amount) {
        if (!isEconomyAvailable()) {
            plugin.sendMessage(player, plugin.getMessage("economy-disable").replace("{currency}", currencyName));
            return false;
        }

        try {
            if (!hasEnoughMoney(player, amount)) {
                return false;
            }

            CoinsEngineAPI.removeBalance(player, currency, amount);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при списании денег: " + e.getMessage());
            return false;
        }
    }

    public int getUpgradeCost(int currentLevel) {
        double baseCost = plugin.getConfig().getInt("economy.base-cost", 10);
        double costMultiplier = plugin.getConfig().getDouble("economy.cost-multiplier", 1.5);
        return (int) (baseCost * Math.pow(costMultiplier, currentLevel));
    }

    public String getCurrencySymbol() {
        return isEconomyAvailable() ? currency.getSymbol() : "";
    }

    public boolean isEconomyAvailable() {
        return currency != null;
    }

    public void reload() {
        this.currency = CoinsEngineAPI.getCurrency(currencyName);
    }
}
