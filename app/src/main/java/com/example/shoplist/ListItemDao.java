package com.example.shoplist;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

/**
 * Created by sune on 3/14/18.
 */
@Dao
public interface ListItemDao
{
    @Query("SELECT * FROM Listitem")
    List<ListItem> getAll();

    @Update
    void update(ListItem le);

    @Insert
    void insert(ListItem...listItems);

    @Delete
    void delete(ListItem listItem);
}
