package io.github.rephrasing.jsonsockets.server;

import com.google.gson.JsonElement;
import io.github.rephrasing.jsonsockets.core.JsonSocket;
import io.github.rephrasing.jsonsockets.thread.SocketThread;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * This class handles the server side of two applications
 */
public abstract class JsonServerSocket implements JsonSocket {

    private SocketThread serverThread;
    private ServerSocket socket;
    private Socket connection;

    private Thread sendingThread;

    public JsonServerSocket(int port, Integer backlog, InetAddress address, Integer setSoTimeOut, TimeUnit timeoutTimeUnit) {
        if (backlog == null) {
            backlog = 50;
        }
        if (setSoTimeOut == null) {
            setSoTimeOut = 0;
        } else {
            setSoTimeOut = (int)timeoutTimeUnit.toMillis(setSoTimeOut);
        }
        try {
            if (address != null) {
                this.socket = new ServerSocket(port, backlog, address);
            } else  {
                this.socket = new ServerSocket(port, backlog);
            }
            this.socket.setSoTimeout(setSoTimeOut);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.serverThread = new SocketThread(this::bindPortAndListenBlocking);
    }

    public abstract void onReceive(JsonElement message);

    /**
     * Sends a message to the connected Client
     * @param message the json message
     * @param inNewThread if the sending should run in a new thread
     * @apiNote Sending a message is BLOCKING if not run in a new thread
     * @return true if the message was sent, false otherwise
     */
    public boolean sendMessage(JsonElement message) {
        if (!socket.isBound()) {
            return false;
        }
        this.sendingThread = new Thread(() -> sendMessageBlocking(message));
        sendingThread.start();
        return true;
    }

    /**
     * Binds the port and starts listening for a client to connect
     * @apiNote Listening is BLOCKING if not run in a standalone thread. (is set in constructor)
     */
    public void bindPortAndListen() {
        serverThread.start();
    }

    /**
     * Disconnects the bound server port
     * @return {@link Boolean} true if a bound port was disconnected, false otherwise
     */
    public void disconnect() {
        if (!socket.isBound()) {
            return;
        }
        new Thread(()-> {
            if (sendingThread != null) {
                while (sendingThread.isAlive()) {
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            try {
                this.connection.close();
                this.socket.close();
                onDisconnection();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public Thread getThread() {
        return serverThread;
    }


    private void sendMessageBlocking(JsonElement message) {
        try {
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.writeUTF(getGson().toJson(message));
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Exception bindPortAndListenBlocking() {
        try {
            System.out.println("Listening on port " + socket.getLocalPort());
            this.connection = this.socket.accept();
            System.out.println("Connected by client " + connection.getInetAddress().getHostAddress() + ":" + connection.getPort());
            onConnection();
            DataInputStream in = new DataInputStream(connection.getInputStream());
            while (!connection.isClosed()) {
                String cmd = in.readUTF();
                if (cmd.isEmpty()) continue;
                this.onReceive(getGson().fromJson(cmd, JsonElement.class));
            }
            return null;
        } catch (Exception e) {
            return e;
        }
    }
}
