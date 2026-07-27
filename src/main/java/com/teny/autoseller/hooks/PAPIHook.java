
package com.teny.autoseller.hooks;

import com.teny.autoseller.AutoSellerPlugin;
import com.teny.autoseller.data.PlayerDataManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PAPIHook extends PlaceholderExpansion {
    private final AutoSellerPlugin plugin;
    public PAPIHook(AutoSellerPlugin plugin) { this.plugin = plugin; }

    @Override public String getIdentifier() { return "autoseller"; }
    @Override public String getAuthor() { return "Teny"; }
    @Override public String getVersion() { return "1.1.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params.equalsIgnoreCase("points")) {
            if (player == null || !player.isOnline()) return "0";
            return String.valueOf(plugin.getDataManager().get(player.getPlayer()).points);
        }
        if (params.equalsIgnoreCase("points_formatted")) {
            if (player == null || !player.isOnline()) return "0";
            int pts = plugin.getDataManager().get(player.getPlayer()).points;
            return String.format("%,d", pts).replace(',', ' ');
        }
        if (params.startsWith("booster_")) {
            // booster_ores, booster_mobdrops, booster_crops
            String cat = params.replace("booster_", "");
            if (player == null || !player.isOnline()) return "§cнет";
            boolean active = plugin.getDataManager().hasActiveBooster(player.getPlayer(), cat);
            if (!active) return "§7нет";
            long exp = plugin.getDataManager().get(player.getPlayer()).boosters.get(cat);
            long left = (exp - System.currentTimeMillis()) / 1000;
            return "§aактивен " + (left/60) + "м " + (left%60) + "с";
        }
        if (params.equalsIgnoreCase("autosell_status")) {
            if (player == null || !player.isOnline()) return "выкл";
            boolean en = plugin.getDataManager().get(player.getPlayer()).autoSellEnabled;
            return en ? "§aвкл" : "§cвыкл";
        }
        // ТОП плейсхолдеры: top_1_name, top_1_points, top_2_name и т.д. до 10
        if (params.startsWith("top_")) {
            try {
                String[] split = params.split("_");
                int pos = Integer.parseInt(split[1]); // 1-10
                String type = split[2]; // name or points
                List<Map.Entry<UUID, PlayerDataManager.PlayerData>> top = plugin.getDataManager().getTop(10);
                if (pos < 1 || pos > top.size()) return type.equals("name") ? "---" : "0";
                Map.Entry<UUID, PlayerDataManager.PlayerData> entry = top.get(pos-1);
                OfflinePlayer off = Bukkit.getOfflinePlayer(entry.getKey());
                String name = off.getName() != null ? off.getName() : "Unknown";
                if (type.equalsIgnoreCase("name")) return name;
                if (type.equalsIgnoreCase("points")) return String.valueOf(entry.getValue().points);
            } catch (Exception e) { return ""; }
        }
        return null;
    }
}
