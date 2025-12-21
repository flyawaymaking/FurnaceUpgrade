package com.flyaway.furnaceupgrade;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.*;

public class EventListener implements Listener {
    private final FurnaceUpgrade plugin;
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Map<UUID, Block> lastClickedBlock = new HashMap<>();

    public EventListener(FurnaceUpgrade plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        Recipe recipe = event.getRecipe();

        if (recipe == null) return;

        ItemStack result = recipe.getResult();
        if (result.getType() != Material.BLAST_FURNACE && result.getType() != Material.SMOKER) return;

        FurnaceManager furnaceManager = plugin.getFurnaceManager();

        int maxLevel = 0;

        for (ItemStack ingredient : inventory.getMatrix()) {
            if (ingredient != null && ingredient.getType() == Material.FURNACE) {
                int level = furnaceManager.getFurnaceLevelFromItem(ingredient);
                if (level > maxLevel) {
                    maxLevel = level;
                }
            }
        }

        if (maxLevel > 0) {
            ItemStack upgraded = result.clone();
            furnaceManager.setFurnaceLevelToItem(upgraded, maxLevel);
            inventory.setResult(upgraded);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Block block = event.getBlock();
        if (!isFurnace(block.getType())) return;

        ItemStack item = event.getItemInHand();
        int level = plugin.getFurnaceManager().getFurnaceLevelFromItem(item);

        plugin.getFurnaceManager().setFurnaceLevel(block, level);
        plugin.getFurnaceManager().updateFurnaceDisplay(block);

        if (block.getState() instanceof Furnace furnace) {
            double speedMultiplier = plugin.getUpgradeManager().getSpeedMultiplier(level);
            furnace.setCookSpeedMultiplier(speedMultiplier);
            furnace.update();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        if (!isFurnace(block.getType())) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        FurnaceManager manager = plugin.getFurnaceManager();
        int level = manager.getFurnaceLevel(block);
        if (level < 0) return;

        World world = block.getWorld();
        Location centerLoc = block.getLocation().add(0.5, 0.5, 0.5);

        BlockState state = block.getState();
        if (!(state instanceof Furnace furnace)) return;

        ItemStack[] furnaceContents = {
                furnace.getSnapshotInventory().getItem(0), // Сырье
                furnace.getSnapshotInventory().getItem(1), // Топливо
                furnace.getSnapshotInventory().getItem(2)  // Результат
        };

        int expToDrop = event.getExpToDrop();

        event.setDropItems(false);
        event.setExpToDrop(0);

        ItemStack furnaceDrop = new ItemStack(block.getType());
        manager.setFurnaceLevelToItem(furnaceDrop, level);
        world.dropItemNaturally(centerLoc, furnaceDrop);

        for (ItemStack item : furnaceContents) {
            if (item != null && !item.getType().isAir()) {
                world.dropItemNaturally(centerLoc, item);
            }
        }

        if (expToDrop > 0) {
            world.spawn(centerLoc, ExperienceOrb.class, orb -> orb.setExperience(expToDrop));
        }
    }

    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (event.isCancelled()) return;
        Block block = event.getBlock();
        if (!isFurnace(block.getType())) return;

        int level = plugin.getFurnaceManager().getFurnaceLevel(block);
        double fuelMultiplier = plugin.getUpgradeManager().getFuelMultiplier(level);
        double speedMultiplier = plugin.getUpgradeManager().getSpeedMultiplier(level);

        // Раз увеличиваем скорость переплавки, то для пропорциональности надо уменьшать время горения топлива
        int newBurnTime = (int) Math.ceil(fuelMultiplier * event.getBurnTime() / speedMultiplier);
        event.setBurnTime(newBurnTime);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getItem() != null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (!event.getPlayer().isSneaking()) return;

        Block block = event.getClickedBlock();
        if (!isFurnace(block.getType())) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        FurnaceManager furnaceManager = plugin.getFurnaceManager();

        if (!furnaceManager.hasFurnaceData(block)) {
            furnaceManager.setFurnaceLevel(block, 0);
        }

        long currentTime = System.currentTimeMillis();
        long lastTime = lastClickTime.getOrDefault(playerId, 0L);
        Block lastBlock = lastClickedBlock.get(playerId);

        // Проверяем двойное нажатие в течение 5 секунд на тот же блок
        if (lastBlock != null && lastBlock.equals(block) && (currentTime - lastTime) < 5000) {
            // Второе нажатие - улучшение
            attemptUpgrade(player, block);
            lastClickTime.remove(playerId);
            lastClickedBlock.remove(playerId);
        } else {
            // Первое нажатие - показываем информацию
            showUpgradeInfo(player, block);
            lastClickTime.put(playerId, currentTime);
            lastClickedBlock.put(playerId, block);

            // Очищаем через 5 секунд
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (lastClickedBlock.get(playerId) == block &&
                        lastClickTime.getOrDefault(playerId, 0L) == currentTime) {
                    lastClickTime.remove(playerId);
                    lastClickedBlock.remove(playerId);
                }
            }, 200L); // 5 секунды = 200 тиков
        }

