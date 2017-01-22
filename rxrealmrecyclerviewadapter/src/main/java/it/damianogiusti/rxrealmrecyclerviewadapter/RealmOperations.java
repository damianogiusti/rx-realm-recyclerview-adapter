package it.damianogiusti.rxrealmrecyclerviewadapter;

import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.RealmResults;

/**
 * Created by Damiano Giusti on 22/01/17.
 */
public interface RealmOperations<T extends RealmModel> {
    RealmResults<T> execute(Realm realm);
}
