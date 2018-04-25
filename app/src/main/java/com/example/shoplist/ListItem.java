package com.example.shoplist;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by sune on 3/14/18.
 */

@Entity
public class ListItem
{
    //Attributes:
    @PrimaryKey
    public int id;

    public String title;

    public double price;

    public boolean checkBox;

    public String store;

    public ListItem()
    {
    }

    public ListItem(int anId, String aTitle, double aPrice, boolean aCheckBox, String aStore)
    {
        id = anId;
        title = aTitle;
        price = aPrice;
        checkBox = aCheckBox;
        store = aStore;
    }

    @Ignore
    public boolean hidden = false; //Determines whether item should be displayed in recyclerview. It is not relevant for the database
}
