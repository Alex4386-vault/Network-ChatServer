package me.alex4386.gachon.network.chat.common.socket;

import org.json.JSONObject;

public class SocketPacket {
    public SocketActions action;
    public JSONObject data;

    public SocketPacket() {}

    public SocketPacket(JSONObject packet) {
        this.action = SocketActions.getAction(packet.getString("action"));
        this.data = packet.getJSONObject("data");
    }


    public static SocketPacket buildMessage(String chatUUID, String sender, String message) {
        SocketPacket packet = new SocketPacket();

        packet.action = SocketActions.MESSAGE;
        packet.data = new JSONObject();

        JSONObject poststamp = new JSONObject();
        poststamp.put("sender", sender);

        packet.data.put("postStamp", poststamp);
        packet.data.put("message", message);
        packet.data.put("chat", chatUUID);

        return packet;
    }

    public static SocketPacket buildError(String error) {
        SocketPacket packet = new SocketPacket();

        packet.action = SocketActions.ERROR;
        packet.data = new JSONObject();
        packet.data.put("error", error);

        return packet;
    }

    public boolean isLogin() {
        return this.action == SocketActions.LOGIN;
    }

    public boolean isJoin() {
        return this.action == SocketActions.JOIN;
    }

    public boolean isMessage() { return this.action == SocketActions.MESSAGE; }

    public boolean isUpdate() { return this.action == SocketActions.UPDATE; }

    public boolean isWhisper() {
        return this.action == SocketActions.WHISPER;
    }

    public boolean isError() {
        return this.action == SocketActions.ERROR;
    }

    public String getToken() {
        return this.data.getString("token");
    }

    public String getChatUUID() {
        return this.data.getString("chat");
    }

    public String generateJSON() {
        JSONObject object = new JSONObject();
        object.put("action", this.action.string);
        object.put("data", this.data);

        return object.toString();
    }

}
