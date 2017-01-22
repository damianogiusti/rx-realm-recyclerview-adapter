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

        Realm getRealm();
    }

    public static NewCityDialog newInstance() {
        return new NewCityDialog();
    }

    public static NewCityDialog newInstance(long cityId) {
        NewCityDialog newCityDialog = new NewCityDialog();

        Bundle bundle = new Bundle();
        bundle.putLong(CITY_ID_KEY_FOR_BUNDLE, cityId);
        newCityDialog.setArguments(bundle);

        return newCityDialog;
    }

    private Listener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.new_city_dialog_layout, null, false);

        long id = -1;
        if (getArguments() != null)
            id = getArguments().getLong(CITY_ID_KEY_FOR_BUNDLE, -1);

        final EditText txtNewCityName = (EditText) view.findViewById(R.id.txtNewCityName);

        if (id >= 0 && listener != null)
            txtNewCityName.setText(listener.getRealm().where(City.class).equalTo("id", id).findFirst().getName());

        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(R.string.add)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null)
                            listener.onCityCreationConfirmed(txtNewCityName.getText().toString());
                    }
                });
        return dialog.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Listener)
            listener = (Listener) activity;
    }
}
