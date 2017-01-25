package it.damianogiusti.rxrealmrecyclerviewadapter.sample.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import it.damianogiusti.rxrealmrecyclerviewadapter.RealmOperations;
import it.damianogiusti.rxrealmrecyclerviewadapter.RxRealmRecyclerViewAdapter;
import it.damianogiusti.rxrealmrecyclerviewadapter.sample.R;
import it.damianogiusti.rxrealmrecyclerviewadapter.sample.model.City;

/**
 * Created by Damiano Giusti on 22/01/17.
 */
public class CitiesAdapter extends RxRealmRecyclerViewAdapter<City, CitiesViewHolder> {

    private LayoutInflater inflater;

    public CitiesAdapter(@NonNull Context context, @NonNull RealmOperations<City> operations) {
        super(context, operations);
        inflater = LayoutInflater.from(context);
    }

    @Override
    protected RealmConfiguration getRealmConfiguration() {
        return Realm.getDefaultInstance().getConfiguration();
    }

    @Override
    protected boolean areItemsEquals(City item1, City item2) {
        return item1.equals(item2);
    }

    @Override
    public void onBindViewHolder(CitiesViewHolder holder, City item, int position) {
        holder.txtName.setText(item.getName());
    }

    @Override
    public CitiesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rowView = inflater.inflate(R.layout.city_list_item_layout, parent, false);
        return new CitiesViewHolder(rowView);
    }
}
