
package com.teny.autoseller.gui;

import com.teny.autoseller.AutoSellerPlugin;
import com.teny.autoseller.data.PlayerDataManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {
    private final AutoSellerPlugin plugin;
    public GuiListener(AutoSellerPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        if (title == null) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (title.contains("Скупщик - Меню")) {
            e.setCancelled(true);
            if (clicked.getType() == Material.BARRIER) {
                plugin.getGuiManager().sellAll(p, null);
                p.closeInventory();
            } else {
                // категории
                ConfigurationSection cats = plugin.getConfig().getConfigurationSection("categories");
                for (String key : cats.getKeys(false)) {
                    int slot = cats.getInt(key + ".slot");
                    if (e.getSlot() == slot) {
                        plugin.getGuiManager().openCategory(p, key);
                        return;
                    }
                }
                if (e.getSlot() == 20) plugin.getGuiManager().openAutoSellMenu(p);
                if (e.getSlot() == 22) plugin.getGuiManager().openBoosterShop(p);
                if (e.getSlot() == 24) { plugin.getGuiManager().sellAll(p, null); p.closeInventory(); }
            }
        } else if (title.contains("| Скупка")) {
            e.setCancelled(true);
            if (clicked.getType() == Material.ARROW) {
                plugin.getGuiManager().openMainMenu(p);
                return;
            }
            boolean all = e.isShiftClick();
            plugin.getGuiManager().sellSingle(p, clicked.getType(), all);
            // обновить GUI
            String cat = null;
            ConfigurationSection cats = plugin.getConfig().getConfigurationSection("categories");
            for (String k : cats.getKeys(false)) {
                String cName = plugin.getGuiManager().color(cats.getString(k + ".name",""));
                if (title.contains(cName) || title.contains(k)) {
                    // костыль: найдем по материалу
                    ConfigurationSection items = cats.getConfigurationSection(k + ".items");
                    if (items != null && items.contains(clicked.getType().name())) { cat = k; break; }
                }
            }
            // переоткрыть чтобы обновить счетчик
            if (cat != null) {
                // не закрываем, просто перерисовываем позже
            }
        } else if (title.contains("Автоскупка")) {
            e.setCancelled(true);
            PlayerDataManager.PlayerData data = plugin.getDataManager().get(p);
            if (e.getSlot() == 4) {
                data.autoSellEnabled = !data.autoSellEnabled;
                p.sendMessage(plugin.getGuiManager().color(data.autoSellEnabled ? plugin.getConfig().getString("messages.autosell-enabled") : plugin.getConfig().getString("messages.autosell-disabled")));
                plugin.getGuiManager().openAutoSellMenu(p);
            } else if (e.getSlot() == 6) {
                data.filterWhitelistMode = !data.filterWhitelistMode;
                plugin.getGuiManager().openAutoSellMenu(p);
            } else if (e.getSlot() == 53) {
                plugin.getGuiManager().openMainMenu(p);
            } else if (e.getSlot() >= 9) {
                Material m = clicked.getType();
                if (m == Material.AIR) return;
                String name = m.name();
                if (data.autoSellFilter.contains(name)) data.autoSellFilter.remove(name);
                else data.autoSellFilter.add(name);
                plugin.getGuiManager().openAutoSellMenu(p);
            }
            plugin.getDataManager().saveAll();
        } else if (title.contains("Магазин бустеров")) {
            e.setCancelled(true);
            if (e.getSlot() == 26) { plugin.getGuiManager().openMainMenu(p); return; }
            ConfigurationSection cats = plugin.getConfig().getConfigurationSection("categories");
            int idx = 0;
            for (String key : cats.getKeys(false)) {
                if (e.getSlot() == 11 + idx) {
                    int price = plugin.getConfig().getInt("boosters.price-points");
                    PlayerDataManager.PlayerData data = plugin.getDataManager().get(p);
                    if (plugin.getDataManager().hasActiveBooster(p, key)) {
                        p.sendMessage(plugin.getGuiManager().color(plugin.getConfig().getString("messages.booster-active").replace("%category%", key)));
                        return;
                    }
                    if (data.points < price) {
                        String msg = plugin.getConfig().getString("messages.not-enough-points").replace("%need%", String.valueOf(price)).replace("%have%", String.valueOf(data.points));
                        p.sendMessage(plugin.getGuiManager().color(msg));
                        return;
                    }
                    data.points -= price;
                    long expire = System.currentTimeMillis() + plugin.getConfig().getInt("boosters.duration-minutes") * 60L * 1000L;
                    data.boosters.put(key, expire);
                    String msg = plugin.getConfig().getString("messages.booster-bought").replace("%category%", key).replace("%time%", String.valueOf(plugin.getConfig().getInt("boosters.duration-minutes")));
                    p.sendMessage(plugin.getGuiManager().color(msg));
                    plugin.getGuiManager().openBoosterShop(p);
                    plugin.getDataManager().saveAll();
                    return;
                }
                idx++;
            }
        }
    }
}
