package me.alex4386.gachon.network.chat.common;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

// used some functions from my toy-projects
public class ChatUtils {

    public static String getDateHeader() {
        SimpleDateFormat f = new SimpleDateFormat("[hh:mm:ss]");
        return f.format(new Date());
    }

    public static String formatLog(String msg) {
        return getDateHeader()+" "+msg;
    }

    public static void log(String msg) {
        System.out.println(
                formatLog(msg)
        );
    }

    public static String readFile(File file) throws IOException {
        BufferedReader bufferedReader = new java.io.BufferedReader(new java.io.FileReader(file));

        String str = "";
        String tmp;
        while ((tmp = bufferedReader.readLine()) != null) {
            str += tmp+"\n";
        }

        return str;
    }

    public static JSONObject parseJSON(File file) throws IOException {
        return parseJSON(readFile(file));
    }

    public static JSONObject parseJSON(String string) {
        JSONTokener tokener = new JSONTokener(string);
        return new JSONObject(tokener);
    }

    public static void writeJSON(File file, JSONObject jsonObject) throws java.io.IOException {
        writeString(file, jsonObject.toString());
    }

    public static void writeString(File file, String string) throws java.io.IOException {
        if (!file.exists()) file.createNewFile();

        FileWriter writer = new java.io.FileWriter(file);
        writer.write(string);
        writer.close();
    }

}
