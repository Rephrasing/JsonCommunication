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
    private final boolean standaloneThread;

    private Socket socket;
    private SocketThread clientThread;

    public JsonClientSocket(String address, int port, int setSoTimeout, TimeUnit timeUnit, boolean standaloneThread) {
        this.address = address;
        this.port = port;
        this.standaloneThread = standaloneThread;
        int setSoTimeoutMillis = (int) timeUnit.toMillis(setSoTimeout);
        if (standaloneThread) this.clientThread = new SocketThread(() -> this.attemptConnectionBlocking(setSoTimeoutMillis));
    }

    @Override
    public Optional<SocketThread> getStandaloneThread() {
        return Optional.ofNullable(clientThread);
    }

    @Override
    public boolean isStandalone() {
        return standaloneThread;
    }

    public abstract void onReceive(JsonElement message);

    /**
     * Sends a message to the server
     * @param message the json message
     * @param inNewThread whether the message should be sent in a new thread or not
     * @return true if the message was sent, false otherwise
     * @apiNote Sending messages is BLOCKING if not executed in its own thread
     */
    public boolean sendMessage(JsonElement message, boolean inNewThread) {
        if (!socket.isConnected()) {
            return false;
        }
        if (inNewThread) {
            new Thread(()-> sendMessageBlocking(message)).start();
        } else {
            sendMessageBlocking(message);
        }
        return true;
    }

    /**
     * Attempts to connect to a server
     * @param setSoTimeout The Timeout which the connection should wait without receiving any data through {@link #onReceive(JsonElement)}
     * @param timeUnit The {@link TimeUnit} of {@literal setSoTimeout}
     * @apiNote Connecting to a server is BLOCKING if not executed in its standalone thread (is set in constructor)
     */
    @CheckReturnValue
    public Optional<Exception> attemptConnection(int setSoTimeout, TimeUnit timeUnit) {
        int setSoTimeoutMillis = (int) timeUnit.toMillis(setSoTimeout);
        if (standaloneThread) {
            this.clientThread = new SocketThread(() -> this.attemptConnectionBlocking(setSoTimeoutMillis));
            clientThread.start();
            return clientThread.getThrownException();
        }
        return Optional.ofNullable(this.attemptConnectionBlocking(setSoTimeoutMillis));
    }

    /**
     * Disconnects from the server
     * @return true if disconnected, false otherwise
     */
    public boolean disconnect() {
        if (socket.isClosed()) {
            return false;
        }
        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
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

    private Exception attemptConnectionBlocking(int setSoTimeout) {
        try  {
            this.socket = new Socket(address, port);
            socket.setSoTimeout(setSoTimeout);
            System.out.println("Connected to server " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
            DataInputStream in = new DataInputStream(socket.getInputStream());
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
