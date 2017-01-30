package it.damianogiusti.rxrealmrecyclerviewadapter.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmResults;
import it.damianogiusti.rxrealmrecyclerviewadapter.RealmOperations;
import it.damianogiusti.rxrealmrecyclerviewadapter.sample.adapter.CitiesAdapter;
import it.damianogiusti.rxrealmrecyclerviewadapter.sample.dialogs.NewCityDialog;
import it.damianogiusti.rxrealmrecyclerviewadapter.sample.model.City;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity implements
        NewCityDialog.Listener {

    private static final String NEW_CITY_DIALOG_TAG = "MainActivity.NewCityDialog";

    private RecyclerView recyclerView;
    private Button btnAddCity;

    private Realm realm;
    private CitiesAdapter citiesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();

        realm = Realm.getDefaultInstance();

        citiesAdapter = new CitiesAdapter(getApplicationContext(), new RealmOperations<City>() {
            @Override
            public RealmResults<City> execute(Realm realm) {
                return realm.where(City.class).findAllSorted("name");
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(citiesAdapter);

        btnAddCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddCityClicked();
            }
        });

        citiesAdapter.observeLongClickEvents()
                .subscribe(new Action1<City>() {
                    @Override
                    public void call(final City city) {
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                realm.where(City.class).equalTo("id", city.getId()).findFirst().deleteFromRealm();
                                ;
                            }
                        });
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
        citiesAdapter.release();
    }

    private void bindViews() {
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        btnAddCity = (Button) findViewById(R.id.btnAddCity);
    }

    private void onAddCityClicked() {
        NewCityDialog.newInstance()
                .show(getFragmentManager(), NEW_CITY_DIALOG_TAG);
    }

    @Override
    public void onCityCreationConfirmed(String cityName) {
        final City city = new City();
        city.setId(UUID.randomUUID().toString());
        city.setName(cityName);
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.insert(city);
            }
        });
    }

    @Override
    public Realm getRealm() {
        return realm;
    }
}
