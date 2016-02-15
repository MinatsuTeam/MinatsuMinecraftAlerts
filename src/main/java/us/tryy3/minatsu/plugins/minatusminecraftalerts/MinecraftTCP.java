package us.tryy3.minatsu.plugins.minatusminecraftalerts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import us.tryy3.java.minatsu.TCPServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by tryy3 on 2016-02-15.
 */
public class MinecraftTCP extends Thread {
    private int port;
    private ServerSocket server;
    private Alerts alerts;
    private Map<UUID, Server> servers = new HashMap<>();

    public MinecraftTCP(int port, Alerts alerts) {
        this.alerts = alerts;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(port);

            alerts.getLogger().info("Waiting on connections from minecraft servers.");

            while (true) {
                Socket socket = server.accept();

                alerts.getLogger().info("Minecraft Server connection made from %s:%s", getIpAsString(socket.getInetAddress()), socket.getPort());

                UUID uuid = UUID.randomUUID();

                Server server = new Server(socket, uuid, this);
                servers.put(uuid, server);
                server.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getIpAsString(InetAddress address) {
        byte[] ipAddress = address.getAddress();
        StringBuffer str = new StringBuffer();
        for(int i=0; i<ipAddress.length; i++) {
            if(i > 0) str.append('.');
            str.append(ipAddress[i] & 0xFF);
        }
        return str.toString();
    }

    public Server getServerByName(String name) {
        for (Server server : servers.values()) {
            if (server.getName().equalsIgnoreCase(name)) return server;
        }
        return null;
    }

    public void remove(Server server) {
        this.servers.remove(server.getUuid());
    }

    public class Server extends Thread {
        private Socket socket;
        private UUID uuid;
        private BufferedReader in;
        private PrintWriter out;
        private MinecraftTCP parent;
        private String name;

        public Server(Socket socket, UUID uuid, MinecraftTCP parent) {
            this.socket = socket;
            this.uuid = uuid;
            this.parent = parent;

            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void run() {
            while (true) {
                try {
                    String msg = in.readLine();

                    JsonObject json = new JsonParser().parse(msg).getAsJsonObject();

                    if (json == null || !json.has("status")) {
                        alerts.getLogger().severe("Got a Request from a minecraft server, but the json was invalid.");
                        continue;
                    }

                    if (!json.has("messages")) {
                        alerts.getLogger().severe("Minecraft server tried sending a message, but no messages was sent.");
                        return;
                    }

                    if (json.get("status").getAsString().equalsIgnoreCase("connection")) {
                        this.name = (json.has("name")) ? json.get("name").getAsString() : "";
                    } else if (json.get("status").getAsString().equalsIgnoreCase("sendMessage")) {
                        if (!json.has("uuid")) {
                            alerts.getLogger().severe("Minecraft server tried sending a message, but invalid UUID");
                            break;
                        }

                        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
                        TCPServer.Connection connection = alerts.getBot().getTcpServer().getConnection(uuid);

                        if (connection == null) {
                            alerts.getLogger().severe("Minecraft server tried sending a message to invalid TCP client.");
                            return;
                        }

                        connection.sendMessage(json.getAsJsonArray("messages"));
                    }
                } catch (SocketException e) {
                    try {
                        in.close();
                        out.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    this.parent.remove(this);
                    this.interrupt();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getMinecraftName() {
            return name;
        }

        public void sendRaw(String message) {
            out.println(message);
        }

        public void sendMessage(JsonArray messages, UUID uuid) {
            JsonObject obj = new JsonObject();

            obj.addProperty("status", "get");
            obj.addProperty("uuid", "uuid");
            obj.add("messages", messages);
            sendRaw(obj.toString());
        }

        public void close() {
            try {
                this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
