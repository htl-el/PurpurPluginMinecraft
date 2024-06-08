package eu.endermite.commandwhitelist.waterfall;

import eu.endermite.commandwhitelist.common.CWGroup;
import eu.endermite.commandwhitelist.common.ConfigCache;
import eu.endermite.commandwhitelist.common.commands.CWCommand;
import eu.endermite.commandwhitelist.waterfall.command.BungeeMainCommand;
import eu.endermite.commandwhitelist.waterfall.listeners.BungeeChatEventListener;
import eu.endermite.commandwhitelist.waterfall.listeners.BungeeTabcompleteListener;
import eu.endermite.commandwhitelist.waterfall.listeners.WaterfallDefineCommandsListener;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SimplePie;

import java.io.File;
import java.util.*;

public final class CommandWhitelistWaterfall extends Plugin {

    private static CommandWhitelistWaterfall plugin;
    private static ConfigCache configCache;
    private static BungeeAudiences audiences;

    @Override
    public void onEnable() {
        plugin = this;
        getLogger().info("Running on " + ChatColor.DARK_AQUA + getProxy().getName());
        loadConfig();
        audiences = BungeeAudiences.create(this);
        Metrics metrics = new Metrics(this, 8704);

        this.getProxy().getPluginManager().registerListener(this, new BungeeChatEventListener());
        try {
            Class.forName("io.github.waterfallmc.waterfall.event.ProxyDefineCommandsEvent");
            metrics.addCustomChart(new SimplePie("proxy", () -> "Waterfall"));
            this.getProxy().getPluginManager().registerListener(this, new WaterfallDefineCommandsListener());
        } catch (ClassNotFoundException e) {
            metrics.addCustomChart(new SimplePie("proxy", () -> "Bungee"));
            getLogger().severe("Bungee command completion blocker requires Waterfall other Waterfall fork.");
        }
        this.getProxy().getPluginManager().registerListener(this, new BungeeTabcompleteListener());
        getProxy().getPluginManager().registerCommand(this, new BungeeMainCommand("bcw"));


    }

    public static CommandWhitelistWaterfall getPlugin() {
        return plugin;
    }

    public static ConfigCache getConfigCache() {
        return configCache;
    }

    public static BungeeAudiences getAudiences() {
        return audiences;
    }

    public void loadConfig() {
        if (configCache == null)
            configCache = new ConfigCache(new File(getDataFolder(), "config.yml"), false, getLogger());
        else
            configCache.reloadConfig();
    }

    public void loadConfigAsync(CommandSender sender) {
        getProxy().getScheduler().runAsync(this, () -> {
            loadConfig();
            audiences.sender(sender).sendMessage(CWCommand.miniMessage.deserialize(CommandWhitelistWaterfall.getConfigCache().prefix + CommandWhitelistWaterfall.getConfigCache().config_reloaded));
        });
    }

    /**
     * @param player Bungee Player
     * @return commands available to the player
     */
    public static HashSet<String> getCommands(ProxiedPlayer player) {
        HashSet<String> commandList = new HashSet<>();
        HashMap<String, CWGroup> groups = configCache.getGroupList();
        for (Map.Entry<String, CWGroup> s : groups.entrySet()) {
            if (s.getKey().equalsIgnoreCase("default"))
                commandList.addAll(s.getValue().getCommands());
            else if (player.hasPermission(s.getValue().getPermission()))
                commandList.addAll(s.getValue().getCommands());
        }
        return commandList;
    }

    /**
     * @param player Bungee Player
     * @return subcommands unavailable for the player
     */
    public static HashSet<String> getSuggestions(ProxiedPlayer player) {
        HashMap<String, CWGroup> groups = configCache.getGroupList();
        HashSet<String> suggestionList = new HashSet<>();
        for (Map.Entry<String, CWGroup> s : groups.entrySet()) {
            if (s.getKey().equalsIgnoreCase("default"))
                suggestionList.addAll(s.getValue().getSubCommands());
            if (player.hasPermission(s.getValue().getPermission())) continue;
            suggestionList.addAll(s.getValue().getSubCommands());
        }
        return suggestionList;
    }

    /**
     * @return Command denied message. Will use custom if command exists in any group.
     */
    public static String getCommandDeniedMessage(String command) {
        String commandDeniedMessage = configCache.command_denied;
        HashMap<String, CWGroup> groups = configCache.getGroupList();
        for (CWGroup group : groups.values()) {
            if (group.getCommands().contains(command)) {
                if (group.getCommandDeniedMessage() == null || group.getCommandDeniedMessage().isEmpty()) continue;
                commandDeniedMessage = group.getCommandDeniedMessage();
                break; // get first message we find
            }
        }
        return commandDeniedMessage;
    }

    public static ArrayList<String> getServerCommands() {
        ArrayList<String> serverCommands = new ArrayList<>();
        for (Map.Entry<String, Command> command : CommandWhitelistWaterfall.getPlugin().getProxy().getPluginManager().getCommands()) {
            serverCommands.add(command.getValue().getName());
        }
        return serverCommands;
    }
}
