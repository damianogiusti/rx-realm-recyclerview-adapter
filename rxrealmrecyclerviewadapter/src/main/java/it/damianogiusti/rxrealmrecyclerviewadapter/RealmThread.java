package it.damianogiusti.rxrealmrecyclerviewadapter;

import android.os.HandlerThread;

/**
 * Created by Damiano Giusti on 22/01/17.
 */
final class RealmThread extends HandlerThread {
    public RealmThread() {
        super("RxRealmAdapter-Thread");
    }
}