        event.setCancelled(true);
    }

    private void showUpgradeInfo(Player player, Block block) {
        FurnaceManager furnaceManager = plugin.getFurnaceManager();
        UpgradeManager upgradeManager = plugin.getUpgradeManager();

        int currentLevel = furnaceManager.getFurnaceLevel(block);
        UpgradeInfo nextUpgrade = upgradeManager.getUpgradeInfo(currentLevel + 1);

        if (nextUpgrade == null) {
            plugin.sendMessage(player, plugin.getMessage("max-level-reached"));
            return;
        }

        double speed = upgradeManager.getSpeedMultiplier(currentLevel + 1);
        double fuel = upgradeManager.getFuelMultiplier(currentLevel + 1);
        String currencySymbol = plugin.getEconomyManager().getCurrencySymbol();
        String furnaceName = plugin.getFurnaceName(block.getType());

        StringBuilder upgradeMessage = new StringBuilder(plugin.getMessage("upgrade-possible"));
        upgradeMessage.append("\n").append(plugin.getMessage("upgrade-level")
                        .replace("{furnace}", furnaceName)
                        .replace("{level}", String.valueOf(currentLevel + 1))
                        .replace("{level-name}", nextUpgrade.getName())
                        .replace("{speed}", String.format("%.2f", speed))
                        .replace("{fuel}", String.format("%.2f", fuel))
                        .replace("{price}", nextUpgrade.getCost() + currencySymbol))
                .append("\n").append(plugin.getMessage("click-again-to-upgrade"));

        plugin.sendMessage(player, upgradeMessage.toString());
    }

    private void attemptUpgrade(Player player, Block block) {
        FurnaceManager furnaceManager = plugin.getFurnaceManager();
        EconomyManager economyManager = plugin.getEconomyManager();
        UpgradeManager upgradeManager = plugin.getUpgradeManager();

        int currentLevel = furnaceManager.getFurnaceLevel(block);
        UpgradeInfo nextUpgrade = upgradeManager.getUpgradeInfo(currentLevel + 1);

        if (nextUpgrade == null) {
            plugin.sendMessage(player, plugin.getMessage("max-level-reached"));
            return;
        }

        double upgradeCost = nextUpgrade.getCost();

        if (!economyManager.hasEnoughMoney(player, upgradeCost)) {
            String notEnoughMoneyMessage = plugin.getMessage("not-enough-money")
                    .replace("{cost}", String.format("%.2f", upgradeCost))
                    .replace("{currency}", economyManager.getCurrencySymbol());
            plugin.sendMessage(player, notEnoughMoneyMessage);
            return;
        }

        if (economyManager.withdrawMoney(player, upgradeCost)) {
            int newLevel = currentLevel + 1;
            furnaceManager.setFurnaceLevel(block, newLevel);

            if (block.getState() instanceof Furnace furnace) {
                double speedMultiplier = upgradeManager.getSpeedMultiplier(newLevel);
                furnace.setCookSpeedMultiplier(speedMultiplier);
                furnace.update();
            }

            furnaceManager.updateFurnaceDisplay(block);
            String furnaceName = plugin.getFurnaceName(block.getType());

            String successMessage = plugin.getMessage("upgrade-successful")
                    .replace("{furnace}", furnaceName)
                    .replace("{level}", String.valueOf(newLevel))
                    .replace("{level-name}", nextUpgrade.getName());
            plugin.sendMessage(player, successMessage);

            playUpgradeEffects(player, block);
        }
    }

    private boolean isFurnace(Material material) {
        return material == Material.FURNACE || material == Material.BLAST_FURNACE || material == Material.SMOKER;
    }

    private void playUpgradeEffects(Player player, Block block) {
        String soundName = plugin.getConfig().getString("visual.upgrade-sound", "minecraft:entity.player.levelup");

        NamespacedKey soundKey = NamespacedKey.fromString(soundName);
        if (soundKey == null) {
            plugin.getLogger().warning("Неверный sound key: " + soundName);
            return;
        }

        Sound sound = Registry.SOUNDS.get(soundKey);
        if (sound == null) {
            plugin.getLogger().warning("Звук не найден в реестре: " + soundName);
            return;
        }

        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);

        String effectName = plugin.getConfig().getString("visual.upgrade-effect", "minecraft:happy_villager");

        NamespacedKey effectKey = NamespacedKey.fromString(effectName);
        if (effectKey == null) {
            plugin.getLogger().warning("Invalid particle key: " + effectName);
            return;
        }

        Particle particle = Registry.PARTICLE_TYPE.get(effectKey);
        if (particle == null) {
            plugin.getLogger().warning("Unknown particle: " + effectName);
            return;
        }

        block.getWorld().spawnParticle(particle, block.getLocation().add(0.5, 1.0, 0.5), 20, 0.5, 0.5, 0.5, 0.0);
    }
}
