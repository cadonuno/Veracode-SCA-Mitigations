package util;

import java.util.concurrent.TimeUnit;

public class TimeUtils {
    public static void sleepFor(int numberOfSeconds) {
        try {
            TimeUnit.SECONDS.sleep(numberOfSeconds);
        } catch (InterruptedException e) {
            //nothing to do here
        }
    }
}
