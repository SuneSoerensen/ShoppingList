package com.example.shoplist;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

/**
 * Created by sune on 3/14/18.
 */

@Database(entities = {ListItem.class}, version = 3)
public abstract class AppDataBase extends RoomDatabase
{
    private static AppDataBase INSTANCE;
    public abstract ListItemDao listItemDao();

    public static AppDataBase getAppDataBase(Context context)
    {
        //Only create a new database if the is none already (follow singleton-pattern):
        if(INSTANCE == null)
        {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                    AppDataBase.class, "item-database").allowMainThreadQueries().build();
        }

        return INSTANCE;
    }

    public static void deleteInstance()
    {
        INSTANCE = null;
    }
}
