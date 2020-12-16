package me.alex4386.gachon.network.chat.common.socket;

import java.net.Socket;

public abstract class SocketEvent extends SocketEventParent {
    public void onIncomingConnection(Socket socket) {}
    public void onOutgoingConnection(Socket socket) {}
    public void onMessageTransmit(Socket socket, String msg) {}
    public void onMessageReceive(Socket socket, String msg) {}
}

