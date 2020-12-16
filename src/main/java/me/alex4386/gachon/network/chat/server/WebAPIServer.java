package me.alex4386.gachon.network.chat.server;

import express.Express;
import express.http.request.Request;
import express.http.response.Response;
import express.utils.MediaType;
import express.utils.Status;
import me.alex4386.gachon.network.chat.server.authorization.UserObject;
import me.alex4386.gachon.network.chat.server.database.DatabaseConnection;
import me.alex4386.gachon.network.chat.common.http.HttpResponseCode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class WebAPIServer {

    public static UserObject getCredentials(DatabaseConnection conn, Request request) {
        if (request.hasAuthorization()) {
            List<String> authorizations = request.getHeader("Authorization");

            for (String authorization : authorizations) {
                if (authorization.startsWith("Bearer")) {
                    // Fallback.
                    String token = authorization.split(" ")[1];

                    for (UserObject loggedInUser : Main.loggedInUsers) {
                        if (loggedInUser.getToken().equals(token)) {
                            loggedInUser.setLoginAt();
                            return loggedInUser;
                        }
                    }

                } else if (authorization.startsWith("Basic")) {
                    // Fallback.
                    String credentials = authorization.split(" ")[1];

                    Base64.Decoder decoder = Base64.getDecoder();
                    String[] rawCredentials = new String(decoder.decode(credentials)).split(":");

                    String username = rawCredentials[0].toLowerCase();
                    String password = rawCredentials[1];

                    try {
                        UserObject user = UserObject.authorizeUser(conn, username, password);
                        user.setLoginAt();
                        return user;
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }
        }

        return null;
    }

    public static void main(int port) throws SQLException {
        Express app = new Express();

        app.use((req, res) -> {
            res.setContentType(MediaType._json);
        });

        app.get("/", (req, res) -> {
            JSONObject json = new JSONObject();
            json.put("hello", "world");

            UserObject user = getCredentials(Main.database, req);
            if (user != null) {
                json.put("user", user.toJSON());
            }

            res.send(json.toString());
        });

        app.get("/profile", (req, res) -> {

            UserObject user = getCredentials(Main.database, req);
            if (user != null) {
                sendSuccess(res, user.toJSON());
            } else {
                res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                sendError(res, "You are not logged in");
            }
        });

        app.post("/profile", (req, res) -> {

            UserObject user = getCredentials(Main.database, req);

            if (user != null) {
                System.out.println("[Profile] user is valid.");

                String bio = null;
                String email = null;
                Date birthday = null;
                String realname = null;
                String nickname = null;

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

                try { bio = URLDecoder.decode(req.getQuery("bio")); } catch(Exception e) {}
                try { email = URLDecoder.decode(req.getQuery("email")); } catch(Exception e) {}
                try { birthday = df.parse(URLDecoder.decode(req.getQuery("birthday"))); } catch(Exception e) {}
                try { realname = URLDecoder.decode(req.getQuery("realname")); } catch(Exception e) {}
                try { nickname = URLDecoder.decode(req.getQuery("nickname")); } catch(Exception e) {}

                boolean success = true;
                if (bio != null) success = success && user.setBio(bio);
                if (email != null) success = success && user.setEmail(email);
                if (birthday != null) success = success && user.setBirthday(birthday);
                if (realname != null) success = success && user.setRealName(realname);
                if (nickname != null) success = success && user.setNickname(nickname);

                if (success) {
                    sendSuccess(res);
                } else {
                    sendError(res, "DB Error!");
                }
                return;
            } else {
                res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                sendError(res, "You are not logged in");
                return;
            }
        });


        app.get("/friends", (req, res) -> {

            UserObject user = getCredentials(Main.database, req);
            try {
                if (user != null) {
                    List<UUID> friendsUUID = user.getFriendsList();

                    JSONObject data = new JSONObject();
                    JSONArray array = new JSONArray();

                    for (UUID friendUUID : friendsUUID) {
                        UserObject friend = UserObject.findUser(Main.database, friendUUID);
                        array.put(friend.toSafeJSON());
                    }

                    data.put("friends", array);

                    sendSuccess(res, data);
                    return;
                } else {
                    res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                    sendError(res, "You are not logged in");
                    return;
                }
            } catch (SQLException e) {
                res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                sendError(res, "You are not logged in");
                return;
            }

        });


        app.get("/friends/:friendId", (req, res) -> {

            UserObject user = getCredentials(Main.database, req);
            try {
                if (user != null) {
                    List<UUID> friendsUUID = user.getFriendsList();

                    UUID friendUUID = UUID.fromString(req.getParam("friendId"));
                    boolean isFriend = friendsUUID.contains(friendUUID);

                    if (isFriend) {
                        UserObject friend = UserObject.findUser(Main.database, friendUUID);

                        sendSuccess(res, friend.toSafeJSON());
                    } else {
                        res.setStatus(Status.valueOf(HttpResponseCode.NOT_FOUND.getCode()));
                        sendError(res, "There is no such friend");
                        return;
                    }

                    return;
                } else {
                    res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                    sendError(res, "You are not logged in");
                    return;
                }
            } catch (SQLException e) {
                res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                sendError(res, "You are not logged in");
                return;
            }

        });

        app.delete("/friends/:friendId", (req, res) -> {

            UserObject user = getCredentials(Main.database, req);
            if (user != null) {
                List<UUID> friendsUUID = user.getFriendsList();

                UUID friendUUID = UUID.fromString(req.getParam("friendId"));
                boolean isFriend = friendsUUID.contains(friendUUID);

                if (isFriend) {
                    boolean dbRun = (boolean) Main.database.executeQuery("DELETE FROM users WHERE uuid=? AND friend_uuid=?", user.getUuid(), friendUUID.toString());
                    if (dbRun) {
                        sendSuccess(res);
                    } else {
                        res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                        sendError(res, "DB Error!");
                        return;
                    }
                } else {
                    res.setStatus(Status.valueOf(HttpResponseCode.NOT_FOUND.getCode()));
                    sendError(res, "There is no such friend");
                    return;
                }

                return;
            } else {
                res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                sendError(res, "You are not logged in");
                return;
            }

        });

        app.post("/auth/login", (req, res) -> {
            String username = req.getQuery("username").toLowerCase();
            String password = req.getQuery("password");

            UserObject user;

            try {
                user = UserObject.authorizeUser(Main.database, username, password);
            } catch (SQLException throwables) {
                res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                throwables.printStackTrace();
                sendError(res, "DB Error, BOI");
                return;
            }

            if (user == null) {
                res.setStatus(Status.valueOf(HttpResponseCode.NOT_FOUND.getCode()));
                sendError(res, "Invalid User");
                return;
            }

            JSONObject data = user.toJSON();

            Main.loggedInUsers.add(user);

            sendSuccess(res, data);
        });

        app.post("/auth/signup", (req, res) -> {
            String username = URLDecoder.decode(req.getQuery("username")).toLowerCase();
            String password = URLDecoder.decode(req.getQuery("password"));
            String birthday = URLDecoder.decode(req.getQuery("birthday"));
            String nickname = URLDecoder.decode(req.getQuery("nickname"));
            String realname = URLDecoder.decode(req.getQuery("realname"));
            String email = URLDecoder.decode(req.getQuery("email"));


            if (username.equals("")) {
                System.out.println("[User] invalid signup request found: Username");
                sendError(res, "Invalid Username!");
                return;
            }

            if (password.equals("")) {
                System.out.println("[User] invalid signup request found: Password");
                sendError(res, "Invalid Password!");
                return;
            }
            if (birthday.equals("")) {
                System.out.println("[User] invalid signup request found: Birthday");
                sendError(res, "Invalid Birthday!");
                return;
            }
            if (realname.equals("")) {
                System.out.println("[User] invalid signup request found: Nickname");
                sendError(res, "Invalid Nickname!");
                return;
            }
            if (realname.equals("")) {
                System.out.println("[User] invalid signup request found: Realname");
                sendError(res, "Invalid Realname!");
                return;
            }
            if (email.equals("")) {
                System.out.println("[User] invalid signup request found: E-mail");
                sendError(res, "Invalid e-mail!");
                return;
            }

            ResultSet rs = (ResultSet) Main.database.executeQuery("SELECT COUNT(1) AS users FROM users WHERE username=?", username);

            try {
                if (rs.getInt("users") != 0) {
                    res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                    sendError(res, "There's already a user, m8.");
                    return;
                }
            } catch (SQLException | NullPointerException e) {
                res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                System.out.println("[User] DB error was detected from "+username);
                sendError(res, "DB Error, BOI");
                e.printStackTrace();
                return;
            }


            try {
                System.out.println("[User] inserting "+username+" into Main.database");
                String hashString = UserObject.hashPassword(password);
                String uuid = UUID.randomUUID().toString();

                Object result = Main.database.executeQuery("INSERT INTO users (uuid, username, password, nickname, realname, email, birthday, bio) VALUES (?, ?, ?, ?, ?, ?, ?, '')", uuid, username, hashString, nickname, realname, email, birthday);

                if (result == null) {
                    res.setStatus(Status.valueOf(HttpResponseCode.BAD_REQUEST.getCode()));
                    sendError(res, "Invalid Birthday!");
                }

                System.out.println("[User] insert success");
                sendSuccess(res);

                return;
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                sendError(res, "Hashing Error!");

                e.printStackTrace();
                return;
            }
        });

        app.delete("/users/signup", (req, res) -> {
            UserObject user = getCredentials(Main.database, req);
            if (user != null) {
                boolean complete = (boolean) Main.database.executeQuery("DELETE FROM users WHERE uuid=?", user.getUuid());
                if (complete) {
                    Iterator<UserObject> iterator = Main.loggedInUsers.iterator();
                    while (iterator.hasNext()) {
                        UserObject thisUser = iterator.next();
                        if (thisUser.getUuid().equals(user.getUuid())) {
                            iterator.remove();
                        }
                    }

                    sendSuccess(res);
                    return;
                } else {
                    res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                    sendError(res, "Unknown Error");
                    return;
                }
            } else {
                res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                sendError(res, "You are not logged in");
                return;
            }
        });

        app.get("/users/:uuid", (req, res) -> {
            String uuidString = req.getParam("uuid");

            if (uuidString.equals("search")) {
                String username = req.getQuery("username");

                ResultSet rs = (ResultSet) Main.database.executeQuery("SELECT * FROM users WHERE username LIKE CONCAT('%', ?, '%')", username);
                JSONArray array = new JSONArray();

                try {
                    rs.beforeFirst();
                    while (rs.next()) {
                        String uuid = rs.getString("uuid");

                        UserObject user = UserObject.findUser(Main.database, UUID.fromString(uuid));

                        if (user != null) {
                            array.put(user.toSafeJSON());
                        }
                    }

                    JSONObject data = new JSONObject();
                    data.put("results", array);

                    sendSuccess(res, data);
                } catch (SQLException e) {
                    res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                    sendError(res, "DB Error!");

                    e.printStackTrace();
                    return;
                }
            } else {
                UUID uuid = UUID.fromString(uuidString);

                try {
                    UserObject user = UserObject.findUser(Main.database, uuid);

                    if (user != null) {
                        sendSuccess(res, user.toSafeJSON());
                        return;
                    } else {
                        res.setStatus(Status.valueOf(HttpResponseCode.NOT_FOUND.getCode()));
                        sendError(res, "Not Found");
                        return;
                    }

                } catch (SQLException e) {
                    res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                    sendError(res, "DB Error!");

                    e.printStackTrace();
                    return;
                }
            }
        });

        app.get("/users/:uuid/friend", (req, res) -> {

            UserObject user = getCredentials(Main.database, req);
            if (user != null) {
                String uuidString = req.getParam("uuid");
                UUID uuid = UUID.fromString(uuidString);

                try {
                    if (user.getFriendsList().contains(uuid)) {
                        res.setStatus(Status.valueOf(HttpResponseCode.BAD_REQUEST.getCode()));
                        sendError(res, "The friend already exists");

                        return;
                    }

                    UserObject friend = UserObject.findUser(Main.database, uuid);

                    if (friend != null) {
                        Object result = Main.database.executeQuery("INSERT INTO friends (uuid, friend_uuid) VALUES (?, ?)", user.getUuid(), friend.getUuid());

                        if (result != null) {
                            sendSuccess(res);
                        } else {
                            res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                            sendError(res, "DB Error!");

                            return;
                        }
                    } else {
                        res.setStatus(Status.valueOf(HttpResponseCode.NOT_FOUND.getCode()));
                        sendError(res, "Not Found");
                        return;
                    }
                } catch (SQLException e) {
                    res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                    sendError(res, "DB Error!");

                    e.printStackTrace();
                    return;
                }
            } else {
                res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                sendError(res, "You are not logged in");
                return;
            }
        });

        app.get("/chats", (req, res) -> {
            UserObject user = getCredentials(Main.database, req);
            if (user != null) {
                List<UUID> chatUUIDs = user.getChats();
                JSONArray chats = new JSONArray();

                for (UUID chatUUID: chatUUIDs) {
                    chats.put(chatUUID);
                }

                JSONObject data = new JSONObject();
                data.put("chats", chats);
                sendSuccess(res, data);
            } else {
                res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                sendError(res, "You are not logged in");
                return;
            }
        });

        app.post("/chats", (req, res) -> {
            UserObject user = getCredentials(Main.database, req);
            if (user != null) {
                UUID chatUUID = UUID.randomUUID();
                Object a = Main.database.executeQuery("INSERT INTO chats (uuid, user_uuid) VALUES (?, ?)", chatUUID.toString(), user.getUuid());

                if (a == null) {
                    res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                    sendError(res, "DB Error!");
                } else {
                    JSONObject data = new JSONObject();
                    data.put("uuid", chatUUID.toString());
                    sendSuccess(res, data);
                }
            } else {
                res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                sendError(res, "You are not logged in");
                return;
            }
        });

        app.get("/chats/:chatUUID", (req, res) -> {
            UUID chatUUID = UUID.fromString(req.getParam("chatUUID"));
            UserObject user = getCredentials(Main.database, req);

            if (user != null) {
                if (user.getChats().contains(chatUUID)) {
                    ResultSet rs = (ResultSet) Main.database.executeQuery("SELECT * FROM chats WHERE uuid=?", chatUUID.toString());
                    JSONArray members = new JSONArray();

                    try {
                        rs.beforeFirst();

                        while (rs.next()) {
                            UUID uuid = UUID.fromString(rs.getString("user_uuid"));
                            UserObject thisUser = UserObject.findUser(Main.database, uuid);
                            members.put(thisUser.toSafeJSON());
                        }

                        JSONObject data = new JSONObject();
                        data.put("members", members);

                        sendSuccess(res, data);
                        return;
                    } catch (SQLException e) {
                        res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                        sendError(res, "DB Error!");

                        e.printStackTrace();
                        return;
                    }
                } else {
                    res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                    sendError(res, "You are not part of this chat");
                    return;
                }
            } else {
                res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                sendError(res, "You are not logged in");
                return;
            }
        });

        app.get("/chats/:chatUUID/members", (req, res) -> {
            UUID chatUUID = UUID.fromString(req.getParam("chatUUID"));
            UserObject user = getCredentials(Main.database, req);

            if (user != null) {
                if (user.getChats().contains(chatUUID)) {
                    ResultSet rs = (ResultSet) Main.database.executeQuery("SELECT * FROM chats WHERE uuid=?", chatUUID, user.getUuid());
                    JSONArray members = new JSONArray();

                    try {
                        rs.beforeFirst();

                        while (rs.next()) {
                            UUID uuid = UUID.fromString(rs.getString("user_uuid"));
                            UserObject thisUser = UserObject.findUser(Main.database, uuid);
                            members.put(thisUser.toSafeJSON());
                        }

                        JSONObject data = new JSONObject();
                        data.put("members", members);

                        sendSuccess(res, data);
                        return;
                    } catch (SQLException e) {
                        res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                        sendError(res, "DB Error!");

                        e.printStackTrace();
                        return;
                    }
                } else {
                    res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                    sendError(res, "You are not part of this chat");
                    return;
                }
            } else {
                res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                sendError(res, "You are not logged in");
                return;
            }
        });

        app.post("/chats/:chatUUID/members", (req, res) -> {
            UUID chatUUID = UUID.fromString(req.getParam("chatUUID"));
            UserObject user = getCredentials(Main.database, req);

            UUID userUUID = UUID.fromString(req.getQuery("user"));

            if (user != null) {
                if (user.getChats().contains(chatUUID) && user.getFriendsList().contains(userUUID)) {
                    boolean target = (boolean) Main.database.executeQuery("INSERT INTO chats (uuid, user_uuid) VALUES (?, ?)", chatUUID.toString(), userUUID.toString());
                    if (target) {
                        sendSuccess(res);
                    } else {
                        res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                        sendError(res, "DB Error!");
                        return;
                    }
                } else {
                    res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                    sendError(res, "You are not part of this chat");
                    return;
                }
            } else {
                res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                sendError(res, "You are not logged in");
                return;
            }
        });


        app.delete("/chats/:chatUUID/members", (req, res) -> {
            UUID chatUUID = UUID.fromString(req.getParam("chatUUID"));
            UserObject user = getCredentials(Main.database, req);

            UUID userUUID = UUID.fromString(req.getQuery("user"));

            if (user != null) {
                if (user.getChats().contains(chatUUID)) {
                    boolean target = (boolean) Main.database.executeQuery("DELETE FROM chats WHERE uuid=? AND user_uuid=?", chatUUID.toString(), userUUID.toString());
                    if (target) {
                        sendSuccess(res);
                    } else {
                        res.setStatus(Status.valueOf(HttpResponseCode.INTERNAL_SERVER_ERROR.getCode()));
                        sendError(res, "DB Error!");
                        return;
                    }
                } else {
                    res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                    sendError(res, "You are not part of this chat");
                    return;
                }
            } else {
                res.setStatus(Status.valueOf(HttpResponseCode.FORBIDDEN.getCode()));
                sendError(res, "You are not logged in");
                return;
            }
        });

        app.listen(() -> {
            System.out.println("online!");
        }, port);
    }

    public static void sendSuccess(Response res) {
        JSONObject json = new JSONObject();
        json.put("success", true);

        res.send(json.toString());
    }

    public static void sendSuccess(Response res, JSONObject data) {
        JSONObject json = new JSONObject();
        json.put("success", true);
        json.put("data", data);

        res.send(json.toString());
    }

    public static void sendError(Response res, String errorMessage) {
        JSONObject json = new JSONObject();
        json.put("success", false);

        if (errorMessage != null) {
            json.put("error", errorMessage);
        }

        res.send(json.toString());
    }
}
