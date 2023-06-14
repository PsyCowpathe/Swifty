package com.example.swiftyy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ListAdapter extends ArrayAdapter<DataModel>
{
    private final Context context;

    public ListAdapter(Context context, ArrayList<DataModel> values)
    {
        super(context, R.layout.row_item, values);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        DataModel dataModel = getItem(position);
        View rowView = null;
        LayoutInflater inflater = null;

        if (rowView == null)
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rowView = inflater.inflate(R.layout.row_item, parent, false);
        TextView nameView = (TextView) rowView.findViewById(R.id.name);
        TextView markView = (TextView) rowView.findViewById(R.id.mark);
        nameView.setText(dataModel.getName());
        markView.setText(dataModel.getMark());

        return rowView;
    }
}
