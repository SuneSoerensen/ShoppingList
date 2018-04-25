package com.example.shoplist;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;


public class DelAllVisFragment extends DialogFragment
{
    //Interface to MainActivity:
    public interface DelAllVisInterface
    {
        void deleteCurrentlyVisible();
    }
    DelAllVisInterface delAllVisinterface;

    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        try
        {
            delAllVisinterface = (DelAllVisInterface)activity;
        }
        catch(ClassCastException e)
        {
            throw new ClassCastException(activity.toString() + "Must implement DelAllVisInterface");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater li = getActivity().getLayoutInflater();
        final View dialogView = li.inflate(R.layout.del_all_vis_dialog, null);

        builder.setMessage(R.string.del_all_vis_dialog_text)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        delAllVisinterface.deleteCurrentlyVisible(); //Delete all visible items
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id) { } //A tap on "Cancel"-button just closes the dialog
                })
                .setView(dialogView);

        //Create the AlertDialog object and return it:
        return builder.create();
    }

}
