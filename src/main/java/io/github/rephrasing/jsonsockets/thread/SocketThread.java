package io.github.rephrasing.jsonsockets.thread;

import java.util.Optional;
import java.util.function.Supplier;

public class SocketThread extends Thread {

    private final Supplier<Exception> function;
    private Optional<Exception> exception;

    public SocketThread(Supplier<Exception> function) {
        this.function = function;
    }

    @Override
    public void run() {
        exception = Optional.ofNullable(function.get());
    }

    public Optional<Exception> getThrownException() {
        return exception;
    }
}
