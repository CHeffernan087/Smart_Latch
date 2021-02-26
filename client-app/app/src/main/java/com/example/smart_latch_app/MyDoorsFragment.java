package com.example.smart_latch_app;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.ListFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MyDoorsFragment extends ListFragment {
    private FloatingActionButton addDoorsButton;
    private FirstFragment firstFragment = new FirstFragment();
    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.mydoors_fragment, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addDoorsButton = view.findViewById(R.id.addDoorButton);
        fragmentManager = getActivity().getSupportFragmentManager();
        String[] values = new String[] { "Test Door ID" };
        MySimpleArrayAdapter adapter = new MySimpleArrayAdapter(getActivity(), values);
        setListAdapter(adapter);


        addDoorsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("CLICKED ADD BUTTON");
                // open an NFC activity
                // I'd say i'll haveto do some refactoring in the DoorDisplayBox.java to get it to be a component we can use more generally and pass in params as needed
            }
        });

    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String item = (String) getListAdapter().getItem(position);
        Toast.makeText(getActivity(), item + " selected", Toast.LENGTH_LONG).show();
        gotoFirstFragment();
    }

    private void gotoFirstFragment() {
        // need to pass in something here that identifies the door selected
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer,firstFragment);
        fragmentTransaction.commit();
    }
}
