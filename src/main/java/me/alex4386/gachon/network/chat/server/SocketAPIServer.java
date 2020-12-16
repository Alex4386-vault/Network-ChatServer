package me.alex4386.gachon.network.chat.server;

import me.alex4386.gachon.network.chat.common.ChatUtils;
import me.alex4386.gachon.network.chat.server.authorization.UserObject;
import me.alex4386.gachon.network.chat.common.socket.SocketEvent;
import me.alex4386.gachon.network.chat.common.socket.SocketPacket;
import me.alex4386.gachon.network.chat.common.socket.SocketServer;

import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.*;

public class SocketAPIServer {
    public static Map<String, Socket> tokenSocket = new HashMap<>();
    public static Map<Socket, UUID> loginSocket = new HashMap<>();
    public static Map<Socket, UUID> chatSocket = new HashMap<>();

    public static Socket findSocketByToken(String token) {
        return tokenSocket.get(token);
    }

    public static String findTokenBySocket(Socket socket) {
        for (Map.Entry<String, Socket> entry : tokenSocket.entrySet()) {
            if (entry.getValue().equals(socket)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static List<Socket> findSocketsByUUID(UUID uuid) {
        List<Socket> sockets = new ArrayList<>();
        for (Map.Entry<Socket, UUID> entry : loginSocket.entrySet()) {
            if (entry.getValue().equals(uuid)) {
                sockets.add(entry.getKey());
            }
        }
        return sockets;
    }

    public static List<Socket> findSocketsByChat(UUID uuid) {
        List<Socket> sockets = new ArrayList<>();
        for (Map.Entry<Socket, UUID> entry : chatSocket.entrySet()) {
            if (entry.getValue().equals(uuid)) {
                sockets.add(entry.getKey());
            }
        }
        return sockets;
    }

    public static void main(int port) throws IOException {
        SocketServer server = new SocketServer(port);

        server.addEventListener(new SocketEvent() {
            @Override
            public void onIncomingConnection(Socket socket) {
                ChatUtils.log("Incoming connection from "+socket.getInetAddress()+".");
            }

            @Override
            public void onOutgoingConnection(Socket socket) {
                ChatUtils.log("Connecting to "+socket.getInetAddress()+".");
            }

            @Override
            public void onMessageReceive(Socket socket, String msg) {
                ChatUtils.log("Retrieved "+msg+" from "+socket.getInetAddress());
                SocketPacket packet = new SocketPacket(ChatUtils.parseJSON(msg));

                if (packet.isLogin() || packet.isJoin()) {
                    String token = packet.getToken();
                    UserObject user = Main.findByToken(token);

                    if (user == null) {
                        try {
                            server.sendMessage(socket, SocketPacket.buildError("Invalid Token").generateJSON());
                        } catch (IOException e) {}
                        return;
                    }

                    if (packet.isLogin()) {
                        loginSocket.remove(socket);
                        chatSocket.remove(socket);

                        tokenSocket.put(token, socket);
                        loginSocket.put(socket, UUID.fromString(user.getUuid()));

                        user.updateLastAction();
                    } else if (packet.isJoin()) {
                        String chat = packet.getChatUUID();
                        UUID chatUUID = UUID.fromString(chat);

                        if (user.getChats().contains(chatUUID)) {
                            List<Socket> sockets = findSocketsByChat(chatUUID);
                            for (Socket thisSocket: sockets) {
                                try { server.sendMessage(thisSocket, msg); } catch(Exception e) {}
                            }
                        } else {
                            try { server.sendMessage(socket, SocketPacket.buildError("Not enough privilege").generateJSON()); }
                            catch(IOException e) {}
                        }

                        tokenSocket.put(token, socket);
                        chatSocket.put(socket, chatUUID);

                        user.updateLastAction();
                    }
                } else if (packet.isMessage() || packet.isWhisper()) {
                    UUID chatUUID = chatSocket.get(socket);
                    UUID senderUUID = UUID.fromString(findTokenBySocket(socket));

                    try {
                        UserObject user = UserObject.findUser(Main.database, senderUUID);
                        user.updateLastAction();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }

                    if (chatUUID == null) {
                        try { server.sendMessage(socket, SocketPacket.buildError("You didn't joined any chat").generateJSON()); }
                        catch(IOException e) {}
                    } else {
                        List<Socket> sockets = findSocketsByChat(chatUUID);
                        for (Socket thisSocket: sockets) {
                            try { server.sendMessage(thisSocket, msg); } catch(Exception e) {}
                        }
                    }
                } else if (packet.isUpdate()) {
                    try {
                        UUID uuid = loginSocket.get(socket);
                        UserObject user = UserObject.findUser(Main.database, uuid);

                        user.updateLastAction();

                        List<UUID> friends = user.getFriendsList();
                        for (UUID friend:friends) {
                            List<Socket> friendSockets = findSocketsByUUID(friend);
                            for (Socket friendSocket : friendSockets) {
                                try { server.sendMessage(friendSocket, msg); } catch(Exception e) {}
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onMessageTransmit(Socket socket, String msg) {
                ChatUtils.log("Transmitted "+msg+" to "+socket.getInetAddress());
            }
        });

        server.connect();
    }
}
