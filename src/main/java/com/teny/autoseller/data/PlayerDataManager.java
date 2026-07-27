
package com.teny.autoseller.data;

import com.teny.autoseller.AutoSellerPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerDataManager {
    private final AutoSellerPlugin plugin;
    private final File file;
    private FileConfiguration cfg;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(AutoSellerPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    public static class PlayerData {
        public int points = 0;
        public boolean autoSellEnabled = false;
        public Set<String> autoSellFilter = new HashSet<>();
        public Map<String, Long> boosters = new HashMap<>();
        public boolean filterWhitelistMode = true;
    }

    private void loadAll() {
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerData d = new PlayerData();
                d.points = cfg.getInt(key + ".points");
                d.autoSellEnabled = cfg.getBoolean(key + ".autosell.enabled");
                d.filterWhitelistMode = cfg.getBoolean(key + ".autosell.whitelist", true);
                List<String> list = cfg.getStringList(key + ".autosell.filter");
                d.autoSellFilter = new HashSet<>(list);
                if (cfg.getConfigurationSection(key + ".boosters") != null) {
                    for (String cat : cfg.getConfigurationSection(key + ".boosters").getKeys(false)) {
                        d.boosters.put(cat, cfg.getLong(key + ".boosters." + cat));
                    }
                }
                cache.put(uuid, d);
            } catch (Exception ignored) {}
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, PlayerData> e : cache.entrySet()) {
            String base = e.getKey().toString();
            PlayerData d = e.getValue();
            cfg.set(base + ".points", d.points);
            cfg.set(base + ".autosell.enabled", d.autoSellEnabled);
            cfg.set(base + ".autosell.whitelist", d.filterWhitelistMode);
            cfg.set(base + ".autosell.filter", new ArrayList<>(d.autoSellFilter));
            cfg.set(base + ".boosters", null);
            for (Map.Entry<String, Long> b : d.boosters.entrySet()) {
                cfg.set(base + ".boosters." + b.getKey(), b.getValue());
            }
        }
        try { cfg.save(file); } catch (IOException ex) { ex.printStackTrace(); }
    }

    public PlayerData get(Player p) {
        return cache.computeIfAbsent(p.getUniqueId(), k -> new PlayerData());
    }

    public double getBoosterMultiplier(Player p, String category) {
        PlayerData d = get(p);
        Long exp = d.boosters.get(category);
        if (exp == null) return 1.0;
        if (System.currentTimeMillis() > exp) {
            d.boosters.remove(category);
            return 1.0;
        }
        return plugin.getConfig().getDouble("boosters.multiplier", 2.0);
    }

    public boolean hasActiveBooster(Player p, String category) {
        return getBoosterMultiplier(p, category) > 1.0;
    }

    public List<Map.Entry<UUID, PlayerData>> getTop(int limit) {
        return cache.entrySet().stream()
                .sorted((a,b) -> Integer.compare(b.getValue().points, a.getValue().points))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
