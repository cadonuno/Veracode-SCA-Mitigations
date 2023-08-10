package selenium;

import java.util.concurrent.TimeoutException;

public interface RunnableWithTimeout {

    void run() throws TimeoutException;
}
