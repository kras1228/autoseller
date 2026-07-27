
package com.teny.autoseller.task;

import com.teny.autoseller.AutoSellerPlugin;
import com.teny.autoseller.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class AutoSellTask extends BukkitRunnable {
    private final AutoSellerPlugin plugin;
    public AutoSellTask(AutoSellerPlugin plugin) { this.plugin = plugin; }

    @Override
    public void run() {
        ConfigurationSection cats = plugin.getConfig().getConfigurationSection("categories");
        if (cats == null) return;
        Map<Material, Double> priceMap = new HashMap<>();
        Map<Material, Integer> pointsMap = new HashMap<>();
        Map<Material, String> matToCat = new HashMap<>();
        for (String cat : cats.getKeys(false)) {
            ConfigurationSection items = cats.getConfigurationSection(cat + ".items");
            if (items == null) continue;
            for (String m : items.getKeys(false)) {
                Material mat = Material.matchMaterial(m);
                if (mat == null) continue;
                priceMap.put(mat, items.getDouble(m + ".price"));
                pointsMap.put(mat, items.getInt(m + ".points"));
                matToCat.put(mat, cat);
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerDataManager.PlayerData data = plugin.getDataManager().get(p);
            if (!data.autoSellEnabled) continue;
            if (data.autoSellFilter.isEmpty() && data.filterWhitelistMode) continue; // ничего не выбрано

            double totalMoney = 0;
            int totalPoints = 0;
            int totalItems = 0;

            for (int i = 0; i < p.getInventory().getSize(); i++) {
                if (i == 40) continue; // offhand skip? можно продавать
                ItemStack it = p.getInventory().getItem(i);
                if (it == null || it.getType() == Material.AIR) continue;
                if (!priceMap.containsKey(it.getType())) continue;

                boolean inFilter = data.autoSellFilter.contains(it.getType().name());
                if (data.filterWhitelistMode && !inFilter) continue;
                if (!data.filterWhitelistMode && inFilter) continue;

                String cat = matToCat.get(it.getType());
                double mult = plugin.getDataManager().getBoosterMultiplier(p, cat);
                totalMoney += priceMap.get(it.getType()) * mult * it.getAmount();
                totalPoints += (int)(pointsMap.get(it.getType()) * mult * it.getAmount());
                totalItems += it.getAmount();
                p.getInventory().setItem(i, null);
            }

            if (totalItems > 0) {
                plugin.getEconomy().depositPlayer(p, totalMoney);
                data.points += totalPoints;
                p.sendMessage(plugin.getGuiManager().color("&8[&eАвтоскупка&8] &aПродано &f" + totalItems + " &aза &6" + String.format("%.2f", totalMoney) + "$ &7+ &e" + totalPoints + " очков"));
            }
        }
    }
}
