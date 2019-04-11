package threadpool;

import java.io.Closeable;

public interface IThreadPool extends Closeable {
    void post(Runnable runnable);
}
