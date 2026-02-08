package com.flyaway.furnaceupgrade;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.block.TileState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class FurnaceManager {
    private final FurnaceUpgrade plugin;
    private final NamespacedKey furnaceLevelKey;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public FurnaceManager(FurnaceUpgrade plugin) {
        this.plugin = plugin;
        this.furnaceLevelKey = new NamespacedKey(plugin, "furnaceLevel");
    }

    public int getFurnaceLevel(Block block) {
        if (!(block.getState() instanceof TileState tileState)) return 0;

        PersistentDataContainer data = tileState.getPersistentDataContainer();
        return data.getOrDefault(furnaceLevelKey, PersistentDataType.INTEGER, 0);
    }

    public void setFurnaceLevel(Block block, int level) {
        if (!(block.getState() instanceof TileState tileState)) return;

        PersistentDataContainer data = tileState.getPersistentDataContainer();
        data.set(furnaceLevelKey, PersistentDataType.INTEGER, level);
        tileState.update();
    }

    public boolean hasFurnaceData(Block block) {
        if (!(block.getState() instanceof TileState tileState)) return false;

        PersistentDataContainer data = tileState.getPersistentDataContainer();
        return data.has(furnaceLevelKey, PersistentDataType.INTEGER);
    }

    public int getFurnaceLevelFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.getOrDefault(furnaceLevelKey, PersistentDataType.INTEGER, 0);
    }

    public void setFurnaceLevelToItem(ItemStack item, int level) {
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();

        if (level > 0) {
            data.set(furnaceLevelKey, PersistentDataType.INTEGER, level);

            String levelName = plugin.getUpgradeManager().getUpgradeName(level);
            double speedMultiplier = plugin.getUpgradeManager().getSpeedMultiplier(level);
            double fuelMultiplier = plugin.getUpgradeManager().getFuelMultiplier(level);
            String furnaceName = plugin.getFurnaceName(item.getType());

            String displayName = plugin.getMessage("furnace-level-display")
                    .replace("{furnace}", furnaceName)
                    .replace("{level-name}", levelName).replace("{level}", String.valueOf(level));
            meta.displayName(miniMessage.deserialize(displayName));

            String loreTemplate = plugin.getMessage("item-level-lore")
                    .replace("{level}", String.valueOf(level))
                    .replace("{level-name}", levelName)
                    .replace("{speed}", String.format("%.2f", speedMultiplier))
                    .replace("{fuel}", String.format("%.2f", fuelMultiplier));

            String[] loreLines = loreTemplate.split("\n");
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(MiniMessage.miniMessage().deserialize(line));
            }

            meta.lore(lore);
        } else {
            data.remove(furnaceLevelKey);
            meta.lore(null);
            meta.displayName(null);
        }

        item.setItemMeta(meta);
    }

    public void updateFurnaceDisplay(Block block) {
        if (!(block.getState() instanceof Furnace furnace)) return;

        int level = getFurnaceLevel(block);
        String levelName = plugin.getUpgradeManager().getUpgradeName(level);
        String furnaceName = plugin.getFurnaceName(block.getType());

        furnace.customName(miniMessage.deserialize(plugin.getMessage("furnace-level-display")
                .replace("{furnace}", furnaceName)
                .replace("{level-name}", levelName)));
        furnace.update();
    }
}
