package com.flyaway.furnaceupgrade;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class FurnaceUpgrade extends JavaPlugin {
    private FurnaceManager furnaceManager;
    private EconomyManager economyManager;
    private UpgradeManager upgradeManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        // Сохраняем конфиг по умолчанию
        this.saveDefaultConfig();

        // Инициализируем менеджеры
        this.furnaceManager = new FurnaceManager(this);
        this.economyManager = new EconomyManager(this);
        this.upgradeManager = new UpgradeManager(this);
        CommandHandler commandHandler = new CommandHandler(this);

        // Регистрируем события и команды
        getCommand("furnaceupgrade").setExecutor(commandHandler);
        getCommand("furnaceupgrade").setTabCompleter(commandHandler);
        getServer().getPluginManager().registerEvents(new EventListener(this), this);

        getLogger().info("FurnaceUpgrade успешно запущен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("FurnaceUpgrade выключен!");
    }

    // Геттеры для менеджеров
    public FurnaceManager getFurnaceManager() {
        return furnaceManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public String getMessage(String key) {
        String message = getConfig().getString("messages." + key, "<red>message." + key + " not-found");
        return message.replaceAll("\\s+$", "");
    }

    public void sendMessage(CommandSender sender, String message) {
        String prefix = getConfig().getString("prefix", "<blue>FurnaceUpgrade | </blue>");
        sender.sendMessage(miniMessage.deserialize(prefix + " " + message));
    }

    public String getFurnaceName(Material material) {
        if (material == Material.FURNACE) {
            return getConfig().getString("furnace-name", "Печь");
        } else if (material == Material.BLAST_FURNACE) {
            return getConfig().getString("blast-furnace-name", "Плавильня");
        } else if (material == Material.SMOKER) {
            return getConfig().getString("smoker-name", "Коптильня");
        }
        return "not-furnace";
    }
}
