package me.alex4386.gachon.network.chat.server;

import me.alex4386.gachon.network.chat.server.authorization.UserObject;
import me.alex4386.gachon.network.chat.server.database.DatabaseConnection;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class Main {

    // if macOS homebrew installation, start mariadb with `brew services start mariadb`
    public static DatabaseConnection database;
    public static List<UserObject> loggedInUsers = new ArrayList<UserObject>();
    public static int webserverPort = 8080;
    public static int socketPort = 8081;


    public static UserObject findByToken(String token) {
        for (UserObject user : loggedInUsers) {
            if (user.getToken().equals(token)) {
                return user;
            }
        }

        return null;
    }

    public static void main(String[] args) throws SQLException, IOException {
        database = new DatabaseConnection("localhost", 13306, "alex4386", "f@k3_p@ssw0rd!", "network_chat");
        WebAPIServer.main(webserverPort);
        SocketAPIServer.main(socketPort);

    }
}
