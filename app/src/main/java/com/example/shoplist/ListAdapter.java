package com.example.shoplist;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

import com.example.shoplist.MainActivity;

import static com.example.shoplist.MainActivity.DIALOG_EDIT;

/**
 * Created by frederik on 12-03-2018.
 */
public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
    //Interface to MainActivity:
    public interface checkboxInterface
    {
        void checkboxHasChanged(int id, boolean val);
    }

    checkboxInterface cListener;

    //Data-set, containing all ListItem's:
    private ArrayList<ListItem> mDataset;
    private Context cont;


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View mView;
        public ViewHolder(View v) {
            super(v);
            mView = v;
        }
    }


    public ListAdapter(ArrayList<ListItem> myDataset, Context context)
    {
        mDataset = myDataset;
        cont = context;
        cListener = (checkboxInterface)context;
    }

    @Override
    public ListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        //Create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_element, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
       int modPosition = 0;
       int count = 0; //Number of visible items

       for(int i = 0; i < mDataset.size(); i++)
       {
           if(!mDataset.get(i).hidden) //If the current item is visible
           {
               count++;
               if(count == position+1)
                   modPosition = i;
           }
       }

       for(int i = 0; i < mDataset.size(); i++)
       {
           if(!mDataset.get((modPosition+i) % mDataset.size()).hidden) //If the current item is visible
           {
               modPosition = (modPosition+i) % mDataset.size();
               break;
           }
       }

        //Get TextView (item title):
        TextView tw = holder.mView.findViewById(R.id.text_field);
        tw.setText(mDataset.get(modPosition).title);

        final int newPos = modPosition;

        //Tap item title to edit it:
        tw.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                editListItem(mDataset.get(newPos));
            }
        });

        //Get item checkbox:
        CheckBox cb = holder.mView.findViewById(R.id.checkbox);
        cb.setChecked(mDataset.get(modPosition).checkBox);

        //Listen for taps on item checkbox:
        cb.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mDataset.get(newPos).checkBox = !mDataset.get(newPos).checkBox;
                cListener.checkboxHasChanged( mDataset.get(newPos).id,  mDataset.get(newPos).checkBox);
            }
        });
    }

    @Override
    public int getItemCount()
    {
        int res = 0; //Number of visible items

        for(int i = 0; i < mDataset.size(); i++ )
        {
            //Count only visible items:
            if(!mDataset.get(i).hidden)
                res++;
        }
        return res;
    }

    //Invoked when an item's title is tapped:
    public void editListItem(ListItem li)
    {
        //Put all ListItems attributes into a bundle:
        Bundle b = new Bundle();
        b.putInt("DialogState", DIALOG_EDIT);
        b.putInt("id", li.id);
        b.putString("title", li.title);
        b.putDouble("price", li.price);
        b.putBoolean("checkbox", li.checkBox);
        b.putString("store", li.store);

        //Start a dialog fragment to edit the list-item:
        FragmentManager fm = ((Activity)cont).getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        NewItemDialogFragment nidf = new NewItemDialogFragment();
        nidf.setArguments(b);
        nidf.show(ft, "new_item_dialog");
    }
}

