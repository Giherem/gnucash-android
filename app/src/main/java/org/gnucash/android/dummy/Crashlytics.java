package org.gnucash.android.dummy;

/**
 * Created by jamian on 17/04/17.
 */


/**
 * Dummy class for suppressing the real Crashlytics library
 */
public class Crashlytics {
    public static void log(String s) {}
    public static void logException(Throwable e) {}

    public static void log(int error, String tag, String s) {
    }
}
