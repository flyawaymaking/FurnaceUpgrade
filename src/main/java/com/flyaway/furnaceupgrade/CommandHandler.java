package com.flyaway.furnaceupgrade;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final FurnaceUpgrade plugin;

    public CommandHandler(FurnaceUpgrade plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("furnaceupgrade.reload")) {
                    plugin.sendMessage(sender, plugin.getMessage("no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getEconomyManager().reload();
                plugin.sendMessage(sender, plugin.getMessage("reload-successful"));
                break;

            case "info":
                if (!(sender instanceof Player)) {
                    plugin.sendMessage(sender, plugin.getMessage("only-player"));
                    return true;
                }
                sendUpgradeInfo((Player) sender);
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Просто предлагаем доступные команды
            if ("reload".startsWith(args[0].toLowerCase()) && sender.hasPermission("furnaceupgrade.reload")) {
                completions.add("reload");
            }
            if ("info".startsWith(args[0].toLowerCase()) && sender instanceof Player) {
                completions.add("info");
            }
        }

        return completions;
    }

    private void sendHelp(CommandSender sender) {
        plugin.sendMessage(sender, plugin.getMessage("help"));
    }

    private void sendUpgradeInfo(Player player) {
        UpgradeManager upgradeManager = plugin.getUpgradeManager();
        String currencySymbol = plugin.getEconomyManager().getCurrencySymbol();
        StringBuilder message = new StringBuilder(plugin.getMessage("upgrade-title"));

        for (int i = 0; i <= upgradeManager.getMaxLevel(); i++) {
            UpgradeInfo info = upgradeManager.getUpgradeInfo(i);
            if (info != null) {
                double speed = upgradeManager.getSpeedMultiplier(i);
                double fuel = upgradeManager.getFuelMultiplier(i);
                String price = i < upgradeManager.getMaxLevel() ? info.getCost() + currencySymbol : "Max";
                message.append("\n").append(plugin.getMessage("upgrade-level")
                        .replace("{level}", String.valueOf(i))
                        .replace("{level-name}", info.getName())
                        .replace("{speed}", String.format("%.2f", speed))
                        .replace("{fuel}", String.format("%.2f", fuel))
                        .replace("{price}", price));
            }
        }
        plugin.sendMessage(player, message.toString());
    }
}
