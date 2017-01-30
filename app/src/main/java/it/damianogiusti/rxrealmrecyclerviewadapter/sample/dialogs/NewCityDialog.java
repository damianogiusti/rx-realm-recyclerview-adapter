package it.damianogiusti.rxrealmrecyclerviewadapter.sample.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import io.realm.Realm;
import it.damianogiusti.rxrealmrecyclerviewadapter.sample.R;
import it.damianogiusti.rxrealmrecyclerviewadapter.sample.model.City;

/**
 * Created by Damiano Giusti on 13/01/17.
 */
public class NewCityDialog extends DialogFragment {

    private static final String CITY_ID_KEY_FOR_BUNDLE = "cityIdBundled";

    public interface Listener {
        void onCityCreationConfirmed(String cityName);

        void onCityUpdateConfirmed(City oldCity, String newName);

        Realm getRealm();
    }

    public static NewCityDialog newInstance() {
        return new NewCityDialog();
    }

    public static NewCityDialog newInstance(String cityId) {
        NewCityDialog newCityDialog = new NewCityDialog();

        Bundle bundle = new Bundle();
        bundle.putString(CITY_ID_KEY_FOR_BUNDLE, cityId);
        newCityDialog.setArguments(bundle);

        return newCityDialog;
    }

    private Listener baseActivity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.new_city_dialog_layout, null, false);

        String id = null;
        if (getArguments() != null)
            id = getArguments().getString(CITY_ID_KEY_FOR_BUNDLE, null);

        final boolean isUpdate = (id != null);
        final EditText txtNewCityName = (EditText) view.findViewById(R.id.txtNewCityName);

        City cityToUpdate = null;
        if (isUpdate && baseActivity != null) {
            cityToUpdate = baseActivity.getRealm().where(City.class).equalTo("id", id).findFirst();
            txtNewCityName.setText(cityToUpdate.getName());
        }

        final City finalCityToUpdate = cityToUpdate;
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(isUpdate ? R.string.update : R.string.add)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (baseActivity != null) {
                            if (isUpdate)
                                baseActivity.onCityUpdateConfirmed(finalCityToUpdate, txtNewCityName.getText().toString());
                            else
                                baseActivity.onCityCreationConfirmed(txtNewCityName.getText().toString());
                        }
                    }
                });
        return dialog.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Listener)
            baseActivity = (Listener) activity;
    }
}
