package me.alex4386.gachon.network.chat.common.socket;

import java.net.Socket;

public abstract class SocketEventParent {
    protected abstract void onIncomingConnection(Socket socket);
    protected abstract void onOutgoingConnection(Socket socket);
    protected abstract void onMessageTransmit(Socket socket, String msg);
    protected abstract void onMessageReceive(Socket socket, String msg);
}

