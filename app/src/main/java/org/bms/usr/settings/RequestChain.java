package org.bms.usr.settings;

import androidx.core.util.Consumer;

import java.util.LinkedList;
import java.util.Queue;

public class RequestChain {

    private final Queue<Runnable> tasks = new LinkedList<>();
    private final Runnable onComplete;
    private final Consumer<String> onError;

    public RequestChain(Runnable onComplete, Consumer<String> onError) {
        this.onComplete = onComplete;
        this.onError = onError;
    }

    public void add(Runnable task) {
        tasks.add(task);
    }

    public void start() {
        executeNext();
    }

    private void executeNext() {
        Runnable task = tasks.poll();

        if (task == null) {
            onComplete.run();
            return;
        }

        task.run();
    }

    public UsrHttpClient.Callback wrap(UsrHttpClient.Callback cb) {
        return new UsrHttpClient.Callback() {
            @Override
            public void onSuccess(String resp) {
                cb.onSuccess(resp);
                executeNext();
            }

            @Override
            public void onError(String err) {
                cb.onError(err);
                onError.accept(err);
            }
        };
    }
}

