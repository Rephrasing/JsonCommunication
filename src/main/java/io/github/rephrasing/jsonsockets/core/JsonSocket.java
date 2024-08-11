package io.github.rephrasing.jsonsockets.core;

import com.google.gson.Gson;

/**
 * Author: Rephrasing
 */
public interface JsonSocket {


    Thread getThread();

    /**
     * Provides the Gson serializer
     * @return {@link Gson}
     */
    Gson getGson();

    void onConnection();
    void onDisconnection();

}
