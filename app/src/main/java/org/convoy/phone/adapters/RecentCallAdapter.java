package org.convoy.phone.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.convoy.phone.R;
import org.convoy.phone.model.RecentCallItem;

import java.util.List;

public class RecentCallAdapter extends ArrayAdapter<RecentCallItem> {
    private final LayoutInflater inflater;

    public RecentCallAdapter(Context context, List<RecentCallItem> items) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_recent, parent, false);
        }

        RecentCallItem item = getItem(position);
        if (item != null) {
            ((TextView) view.findViewById(R.id.recent_name)).setText(item.name);
            ((TextView) view.findViewById(R.id.recent_number)).setText(item.number);
            ((TextView) view.findViewById(R.id.recent_details)).setText(item.details);
        }

        return view;
    }
}
