package com.example.josebernhardt.rpvc_;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.List;

/**
 * Created by jose on 02/05/16.
 */
public class CommandCenter extends Fragment {

    ListView listView;
    List<Car> CarList = MainActivity.CarList;
    MyAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new MyAdapter(getActivity(),CarList);


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Creating Command Framgnet
        View view = inflater.inflate(R.layout.command_fragment, container, false);

        return view;

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Attach ListView with the List XML
        listView = (ListView) getView().findViewById(R.id.list);
        //Calling the adapter and passing Data to the constructor
        listView.setAdapter(adapter);


    }


}

