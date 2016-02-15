package us.tryy3.minatsu.plugins.minatusminecraftalerts;

import us.tryy3.java.minatsu.Bot;
import us.tryy3.java.minatsu.command.CommandManager;
import us.tryy3.java.minatsu.plugins.Plugin;
import us.tryy3.java.minatsu.plugins.PluginDescription.DescriptionBuilder;
import us.tryy3.minatsu.plugins.minatsupermissions.MinatsuPermissions;
import us.tryy3.minatsu.plugins.minatsupermissions.PermissionsApi;
import us.tryy3.minatsu.plugins.minatusminecraftalerts.commands.CommandHandler;

import java.io.File;

/**
 * Created by tryy3 on 2016-02-15.
 */
public class Alerts extends Plugin {
    PermissionsApi api;
    MinecraftTCP minecraftTCP;
    @Override
    public void init(Bot bot, File pluginDir) {
        DescriptionBuilder builder = new DescriptionBuilder("MinatsuMinecraftAlerts", "0.0.1");

        builder.authors("tryy3");
        builder.dependency("MinatsuPermissions");
        builder.description("Get alerts about minecraft server status.");
        super.init(bot, pluginDir, builder.build());
    }

    @Override
    public void onStart() {
        super.onStart();

        CommandManager manager = getBot().getCommandManager();

        Bot bot = getBot();
        if (bot.getPluginManager().getPlugin("MinatsuPermissions") == null || !(bot.getPluginManager().getPlugin("MinatsuPermissions") instanceof MinatsuPermissions)) {
            this.unload();
            throw new Error("Can't find MinatsuPermissions");
        }
        this.api = ((MinatsuPermissions) bot.getPluginManager().getPlugin("MinatsuPermissions")).getPermissionsApi();

        minecraftTCP = new MinecraftTCP(6767, this);
        minecraftTCP.start();

        manager.registerCommand(new CommandHandler("minatsuminecraftalert", this));
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public PermissionsApi getPerms() {
        return api;
    }

    public MinecraftTCP getTcp() {
        return minecraftTCP;
    }
}
