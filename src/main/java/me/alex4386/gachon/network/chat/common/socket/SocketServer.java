package me.alex4386.gachon.network.chat.common.socket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class SocketServer {
    public int port;

    public List<SocketEvent> events = new ArrayList<>();

    public ServerSocket socket;
    private SocketServerThread thread;

    public SocketServer(int port) {
        this.port = port;
    }

    public void connect() throws IOException {
        this.socket = new ServerSocket(this.port);
        this.thread = new SocketServerThread(this);
        this.thread.start();
    }

    public void addEventListener(SocketEvent event) {
        events.add(event);
    }

    public void sendMessage(Socket socket, String msg) throws IOException {
        if (socket.isBound()) {
            OutputStream out = socket.getOutputStream();
            DataOutputStream dout = new DataOutputStream(out);

            dout.writeUTF(msg);
            dout.flush();

            for (SocketEvent event : this.events) {
                event.onMessageTransmit(socket, msg);
            }
        }
    }
}

class SocketServerThread extends Thread {
    SocketServer server;
    public List<SocketServerSocketThread> socketThreads = new ArrayList<>();

    public SocketServerThread(SocketServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = this.server.socket.accept();

                for (SocketEvent event : this.server.events) {
                    event.onIncomingConnection(socket);
                }

                Thread thread = new SocketServerSocketThread(server, socket);
                thread.start();

            } catch(SocketException e) {
                e.printStackTrace();
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}


class SocketServerSocketThread extends Thread {
    SocketServer server;
    Socket socket;

    public SocketServerSocketThread(SocketServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        while (true) {
            try {
                InputStream in = socket.getInputStream();
                DataInputStream din = new DataInputStream(in);

                String input = din.readUTF();

                for (SocketEvent event : this.server.events) {
                    event.onMessageReceive(socket, input);
                }
            } catch(EOFException e) {
                e.printStackTrace();
                break;
            } catch(SocketException e) {
                e.printStackTrace();
                break;
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}