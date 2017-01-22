package it.damianogiusti.rxrealmrecyclerviewadapter;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

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
                .flatMap(new Func1<RealmResults<T>, Observable<T>>() {
                    @Override
                    public Observable<T> call(RealmResults<T> objects) {
                        return Observable.from(objects);
                    }
                })
                .map(new Func1<T, T>() {
                    @Override
                    public T call(T object) {
                        return realm.copyFromRealm(object);
                    }
                })
                .filter(new Func1<T, Boolean>() {
                    @Override
                    public Boolean call(T object) {
                        int index = adapterItems.indexOf(object);
                        if (index >= 0)
                            return !Utils.areObjectsEquals(object.getClass(), object, adapterItems.get(index));
                        return true;
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
        subscription.unsubscribe();
        realmThreadHandler.post(new Runnable() {
            @Override
            public void run() {
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

    private class RealmSubscriber implements Action1<T> {
        @Override
        public void call(T object) {
            notifyDataSetChanged();
            int index = adapterItems.indexOf(object);
            if (index >= 0) {
                adapterItems.set(index, object);
                notifyItemChanged(index);
            } else {
                adapterItems.add(object);
                notifyItemInserted(adapterItems.indexOf(object));
            }
        }
    }
}
