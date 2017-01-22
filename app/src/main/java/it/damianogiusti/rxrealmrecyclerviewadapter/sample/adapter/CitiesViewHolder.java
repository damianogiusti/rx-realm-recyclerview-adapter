package it.damianogiusti.rxrealmrecyclerviewadapter.sample.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import it.damianogiusti.rxrealmrecyclerviewadapter.sample.R;

/**
 * Created by Damiano Giusti on 22/01/17.
 */
final class CitiesViewHolder extends RecyclerView.ViewHolder {

    public TextView txtName;

    public CitiesViewHolder(View itemView) {
        super(itemView);
        txtName = (TextView) itemView.findViewById(R.id.txtName);
    }
}
