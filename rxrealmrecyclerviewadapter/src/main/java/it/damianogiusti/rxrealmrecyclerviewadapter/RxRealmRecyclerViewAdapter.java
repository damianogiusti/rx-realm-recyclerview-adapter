package it.damianogiusti.rxrealmrecyclerviewadapter;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import io.realm.RealmObject;
import io.realm.RealmResults;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

/**
 * Created by Damiano Giusti on 22/01/17.
 */
public abstract class RxRealmRecyclerViewAdapter<T extends RealmModel, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    private static final String TAG = "RxRealmRVAdapter";
    private static final int ZERO_MAPPING = Integer.MIN_VALUE;


    protected Context context;
    private LayoutInflater inflater;

    private Realm realm;
    private RealmThread realmThread;
    private Handler realmThreadHandler;
    private Subscription subscription;
    private final PublishSubject<T> onClickSubject;
    private final PublishSubject<T> onLongClickSubject;

    protected List<T> adapterItems;

    public RxRealmRecyclerViewAdapter(@NonNull final Context context,
                                      @NonNull final RealmOperations<T> operations) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.adapterItems = new ArrayList<>();
        // init thread for operations
        this.realmThread = new RealmThread();
        this.realmThread.start();
        this.realmThreadHandler = new Handler(realmThread.getLooper());
        // init observable
        this.subscription = subscribe(operations);
        this.onClickSubject = PublishSubject.create();
        this.onLongClickSubject = PublishSubject.create();
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
                        List<Integer> indexes = new ArrayList<>();
                        // cycle all results
                        for (int i = 0; i < adapterItems.size(); i++) {
                            T adapterItem = adapterItems.get(i);
                            boolean found = false;
                            for (T realmItem : objects)
                                if (found = areItemsEquals(realmItem, adapterItem))
                                    break;
                            // if new result collection does not contain an item, prepare to remove it
                            if (!found)
                                indexes.add(i);
                        }

                        List<Pair<T, Integer>> itemsWithIndex = new ArrayList<>(objects.size());
                        // map all new objects
                        for (int i = 0; i < objects.size(); i++)
                            itemsWithIndex.add(new Pair<>(objects.get(i), i));
                        // map all objects to remove with a negative index
                        for (int index : indexes) {
                            if (index == 0)
                                itemsWithIndex.add(new Pair<>(adapterItems.get(index), ZERO_MAPPING));
                            else
                                itemsWithIndex.add(new Pair<>(adapterItems.get(index), index * -1));
                        }

                        return Observable.from(itemsWithIndex);
                    }
                })
                .map(new Func1<Pair<T, Integer>, Pair<T, Integer>>() {
                    @Override
                    public Pair<T, Integer> call(Pair<T, Integer> pair) {
                        if (pair.second >= 0 && RealmObject.isManaged(pair.first))
                            return new Pair<>(realm.copyFromRealm(pair.first), pair.second);
                        return pair;
                    }
                })
                .filter(new Func1<Pair<T, Integer>, Boolean>() {
                    @Override
                    public Boolean call(Pair<T, Integer> pair) {
                        // if the item needs to be removed, emit it immediately
                        if (pair.second < 0)
                            return true;

                        int index = -1;
                        // look for item in the actual items list
                        for (int i = 0; i < adapterItems.size(); i++) {
                            T adapterItem = adapterItems.get(i);
                            if (areItemsEquals(adapterItem, pair.first)) {
                                index = i;
                                break;
                            }
                        }
                        // if not found, needs to be emitted
                        return index < 0;
                    }
                })
                .subscribeOn(AndroidSchedulers.from(realmThread.getLooper()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RealmSubscriber());
    }

    protected abstract RealmConfiguration getRealmConfiguration();

    @WorkerThread
    protected abstract boolean areItemsEquals(T item1, T item2);

    @Override
    public int getItemCount() {
        return adapterItems.size();
    }

    public abstract void onBindViewHolder(VH holder, T item, int position);

    @Override
    public final void onBindViewHolder(VH holder, int position) {
        final T item = adapterItems.get(position);
        // setup click events
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickSubject.onNext(item);
            }
        });
        // setup long click events
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onLongClickSubject.onNext(item);
                return true;
            }
        });
        // delegate binding
        onBindViewHolder(holder, item, position);
    }

    @Override
    public void onViewRecycled(VH holder) {
        super.onViewRecycled(holder);
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);
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

    public Observable<T> observeClickEvents() {
        return onClickSubject.asObservable();
    }

    public Observable<T> observeLongClickEvents() {
        return onLongClickSubject.asObservable();
    }

    // utils

    private class RealmSubscriber implements Action1<Pair<T, Integer>> {
        @Override
        public void call(Pair<T, Integer> pair) {
            T object = pair.first;
            int positionInResults = pair.second;

            // if index is negative, item at the abs value of index need to be removed
            if (positionInResults < 0) {
                int indexToRemove = positionInResults == ZERO_MAPPING ? 0 : positionInResults * -1;
                adapterItems.remove(indexToRemove);
                notifyItemRemoved(indexToRemove);
            } else {
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
}
