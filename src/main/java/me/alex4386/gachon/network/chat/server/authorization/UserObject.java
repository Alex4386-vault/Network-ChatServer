package me.alex4386.gachon.network.chat.server.authorization;

import me.alex4386.gachon.network.chat.server.database.DatabaseConnection;
import org.json.JSONObject;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.xml.transform.Result;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UserObject {
    private DatabaseConnection conn;

    private String uuid;
    private String username;
    private String hashedPassword;
    private String token;

    private long lastActionMilliSeconds;

    public static int tokenLength = 64;
    public static String safeTokenChars = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";

    UserObject(DatabaseConnection conn, String uuid, String username, String hashedPassword) {
        this.conn = conn;
        this.uuid = uuid;
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.token = generateToken();

        this.lastActionMilliSeconds = System.currentTimeMillis();
    }

    public void updateLastAction() {
        this.lastActionMilliSeconds = System.currentTimeMillis();
    }

    public String getToken() {
        return this.token;
    }

    public String getUuid() {
        return this.uuid;
    }

    public String getUsername() {
        return this.username;
    }

    public ResultSet getRow() {
        return (ResultSet) this.conn.executeQuery("SELECT * FROM users WHERE uuid=?", this.uuid);
    }

    public ResultSet getFriends() {
        return (ResultSet) this.conn.executeQuery("SELECT * FROM friends WHERE uuid=?", this.uuid);
    }

    public List<UUID> getFriendsList() {
        List<UUID> friendsList = new ArrayList<>();

        ResultSet rs = getFriends();


        try {
            rs.beforeFirst();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("friend_uuid"));
                friendsList.add(uuid);
            }
        } catch (SQLException throwables) {
        }

        return friendsList;
    }

    public Date getLoginAt() {
        try {
            return this.getRow().getDate("login_at");
        } catch (SQLException throwables) {
            return null;
        }
    }

    public boolean setLoginAt() {
        return setLoginAt(new Date());
    }

    public boolean setLoginAt(Date date) {
        return this.conn.executeQuery("UPDATE users SET login_at=? WHERE uuid=?", date, uuid) != null;
    }

    public String getBio() {
        try {
            return this.getRow().getString("bio");
        } catch (SQLException throwables) {
            return "";
        }
    }

    public boolean setBio(String bio) {
        return this.conn.executeQuery("UPDATE users SET bio=? WHERE uuid=?", bio, uuid) != null;
    }

    public String getEmail() {
        try {
            return this.getRow().getString("email");
        } catch (SQLException throwables) {
            return "";
        }
    }

    public boolean setEmail(String email) {
        return this.conn.executeQuery("UPDATE users SET email=? WHERE uuid=?", email, uuid) != null;
    }

    public Date getBirthday() {
        try {
            return this.getRow().getDate("birthday");
        } catch (SQLException throwables) {
            return null;
        }
    }

    public boolean setBirthday(Date birthday) {
        return this.conn.executeQuery("UPDATE users SET birthday=? WHERE uuid=?", birthday, uuid) != null;
    }

    public String getNickname() {
        try {
            return this.getRow().getString("nickname");
        } catch (SQLException throwables) {
            return "";
        }
    }

    public boolean setNickname(String nickname) {
        return this.conn.executeQuery("UPDATE users SET nickname=? WHERE uuid=?", nickname, uuid) != null;
    }

    public String getRealName() {
        try {
            return this.getRow().getString("realname");
        } catch (SQLException throwables) {
            return "";
        }
    }

    public boolean setRealName(String realname) {
        return this.conn.executeQuery("UPDATE users SET realname=? WHERE uuid=?", realname, uuid) != null;
    }

    public List<UUID> getChats() {
        List<UUID> chatUUIDs = new ArrayList<>();
        ResultSet rs = (ResultSet) this.conn.executeQuery("SELECT * FROM chats WHERE user_uuid=?", uuid);

        try {
            rs.beforeFirst();
            while (rs.next()) {
                chatUUIDs.add(UUID.fromString(rs.getString("uuid")));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return chatUUIDs;
    }

    public static String generateToken() {
        SecureRandom secureRandom = new SecureRandom();

        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < tokenLength; i++) {
            int idx = secureRandom.nextInt(safeTokenChars.length());
            strBuilder.append(safeTokenChars.charAt(idx));
        }

        return strBuilder.toString();
    }

    public static String hashPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        Base64.Encoder encoder = Base64.getEncoder();

        String encodedSalt = encoder.encodeToString(salt);

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = factory.generateSecret(spec).getEncoded();

        String hashString = encoder.encodeToString(hash);

        return encodedSalt+"$$"+hashString;
    }

    public static boolean verifyPassword(String hashedPassword, String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String[] parsed = hashedPassword.split("\\$\\$");

        Base64.Decoder decoder = Base64.getDecoder();
        byte[] salt = decoder.decode(parsed[0]);
        byte[] hash = decoder.decode(parsed[1]);

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] passwordHash = factory.generateSecret(spec).getEncoded();

        for (int i = 0; i < passwordHash.length; i++) {
            if (hash[i] != passwordHash[i]) {
                return false;
            }
        }
        return true;
    }

    public static UserObject findUser(DatabaseConnection conn, String username) throws SQLException {
        ResultSet rs = (ResultSet) conn.executeQuery("SELECT * FROM users WHERE username=?", username);

        int rowCount = rs.last() ? rs.getRow() : 0;
        rs.first();
        if (rowCount == 0) {
            return null;
        } else {
            String uuid = rs.getString("uuid");
            String hashedPassword = rs.getString("password");
            return new UserObject(conn, uuid, username, hashedPassword);
        }
    }

    public static UserObject findUser(DatabaseConnection conn, UUID uuid) throws SQLException {
        ResultSet rs = (ResultSet) conn.executeQuery("SELECT * FROM users WHERE uuid=?", uuid.toString());

        int rowCount = rs.last() ? rs.getRow() : 0;
        rs.first();
        if (rowCount == 0) {
            return null;
        } else {
            String username = rs.getString("username");
            String hashedPassword = rs.getString("password");
            return new UserObject(conn, uuid.toString(), username, hashedPassword);
        }
    }

    public static UserObject authorizeUser(DatabaseConnection conn, String username, String password) throws SQLException {
        UserObject user = findUser(conn, username);

        try {
            if (verifyPassword(user.hashedPassword, password)) {
                return user;
            } else {
                return null;
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONObject toSafeJSON() {
        JSONObject json = new JSONObject();
        json.put("username", this.username);
        json.put("uuid", this.uuid);
        json.put("bio", this.getBio());
        json.put("nickname", this.getNickname());
        json.put("realname", this.getRealName());
        json.put("email", this.getEmail());
        json.put("birthday", this.getBirthday());
        json.put("lastAction", this.lastActionMilliSeconds);

        return json;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("token", this.token);
        json.put("username", this.username);
        json.put("uuid", this.uuid);
        json.put("bio", this.getBio());
        json.put("nickname", this.getNickname());
        json.put("realname", this.getRealName());
        json.put("email", this.getEmail());
        json.put("birthday", this.getBirthday());
        json.put("lastAction", this.lastActionMilliSeconds);

        return json;
    }
}
