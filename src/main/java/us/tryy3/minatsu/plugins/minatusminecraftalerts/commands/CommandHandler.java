package us.tryy3.minatsu.plugins.minatusminecraftalerts.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import us.tryy3.java.minatsu.TCPServer;
import us.tryy3.java.minatsu.command.Command;
import us.tryy3.minatsu.plugins.minatusminecraftalerts.Alerts;
import us.tryy3.minatsu.plugins.minatusminecraftalerts.MinecraftTCP;

import java.util.Arrays;

/**
 * Created by tryy3 on 2016-02-15.
 */
public class CommandHandler extends Command {
    Alerts alerts;

    public CommandHandler(String name, Alerts alerts) {
        super(name);
        this.alerts = alerts;

        super.setDescription("Check minecraft server statuses.");
        super.setUsage("alert [help]");
        super.setAliases(Arrays.asList("minecraftalert", "mma", "ma", "alert", "a"));
    }

    @Override
    public Boolean onCommand(TCPServer.Connection connection, String user, String channel, Command command, String label, String[] args) {
        if (args != null && args.length > 1) {
            MinecraftTCP.Server server = alerts.getTcp().getServerByName(args[1]);

            if (server == null) {
                connection.sendMessage(channel, "Server name is not a valid server.");
                return true;
            }

            switch (args[0]) {
                case "check":
                    connection.sendMessage(channel, "This server is currently online.");
                    return true;

                case "get" :
                    if (args.length == 2) {
                        JsonArray both = new JsonArray();

                        both.addAll(get(true, channel));
                        both.addAll(get(false, channel));

                        server.sendMessage(both, connection.getUuid());
                    } else if (args.length > 2) {
                        boolean side;
                        if (args[1].equalsIgnoreCase("front")) {
                            side = true;
                        } else if (args[1].equalsIgnoreCase("back")) {
                            side = false;
                        } else {
                            break;
                        }

                        if (args.length == 3) {
                            JsonArray front = get(side, channel);
                            server.sendMessage(front, connection.getUuid());
                            return true;
                        } else if (args.length == 4) {
                            switch (args[2]) {
                                case "tps":
                                case "cpu":
                                    JsonArray tps = new JsonArray();
                                    tps.add(getStatus(side, channel, 0));
                                    server.sendMessage(tps, connection.getUuid());
                                    return true;
                                case "mem":
                                    JsonArray mem = new JsonArray();
                                    mem.add(getStatus(side, channel, 1));
                                    server.sendMessage(mem, connection.getUuid());
                                    return true;
                                case "disk":
                                    JsonArray disk = new JsonArray();
                                    disk.add(getStatus(side, channel, 2));
                                    server.sendMessage(disk, connection.getUuid());
                                    return true;
                                case "uptime":
                                    JsonArray uptime = new JsonArray();
                                    uptime.add(getStatus(side, channel, 3));
                                    server.sendMessage(uptime, connection.getUuid());
                                    return true;
                            }
                        }
                    }
                    break;
            }
        }
        connection.sendMessage(channel, "Invalid arguments, please check the help page.");
        return true;
    }

    /*
        Get Codes
        0: TPS/CPU
        1: Memory
        2: Disk
        3: Uptime

        Side
        True: Front
        False: Backend
     */
    public JsonArray get(boolean side, String channel) {
        JsonArray array = new JsonArray();

        array.add(getStatus(side, channel, 0));
        array.add(getStatus(side, channel, 1));
        array.add(getStatus(side, channel, 2));
        array.add(getStatus(side, channel, 3));

        return array;
    }

    public JsonObject getStatus(boolean side, String channel, int status) {
        JsonObject obj = new JsonObject();
        obj.addProperty("side", side);
        obj.addProperty("get", status);
        obj.addProperty("channel", channel);
        return obj;
    }
}
