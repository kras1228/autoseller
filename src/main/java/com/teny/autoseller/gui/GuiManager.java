
package com.teny.autoseller.gui;

import com.teny.autoseller.AutoSellerPlugin;
import com.teny.autoseller.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class GuiManager {
    private final AutoSellerPlugin plugin;

    public GuiManager(AutoSellerPlugin plugin) { this.plugin = plugin; }

    public String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    public void openMainMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, color("&8&lСкупщик &7- Меню"));
        ConfigurationSection cats = plugin.getConfig().getConfigurationSection("categories");
        if (cats != null) {
            for (String key : cats.getKeys(false)) {
                String name = cats.getString(key + ".name");
                String iconStr = cats.getString(key + ".icon");
                int slot = cats.getInt(key + ".slot");
                Material mat = Material.matchMaterial(iconStr);
                if (mat == null) mat = Material.CHEST;
                ItemStack it = new ItemStack(mat);
                ItemMeta meta = it.getItemMeta();
                meta.setDisplayName(color(name));
                meta.setLore(Arrays.asList(color("&7Нажми чтобы открыть"), color("&7категорию: &f" + key)));
                it.setItemMeta(meta);
                inv.setItem(slot, it);
            }
        }
        // Кнопки
        inv.setItem(20, createItem(Material.HOPPER, "&eАвтоскупка", Arrays.asList("&7Включить/настроить","&7авто-продажу")));
        inv.setItem(22, createItem(Material.GOLD_INGOT, "&6Магазин бустеров", Arrays.asList("&7Потрать очки на x2")));
        inv.setItem(24, createItem(Material.BARRIER, "&cПродать все", Arrays.asList("&7Продает все из инвентаря")));
        p.openInventory(inv);
    }

    public void openCategory(Player p, String category) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("categories." + category);
        if (sec == null) return;
        String title = sec.getString("name", category);
        Inventory inv = Bukkit.createInventory(null, 54, color(title + " &8| Скупка"));
        ConfigurationSection items = sec.getConfigurationSection("items");
        if (items != null) {
            for (String matName : items.getKeys(false)) {
                Material mat = Material.matchMaterial(matName);
                if (mat == null) continue;
                double price = items.getDouble(matName + ".price");
                int points = items.getInt(matName + ".points");
                double mult = plugin.getDataManager().getBoosterMultiplier(p, category);
                ItemStack it = new ItemStack(mat);
                ItemMeta meta = it.getItemMeta();
                meta.setDisplayName(color("&f" + matName));
                List<String> lore = new ArrayList<>();
                lore.add(color("&7Цена: &6" + price + "$ &7x" + mult));
                lore.add(color("&7Очки: &e" + (int)(points * mult)));
                lore.add(color("&7Бустер: " + (mult > 1 ? "&aАктивен x" + mult : "&7нет")));
                lore.add(color(""));
                lore.add(color("&aЛКМ - продать 1 шт"));
                lore.add(color("&aShift+ЛКМ - продать все"));
                meta.setLore(lore);
                it.setItemMeta(meta);
                inv.addItem(it);
            }
        }
        inv.setItem(53, createItem(Material.ARROW, "&cНазад", null));
        p.openInventory(inv);
    }

    public void openAutoSellMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, color("&8Автоскупка - настройка"));
        PlayerDataManager.PlayerData data = plugin.getDataManager().get(p);
        ItemStack toggle = createItem(data.autoSellEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
                (data.autoSellEnabled ? "&aВключена" : "&cВыключена"),
                Arrays.asList(color("&7Клик - вкл/выкл")));
        inv.setItem(4, toggle);

        inv.setItem(6, createItem(Material.COMPARATOR, "&eРежим: " + (data.filterWhitelistMode ? "Только выбранные" : "Все кроме выбранных"),
                Arrays.asList("&7Клик - сменить")));

        // Показать все доступные ресурсы для выбора
        int slot = 9;
        ConfigurationSection cats = plugin.getConfig().getConfigurationSection("categories");
        for (String cat : cats.getKeys(false)) {
            ConfigurationSection items = cats.getConfigurationSection(cat + ".items");
            if (items == null) continue;
            for (String matName : items.getKeys(false)) {
                if (slot >= 53) break;
                Material mat = Material.matchMaterial(matName);
                if (mat == null) continue;
                boolean selected = data.autoSellFilter.contains(matName);
                ItemStack it = new ItemStack(mat);
                ItemMeta meta = it.getItemMeta();
                meta.setDisplayName(color((selected ? "&a&l✓ " : "&7") + matName));
                List<String> lore = new ArrayList<>();
                lore.add(color(selected ? "&aБудет авто-продаваться" : "&7Не продается авто"));
                lore.add(color("&7Клик - выбрать"));
                meta.setLore(lore);
                if (selected) meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                it.setItemMeta(meta);
                inv.setItem(slot++, it);
            }
        }
        inv.setItem(53, createItem(Material.ARROW, "&cНазад", null));
        p.openInventory(inv);
    }

    public void openBoosterShop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, color("&6&lМагазин бустеров"));
        ConfigurationSection cats = plugin.getConfig().getConfigurationSection("categories");
        int boosterPrice = plugin.getConfig().getInt("boosters.price-points", 150);
        int duration = plugin.getConfig().getInt("boosters.duration-minutes", 30);
        int slot = 11;
        for (String cat : cats.getKeys(false)) {
            String catName = cats.getString(cat + ".name");
            String icon = cats.getString(cat + ".icon");
            Material mat = Material.matchMaterial(icon);
            if (mat == null) mat = Material.EXPERIENCE_BOTTLE;
            boolean active = plugin.getDataManager().hasActiveBooster(p, cat);
            ItemStack it = new ItemStack(active ? Material.BARRIER : mat);
            ItemMeta meta = it.getItemMeta();
            meta.setDisplayName(color(catName + " &8[x2]"));
            List<String> lore = new ArrayList<>();
            lore.add(color("&7Цена: &e" + boosterPrice + " очков"));
            lore.add(color("&7Длительность: &f" + duration + " мин"));
            lore.add(color("&7У тебя: &e" + plugin.getDataManager().get(p).points + " очков"));
            if (active) {
                lore.add(color("&cУже активен!"));
            } else {
                lore.add(color("&aКлик - купить"));
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
            inv.setItem(slot++, it);
        }
        inv.setItem(26, createItem(Material.ARROW, "&cНазад", null));
        p.openInventory(inv);
    }

    

    public void toggleAutoSell(Player p, String type){
        // simple implementation: toggle main autosell
        com.teny.autoseller.data.PlayerDataManager.PlayerData data = plugin.getDataManager().get(p);
        data.autoSellEnabled = !data.autoSellEnabled;
        plugin.getDataManager().save(p);
        p.sendMessage(color("&aАвто-продажа " + (data.autoSellEnabled ? "включена" : "выключена")));
    }

    public void buyBooster(Player p, String boosterId){
        // boosterId format booster.30m etc - we map to category selection via slot in listener
        // For compatibility with old GuiListener calling buyBooster(p, "booster.30m")
        // We just open real shop logic: buy booster for first category as example
        // New logic: if boosterId contains category, use it, else try to parse from config
        String cat = boosterId;
        if(boosterId.startsWith("booster.")){
            // old calls from listener were with booster time, not category - ignore, buy for all
            p.sendMessage(color("&eБустеры покупаются через меню бустеров, выбери категорию"));
            return;
        }
        // Try to buy for category cat
        int price = plugin.getConfig().getInt("boosters.price-points", 150);
        int duration = plugin.getConfig().getInt("boosters.duration-minutes", 30);
        com.teny.autoseller.data.PlayerDataManager.PlayerData data = plugin.getDataManager().get(p);
        if(data.points < price){
            p.sendMessage(color("&cНе хватает очков! Нужно: "+price));
            return;
        }
        if(plugin.getDataManager().hasActiveBooster(p, cat)){
            p.sendMessage(color("&cБустер уже активен!"));
            return;
        }
        data.points -= price;
        plugin.getDataManager().setBooster(p, cat, System.currentTimeMillis() + duration*60L*1000L);
        p.sendMessage(color("&aКупил x2 бустер для "+cat+" на "+duration+" минут!"));
    }

    // overload for old listener that called buyBooster with time string and slot mapping
    public void buyBoosterOldSlot(Player p, int slot){
        String[] cats = plugin.getConfig().getConfigurationSection("categories").getKeys(false).toArray(new String[0]);
        int idx = slot - 11;
        if(idx <0 || idx >= cats.length) return;
        buyBooster(p, cats[idx]);
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (name != null) meta.setDisplayName(color(name));
        if (lore != null) meta.setLore(lore.stream().map(this::color).collect(Collectors.toList()));
        it.setItemMeta(meta);
        return it;
    }

    public void sellAll(Player p, String specificCategory) {
        PlayerDataManager.PlayerData data = plugin.getDataManager().get(p);
        double totalMoney = 0;
        int totalPoints = 0;
        int totalItems = 0;
        ConfigurationSection cats = plugin.getConfig().getConfigurationSection("categories");

        Map<Material, Double> priceMap = new HashMap<>();
        Map<Material, Integer> pointsMap = new HashMap<>();
        Map<Material, String> matToCat = new HashMap<>();

        for (String catKey : cats.getKeys(false)) {
            if (specificCategory != null && !specificCategory.equals(catKey)) continue;
            ConfigurationSection items = cats.getConfigurationSection(catKey + ".items");
            if (items == null) continue;
            for (String m : items.getKeys(false)) {
                Material mat = Material.matchMaterial(m);
                if (mat == null) continue;
                priceMap.put(mat, items.getDouble(m + ".price"));
                pointsMap.put(mat, items.getInt(m + ".points"));
                matToCat.put(mat, catKey);
            }
        }

        for (ItemStack invItem : p.getInventory().getContents()) {
            if (invItem == null || invItem.getType() == Material.AIR) continue;
            if (!priceMap.containsKey(invItem.getType())) continue;
            // если фильтр включен для autosell но тут ручной sellAll - продаем все
            String cat = matToCat.get(invItem.getType());
            double mult = plugin.getDataManager().getBoosterMultiplier(p, cat);
            double price = priceMap.get(invItem.getType()) * mult * invItem.getAmount();
            int points = (int) (pointsMap.get(invItem.getType()) * mult * invItem.getAmount());
            totalMoney += price;
            totalPoints += points;
            totalItems += invItem.getAmount();
            p.getInventory().remove(invItem);
        }

        if (totalItems == 0) {
            p.sendMessage(color(plugin.getConfig().getString("messages.no-items")));
            return;
        }
        plugin.getEconomy().depositPlayer(p, totalMoney);
        data.points += totalPoints;
        p.sendMessage(color("&aПродано предметов: &f" + totalItems + " &aна &6" + totalMoney + "$ &7и &e" + totalPoints + " очков"));
        plugin.getDataManager().saveAll();
    }


    public void showTop(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(color("&8&m-------&8[ &6&lТОП СКУПЩИКА &8]&m-------"));
        List<java.util.Map.Entry<java.util.UUID, com.teny.autoseller.data.PlayerDataManager.PlayerData>> top = plugin.getDataManager().getTop(10);
        if (top.isEmpty()) {
            sender.sendMessage(color("&7Пока никто ничего не продал"));
            return;
        }
        String format = plugin.getConfig().getString("placeholders.top-format", "&e#%pos% &f%player% &7- &e%points% очков");
        int pos = 1;
        for (java.util.Map.Entry<java.util.UUID, com.teny.autoseller.data.PlayerDataManager.PlayerData> e : top) {
            org.bukkit.OfflinePlayer off = org.bukkit.Bukkit.getOfflinePlayer(e.getKey());
            String name = off.getName() != null ? off.getName() : e.getKey().toString().substring(0,8);
            String line = format.replace("%pos%", String.valueOf(pos)).replace("%player%", name).replace("%points%", String.valueOf(e.getValue().points));
            sender.sendMessage(color(line));
            pos++;
        }
        sender.sendMessage(color("&8&m-------------------------------"));
    }

    public void sellSingle(Player p, Material mat, boolean all) {
        ConfigurationSection cats = plugin.getConfig().getConfigurationSection("categories");
        double basePrice = 0;
        int basePoints = 0;
        String foundCat = null;
        for (String cat : cats.getKeys(false)) {
            ConfigurationSection items = cats.getConfigurationSection(cat + ".items");
            if (items != null && items.contains(mat.name())) {
                basePrice = items.getDouble(mat.name() + ".price");
                basePoints = items.getInt(mat.name() + ".points");
                foundCat = cat;
                break;
            }
        }
        if (foundCat == null) return;

        int amount = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (it != null && it.getType() == mat) amount += it.getAmount();
        }
        if (amount == 0) return;
        int toSell = all ? amount : 1;
        // remove
        int remaining = toSell;
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack it = p.getInventory().getItem(i);
            if (it == null || it.getType() != mat) continue;
            if (it.getAmount() <= remaining) {
                remaining -= it.getAmount();
                p.getInventory().setItem(i, null);
            } else {
                it.setAmount(it.getAmount() - remaining);
                remaining = 0;
            }
            if (remaining <= 0) break;
        }

        double mult = plugin.getDataManager().getBoosterMultiplier(p, foundCat);
        double money = basePrice * mult * toSell;
        int points = (int) (basePoints * mult * toSell);

        plugin.getEconomy().depositPlayer(p, money);
        plugin.getDataManager().get(p).points += points;

        String msg = plugin.getConfig().getString("messages.sold")
                .replace("%amount%", String.valueOf(toSell))
                .replace("%item%", mat.name())
                .replace("%money%", String.format("%.2f", money))
                .replace("%points%", String.valueOf(points));
        p.sendMessage(color(msg));
    }
}
