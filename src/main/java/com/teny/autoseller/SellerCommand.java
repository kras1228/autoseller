
package com.teny.autoseller;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SellerCommand implements CommandExecutor {
    private final AutoSellerPlugin plugin;
    public SellerCommand(AutoSellerPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("seller")) {
            if (!(sender instanceof Player)) { sender.sendMessage("Only players"); return true; }
            plugin.getGuiManager().openMainMenu((Player) sender);
        } else if (cmd.getName().equalsIgnoreCase("sellall")) {
            if (!(sender instanceof Player)) return true;
            plugin.getGuiManager().sellAll((Player) sender, null);
        } else if (cmd.getName().equalsIgnoreCase("sellertop")) {
            plugin.getGuiManager().showTop(sender);
        }
        return true;
    }
}
