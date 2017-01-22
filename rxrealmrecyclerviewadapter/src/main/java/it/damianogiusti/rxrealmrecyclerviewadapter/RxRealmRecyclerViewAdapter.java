package it.damianogiusti.rxrealmrecyclerviewadapter;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import io.realm.RealmResults;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by Damiano Giusti on 22/01/17.
 */
public abstract class RxRealmRecyclerViewAdapter<T extends RealmModel, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    private Context context;
    private Realm realm;
    private RealmThread realmThread;
    private Handler realmThreadHandler;
    private Subscription subscription;

    protected List<T> adapterItems;

    public RxRealmRecyclerViewAdapter(@NonNull final Context context,
                                      @NonNull final RealmOperations<T> operations) {
        this.context = context;
        this.adapterItems = new ArrayList<>();
        // init thread for operations
        this.realmThread = new RealmThread();
        this.realmThread.start();
        this.realmThreadHandler = new Handler(realmThread.getLooper());
        // init observable
        this.subscription = subscribe(operations);
    }

    private Subscription subscribe(@NonNull final RealmOperations<T> operations) {
        return Observable
                .create(new Observable.OnSubscribe<RealmResults<T>>() {
                    @Override
                    public void call(Subscriber<? super RealmResults<T>> subscriber) {
                        if (realm == null)
                            realm = Realm.getInstance(getRealmConfiguration());
                        RealmResults<T> results = operations.execute(realm);
                        results.asObservable().subscribe(subscriber);
                    }
                })
                .flatMap(new Func1<RealmResults<T>, Observable<Pair<T, Integer>>>() {
                    @Override
                    public Observable<Pair<T, Integer>> call(RealmResults<T> objects) {
                        return Observable.from(objects).map(new Func1<T, Pair<T, Integer>>() {
                            int position = 0;

                            @Override
                            public Pair<T, Integer> call(T t) {
                                return new Pair<>(t, position++);
                            }
                        });
                    }
                })
                .map(new Func1<Pair<T, Integer>, Pair<T, Integer>>() {
                    @Override
                    public Pair<T, Integer> call(Pair<T, Integer> pair) {
                        return new Pair<>(realm.copyFromRealm(pair.first), pair.second);
                    }
                })
                .filter(new Func1<Pair<T, Integer>, Boolean>() {
                    @Override
                    public Boolean call(Pair<T, Integer> pair) {
                        int index = adapterItems.indexOf(pair.first);
                        boolean found = index >= 0;
                        return !found || !Utils.areObjectsEquals(pair.first.getClass(), pair.first, adapterItems.get(index));
                    }
                })
                .subscribeOn(AndroidSchedulers.from(realmThread.getLooper()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RealmSubscriber());
    }

    protected abstract RealmConfiguration getRealmConfiguration();

    @Override
    public int getItemCount() {
        return adapterItems.size();
    }

    public abstract void onBindViewHolder(VH holder, T item, int position);

    @Override
    public final void onBindViewHolder(VH holder, int position) {
        onBindViewHolder(holder, adapterItems.get(position), position);
    }

    public void release() {
        realmThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                subscription.unsubscribe();
                realm.close();
                realm = null;
                realmThread.quit();
            }
        });
    }

    // utils

    protected Context getContext() {
        return context;
    }

    private class RealmSubscriber implements Action1<Pair<T, Integer>> {
        @Override
        public void call(Pair<T, Integer> pair) {
            T object = pair.first;
            int positionInResults = pair.second;

            int index = adapterItems.indexOf(object);
            boolean found = index >= 0;
            if (found) {
                adapterItems.set(index, object);
                notifyItemChanged(index);
            } else {
                adapterItems.add(positionInResults, object);
                notifyItemInserted(positionInResults);
            }
        }
    }
}
