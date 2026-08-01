package com.flyaway.furnaceupgrade;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;
import su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency;

public class EconomyManager {
    private final FurnaceUpgrade plugin;
    private String currencyName;
    private ExcellentCurrency currency;
    private ExcellentEconomyAPI api;

    public EconomyManager(FurnaceUpgrade plugin) {
        this.plugin = plugin;
        loadApi();
    }

    private void loadApi() {
        this.currencyName = plugin.getConfig().getString("economy.currency", "money");

        RegisteredServiceProvider<ExcellentEconomyAPI> provider = Bukkit.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class);
        if (provider != null) {
            this.api = provider.getProvider();
            this.currency = api.getCurrency(currencyName);
        }

        if (isEconomyAvailable()) {
            plugin.getLogger().info("Успешно подключена валюта: " + currency.getName());
        } else {
            plugin.getLogger().warning("Валюта '" + currencyName + "' не найдена в ExcellentEconomy!");
        }
    }

    public boolean hasEnoughMoney(Player player, double amount) {
        if (!isEconomyAvailable()) {
            plugin.sendMessage(player, plugin.getMessage("economy-disable").replace("{currency}", currencyName));
            return false;
        }

        try {
            double balance = api.getBalance(player, currency);
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

            api.withdraw(player, currency, amount);
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
        loadApi();
    }
}
