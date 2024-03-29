package android.util;

// Log test class to avoid 'Method d in android.util.Log not mocked' in tests
// https://medium.com/@gal_41749/android-unitests-and-log-class-9546b6480006
public class Log {
    public static int d(String tag, String msg) {
        System.out.println("DEBUG: " + tag + ": " + msg);
        return 0;
    }

    public static int i(String tag, String msg) {
        System.out.println("INFO: " + tag + ": " + msg);
        return 0;
    }

    public static int w(String tag, String msg) {
        System.out.println("WARN: " + tag + ": " + msg);
        return 0;
    }

    public static int e(String tag, String msg) {
        System.out.println("ERROR: " + tag + ": " + msg);
        return 0;
    }

    public static int e(String tag, String msg, Throwable t) {
        System.out.println("ERROR: " + tag + ": " + msg + " " + t.toString());
        return 0;
    }
}