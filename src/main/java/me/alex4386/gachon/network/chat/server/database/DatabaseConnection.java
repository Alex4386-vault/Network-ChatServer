package me.alex4386.gachon.network.chat.server.database;

import java.net.URI;
import java.sql.*;
import java.util.List;

public class DatabaseConnection {
    public Connection connection;

    public DatabaseConnection(String host, int port, String username, String password, String database) throws SQLException {
        // setting it to Japan Standard Time since Korean Standard Time is too complicated for JAVA, duh.
        this.connection = DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/"+database+"?serverTimezone=JST", username, password);
    }

    public Object executeQuery(String query) {
        try {
            Statement statement = this.connection.createStatement();

            Object obj;

            if (query.toLowerCase().indexOf("select") >= 0 ) {
                obj = statement.executeQuery(query);
            } else if (query.toLowerCase().indexOf("insert") >= 0 || query.toLowerCase().indexOf("update") >= 0) {
                obj = statement.executeUpdate(query);
            } else {
                obj = statement.execute(query);
            }

            return obj;
        } catch (SQLException e) {
            return null;
        }
    }

    public Object executeQuery(String query, Object ... data) {

        System.out.println("[DB] processing query "+query);
        try {
            PreparedStatement statement = this.connection.prepareStatement(query,
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);

            for (int i = 1; i <= data.length; i++) {
                Object thisData = data[i-1];
                statement.setObject(i, thisData);
            }

            Object obj;
            String sql;

            if (query.toLowerCase().indexOf("select") >= 0 ) {

                System.out.println("[DB] identified query type select for query "+query);
                obj = statement.executeQuery();

                ResultSet rs = ((ResultSet) obj);

                int rowCount = rs.last() ? rs.getRow() : 0;
                rs.first();

                System.out.println("[DB] This was actually executed: "+rs.getStatement().toString()+" and has "+rowCount+" rows");
            } else if (query.toLowerCase().indexOf("insert") >= 0 || query.toLowerCase().indexOf("update") >= 0) {

                System.out.println("[DB] identified query type insert/update for query "+query);
                obj = statement.executeUpdate();
            } else {
                System.out.println("[DB] identified query type none for query "+query);

                System.out.println(query.toLowerCase());
                obj = statement.execute();
            }

            System.out.println("[DB] query "+query+" process complete");
            return obj;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
