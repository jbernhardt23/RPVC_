package com.example.josebernhardt.rpvc_;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by jose on 07/05/16.
 */
public class MyAdapter extends BaseAdapter {


    private static LayoutInflater inflater = null;
    Context context;
    List<Car> CarList;
    Handler updateListView = new Handler();

    //Constructor recieving from the ListAdpater
    public MyAdapter(Context context, List<Car> CarList) {
        this.context = context;
        this.CarList = CarList;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return CarList.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return CarList.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub

        View vi = convertView;
        if (vi == null)
            vi = inflater.inflate(R.layout.item_view, null);
        this.notifyDataSetChanged();


        TextView cardIdText = (TextView) vi.findViewById(R.id.CarId);
        cardIdText.setText(String.valueOf(CarList.get(position).getCarId()));

        TextView speedText = (TextView) vi.findViewById(R.id.carSpeedText);
        speedText.setText("Car Speed: " + String.valueOf(CarList.get(position).getCurrentSpeed() + " Km/h"));

        TextView distanceText = (TextView) vi.findViewById(R.id.carDistanceText);
        distanceText.setText("Distance: "  + String.valueOf(CarList.get(position).getDistanceBetween() + " m"));



        ImageView imageView = (ImageView) vi.findViewById(R.id.ListimageView);
        imageView.setImageResource(R.drawable.car_icon);
        return vi;

    }



}
