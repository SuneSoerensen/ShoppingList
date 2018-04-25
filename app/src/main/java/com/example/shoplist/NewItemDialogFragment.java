package com.example.shoplist;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;

import static com.example.shoplist.MainActivity.DIALOG_EDIT;

/**
 * Created by frederik on 12-03-2018.
 */

public class NewItemDialogFragment extends DialogFragment {
    //Interface to MainActivity:
    public interface NoticeDialogListener
    {
        void onDialogPositiveClick(DialogFragment dialog, ListItem li, boolean liIsNew);
        void onDialogNegativeClick(DialogFragment dialog, int anId);
    }
    NoticeDialogListener mListener;

    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        try
        {
            mListener = (NoticeDialogListener)activity;
        }
        catch(ClassCastException e)
        {
            throw new ClassCastException(activity.toString() + "Must implement NoticeDialogListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        //Display keyboard when dialog is created:
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        //Get the bundle, containing info. about whether and item is being edited or created:
        final Bundle bundle = getArguments();
        final int state = bundle.getInt("DialogState");
        final int ID = bundle.getInt("id");

        //Create the dialog:
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater li = getActivity().getLayoutInflater();
        View dialogView = li.inflate(R.layout.item_dialog, null);

        boolean isNew = true; //As default, assume an item is being created

        //If an item is being edited:
        if(state == DIALOG_EDIT)
        {
            isNew = false; //This is not a new item

            //Get the title from the bundle and set the EditText:
            EditText etTitleE;
            etTitleE = dialogView.findViewById(R.id.title);
            etTitleE.setText(bundle.getString("title"));

            //Get the price from the bundle and set the EditText:
            EditText etPriceE;
            etPriceE = dialogView.findViewById(R.id.price);
            etPriceE.setText(Double.toString(bundle.getDouble( "price")));

            //Get the store from the bundle and set the EditText:
            EditText etStore;
            etStore = dialogView.findViewById(R.id.store);
            etStore.setText(bundle.getString("store"));
        }

        final boolean isNewCopy = isNew;

        builder.setMessage(R.string.new_item_dialog_text)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                            //Get the title from the EditText:
                            EditText etTitle;
                            etTitle = ((AlertDialog)dialog).findViewById(R.id.title);
                            String title = etTitle.getText().toString();

                            //Get the Price from the EditText:
                            EditText etPrice;
                            etPrice = ((AlertDialog)dialog).findViewById(R.id.price);

                            double price;
                            if(etPrice.getText().toString().length() == 0) //If no price is entered, set it to 0
                            {
                                price = 0;
                            }
                            else
                            {
                                price = Double.parseDouble(etPrice.getText().toString());
                            }

                            //Get the checkbox-state from the bundle:
                            boolean cb = bundle.getBoolean("checkbox");

                            //Get the store from the EditText:
                            EditText etStore;
                            etStore = ((AlertDialog)dialog).findViewById(R.id.store);
                            String store = etStore.getText().toString();

                            //Create a new ListItem with data from the dialog-fields:
                            ListItem li = new ListItem(ID, title, price,cb, store);

                            //Pass the object on to MainActivity:
                            mListener.onDialogPositiveClick(NewItemDialogFragment.this, li, isNewCopy);
                    }
                })
                .setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        if(state == DIALOG_EDIT) //Only delete existing items
                            mListener.onDialogNegativeClick(NewItemDialogFragment.this, ID);
                    }
                })
                .setView(dialogView);

        //Create the AlertDialog object and return it:
        return builder.create();
    }
}
