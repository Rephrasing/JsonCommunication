package io.github.rephrasing.jsonsockets.client;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.gson.JsonElement;
import io.github.rephrasing.jsonsockets.core.JsonSocket;
import io.github.rephrasing.jsonsockets.thread.SocketThread;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * This class handles the client side of two applications
 */
public abstract class JsonClientSocket implements JsonSocket {

    private final String address;
    private final int port;

    private Socket socket;
    private SocketThread readingThread;

    private final int setSoTimeoutMillis;
    private Thread sendingThread;

    public JsonClientSocket(String address, int port, int setSoTimeout, TimeUnit timeUnit) {
        this.address = address;
        this.port = port;
        this.setSoTimeoutMillis = (int) timeUnit.toMillis(setSoTimeout);
        this.readingThread = new SocketThread(this::attemptConnectionBlocking);
    }

    @Override
    public Thread getThread() {
        return readingThread;
    }


    public abstract void onReceive(JsonElement message);

    /**
     * Sends a message to the server
     * @param message the json message
     * @return true if the message was sent, false otherwise
     * @apiNote Sending messages is BLOCKING if not executed in its own thread
     */
    public boolean sendMessage(JsonElement message) {
        if (!socket.isConnected()) {
            return false;
        }
        this.sendingThread = new Thread(() -> sendMessageBlocking(message));
        sendingThread.start();
        return true;
    }

    /**
     * Attempts to connect to a server
     * @apiNote Connecting to a server is BLOCKING if not executed in its standalone thread (is set in constructor)
     */
    @CheckReturnValue
    public Optional<Exception> attemptConnection() {
        this.readingThread = new SocketThread(this::attemptConnectionBlocking);
        readingThread.start();
        return readingThread.getThrownException();
    }

    /**
     * Disconnects from the server
     * @return true if disconnected, false otherwise
     */
    public void disconnect() {
        if (socket.isClosed()) {
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
                socket.close();
                onDisconnection();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendMessageBlocking(JsonElement message) {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(getGson().toJson(message));
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Exception attemptConnectionBlocking() {
        try  {
            this.socket = new Socket(address, port);
            socket.setSoTimeout(this.setSoTimeoutMillis);
            System.out.println("Connected to server " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            onConnection();
            while (!socket.isClosed()) {
                String cmd = in.readUTF();
                if (cmd.isEmpty()) continue; //empty line was sent
                this.onReceive(getGson().fromJson(cmd, JsonElement.class));
            }
            return null;
        } catch (Exception e) {
            return e;
        }
    }
}