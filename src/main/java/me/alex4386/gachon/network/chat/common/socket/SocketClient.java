package me.alex4386.gachon.network.chat.common.socket;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SocketClient {
    public String destination;
    public int port;

    public List<SocketEvent> events = new ArrayList<>();

    public Socket socket;
    public SocketClientThread thread;

    public InputStream in = null;
    public DataInputStream din = null;

    public OutputStream out = null;
    public DataOutputStream dout = null;

    public SocketClient(String destination, int port) throws IOException {
        this.destination = destination;
        this.port = port;
    }

    public void connect() throws IOException {
        this.socket = new Socket(destination, port);
        this.thread = new SocketClientThread(this);
        thread.start();

        for (SocketEvent event : this.events) {
            event.onOutgoingConnection(socket);
        }
    }

    public void addEventListener(SocketEvent event) {
        events.add(event);
    }

    public void sendMessage(String msg) {
        if (this.socket.isConnected()) {
            try {
                if (out == null && dout == null) {
                    out = this.socket.getOutputStream();
                    dout = new DataOutputStream(out);
                }

                dout.writeUTF(msg);
                dout.flush();

                for (SocketEvent event : this.events) {
                    event.onMessageTransmit(socket, msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


class SocketClientThread extends Thread {
    SocketClient client;

    public SocketClientThread(SocketClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        while (true) {
            if (this.client.socket.isClosed()) {
                break;
            }

            try {
                Socket socket = this.client.socket;

                if (this.client.in == null && this.client.din == null) {
                    this.client.in = socket.getInputStream();
                    this.client.din = new DataInputStream(this.client.in);
                }

                String input = this.client.din.readUTF();

                for (SocketEvent event : this.client.events) {
                    event.onMessageReceive(socket, input);
                }
            } catch(EOFException e) {
            } catch(IOException e){
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
