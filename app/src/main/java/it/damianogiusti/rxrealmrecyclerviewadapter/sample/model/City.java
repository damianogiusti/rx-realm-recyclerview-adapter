package it.damianogiusti.rxrealmrecyclerviewadapter.sample.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Damiano Giusti on 22/01/17.
 */
public class City extends RealmObject {
    @PrimaryKey
    private String id;
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof City))
            return false;
        City city = (City) obj;
        return id.equals(city.id);
    }
}
