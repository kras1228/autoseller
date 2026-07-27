
package com.teny.autoseller;

import com.teny.autoseller.data.PlayerDataManager;
import com.teny.autoseller.gui.GuiListener;
import com.teny.autoseller.gui.GuiManager;
import com.teny.autoseller.hooks.PAPIHook;
import com.teny.autoseller.task.AutoSellTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoSellerPlugin extends JavaPlugin {
    private static AutoSellerPlugin instance;
    private Economy economy;
    private PlayerDataManager dataManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        if (!setupEconomy()) {
            getLogger().severe("Vault не найден! Выключаю плагин.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.dataManager = new PlayerDataManager(this);
        this.guiManager = new GuiManager(this);
        getCommand("seller").setExecutor(new SellerCommand(this));
        getCommand("sellall").setExecutor(new SellerCommand(this));
        getCommand("sellertop").setExecutor(new SellerCommand(this));
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        new AutoSellTask(this).runTaskTimer(this, 20L, 20L * getConfig().getInt("autosell.check-interval-seconds", 3));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIHook(this).register();
            getLogger().info("PlaceholderAPI подключен! Доступны плейсхолдеры %autoseller_...%");
        }

        getLogger().info("AutoSeller v1.1.0 включен! Топ и PAPI активны.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) dataManager.saveAll();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static AutoSellerPlugin getInstance() { return instance; }
    public Economy getEconomy() { return economy; }
    public PlayerDataManager getDataManager() { return dataManager; }
    public GuiManager getGuiManager() { return guiManager; }
}
