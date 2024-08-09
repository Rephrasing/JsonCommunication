package io.github.rephrasing.jsonsockets.core;

import com.google.gson.Gson;
import io.github.rephrasing.jsonsockets.thread.SocketThread;

import java.util.Optional;

/**
 * Author: Rephrasing
 */
public interface JsonSocket {

    /**
     * @return Returns {@link Optional} (Present if {@link #isStandalone()} is true)
     */
    Optional<SocketThread> getStandaloneThread();

    /**
     * Checks if the socket is running on its own standalone thread
     * @return {@link Boolean} of true or false
     */
    boolean isStandalone();

    /**
     * Provides the Gson serializer
     * @return {@link Gson}
     */
    Gson getGson();

    void onConnection();
    void onDisconnection();

}
