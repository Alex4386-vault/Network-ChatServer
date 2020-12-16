package me.alex4386.gachon.network.chat.common.socket;

public enum SocketActions {
    LOGIN("login"),
    JOIN("join"),
    UPDATE("update"),
    LEAVE("leave"),
    WHISPER("whisper"),
    MESSAGE("message"),
    ERROR("error");

    String string;

    SocketActions(String string) {
        this.string = string;
    }

    public static SocketActions getAction(String string) {
        for (SocketActions action:SocketActions.values()) {
            if (action.string.equals(string)) {
                return action;
            }
        }

        return null;
    }
}
