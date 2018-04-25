package com.example.shoplist;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity implements NewItemDialogFragment.NoticeDialogListener, ListAdapter.checkboxInterface, StoreFilterDialogFragment.FilterStoreInterface, DelAllVisFragment.DelAllVisInterface
{
    //State of NewItemDialogFragment, to determine if an item is created or edited:
    public static final int DIALOG_CREATE = 0;
    public static final int DIALOG_EDIT   = 1;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //All items in shopping-list:
    private ArrayList<ListItem> listElements;

    //Qeues which the thread dbUpdater will look at and act correspondingly:
    private Queue<ListItem> toBeDeleted;
    public ReentrantLock lockTbd;

    private Queue<ListItem> hasBeenChanged;
    public ReentrantLock lockHbc;

    private Queue<ListItem> isNew;
    public ReentrantLock lockIn;

    private boolean updateDb = true; //Boolean to continue/stop dbUpdater-thread (must be true when app opens or resumes)
    private AppDataBase dataBase;

    Thread dbUpdater; //Maintains the database, such that it reflects the current listElements

    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        listElements = new ArrayList<>();

        //Create queues:
        toBeDeleted = new LinkedList<>();
        hasBeenChanged = new LinkedList<>();
        isNew = new LinkedList<>();

        //Create locks:
        lockTbd = new ReentrantLock();
        lockHbc = new ReentrantLock();
        lockIn  = new ReentrantLock();

        //Create database:
        dataBase = AppDataBase.getAppDataBase(this);

        //Get all items from database:
        final List<ListItem> list = dataBase.listItemDao().getAll();

        //Copy the gotten items into listElements:
        for(int i = 0; i < list.size(); i++)
        {
            listElements.add(list.get(i));
        }

        //Create thread to update database:
        dbUpdater = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                updateDatabase();
            }
        });

        //Set view of MainActivity:
        setContentView(R.layout.activity_main);

        //Drawer layout:
        mDrawerLayout = findViewById(R.id.drawer_layout);

        //Set the navigation for drawer:
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                       switch (menuItem.getItemId())
                        {
                            case R.id.nav_all:
                                showAll(); //Show all items
                                break;
                            case R.id.nav_checked:
                                showBasedOnCheck(true); //Show only checked items
                                break;
                            case R.id.nav_unchecked:
                                showBasedOnCheck(false);//Show only unchecked items
                                break;
                            case R.id.nav_store:
                                //Open a dialog, where user enters store, then show only items with matching store:
                                FragmentManager fm = getFragmentManager();
                                FragmentTransaction ft = fm.beginTransaction();
                                StoreFilterDialogFragment sfdf = new StoreFilterDialogFragment();
                                sfdf.show(ft, "store_filter_dialog");
                                break;
                            default:
                                break;
                        }
                        mDrawerLayout.closeDrawers(); //Close drawers whenever a filter is selected
                        return true;
                    }
                });

        //Top toolbar:
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);


        //Recyclerview:
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);

        //Use a linear layout manager:
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        //Create adapter:
        mAdapter = new ListAdapter(listElements,this);
        mRecyclerView.setAdapter(mAdapter);

        //Button for adding new items:
        FloatingActionButton fab = findViewById(R.id.floatingActionButton2);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle b = new Bundle();
                int state = DIALOG_CREATE;
                b.putInt("DialogState", state);
                b.putInt("id", getMinAvailableId());
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                NewItemDialogFragment nidf = new NewItemDialogFragment();
                nidf.setArguments(b);
                nidf.show(ft, "new_item_dialog");
            }
        });

        dbUpdater.start();
        updateRunningTotal();
    }

    //Invoked when tapping "Okay"-button in NewItemDialogFragment:
    public void onDialogPositiveClick(DialogFragment dialog, ListItem li, boolean liIsNew)
    {
        if(liIsNew) //If it's a new element
        {
            listElements.add(li.id,li);
            mAdapter.notifyDataSetChanged();
            updateRunningTotal();
            lockIn.lock();
            try
            {
                isNew.add(li); //Add to queue of new items
            }
            finally
            {
                lockIn.unlock();
            }
        }
        else
        {
            //If it's a changed element, find it in listElements and update it:
            for(int i = 0; i < listElements.size(); i++)
            {
                if(listElements.get(i).id == li.id)
                {
                    listElements.set(i, li);
                    break;
                }
            }

            lockHbc.lock();
            try
            {
                hasBeenChanged.add(li); // Add it to queue of changes items
            }
            finally
            {
                lockHbc.unlock();
            }

            mAdapter.notifyDataSetChanged();
            updateRunningTotal();
        }
    }

    //Invoked when tapping the "Delete"-button in NewItemDialogFragment:
    public void onDialogNegativeClick(DialogFragment dialog, int anId)
    {
        //Find the item in listElements:
        for(int i = 0; i < listElements.size(); i++)
        {
            if(listElements.get(i).id == anId)
            {
                lockTbd.lock();
                try
                {
                    toBeDeleted.add(listElements.get(i)); //Add it to queue of items to be deleted from database
                }
                finally
                {
                    lockTbd.unlock();
                }

                listElements.remove(i);
                mAdapter.notifyDataSetChanged();
                updateRunningTotal();
            }
        }
    }

    //Run by dbUpdater-thread:
    public void updateDatabase()
    {
        ListItem temp; //Temporary listItem

        while(updateDb)
        {
            //Check toBeDeleted-queue:
            lockTbd.lock();
            try
            {
                temp = toBeDeleted.poll();
                if(temp != null)
                {
                    dataBase.listItemDao().delete(temp);
                }
            }
            finally
            {
                lockTbd.unlock();
            }

            //Check hasBeenChanged-queue:
            lockHbc.lock();
            try
            {
                temp = hasBeenChanged.poll();
                if(temp != null)
                {
                    dataBase.listItemDao().update(temp);
                }
            }
            finally
            {
                lockHbc.unlock();
            }

            //Check isNew-queue:
            lockIn.lock();
            try
            {
                temp = isNew.poll();
                if(temp != null)
                {
                    dataBase.listItemDao().insert(temp);
                }
            }
            finally
            {
                lockIn.unlock();
            }

            //Sleep for 100ms to avoid updating database constantly:
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        //Stop updating database:
        updateDb = false;

        try
        {
            dbUpdater.join();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        //Cleanup database:
        AppDataBase.deleteInstance();
    }

    private int getMinAvailableId()
    {
        for(int i = 0; i < listElements.size(); i++)
        {
            if(listElements.get(i).id > i) //If there was an unused ID
            {
                return i;
            }
        }

        return listElements.size(); //If all ID's were taken, create a new
    }

    //Invoked by ListAdapter, when a checkbox is tapped:
    @Override
    public void checkboxHasChanged(int id, boolean val)
    {
        for(int i = 0; i < listElements.size(); i++)
        {
            if(listElements.get(i).id == id)
            {
                lockHbc.lock();
                try
                {
                    hasBeenChanged.add(listElements.get(i)); //Add to queue of changed items
                }
                finally
                {
                    lockHbc.unlock();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home: //If the drawer-menu-button is tapped
                mDrawerLayout.openDrawer(GravityCompat.START); //Open the drawer menu
                return true;
            case R.id.action_delete: //If the trashcan-icon is clicked:
                //deleteCurrentlyVisible();
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                DelAllVisFragment davf = new DelAllVisFragment();
                davf.show(ft, "del_all_vis_dialog");
                break;
            default:
                super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    /******************************
     *Filters (drawer-menu actions)
     ******************************/
    public void showBasedOnCheck(boolean checked)
    {
        for(int i = 0; i < listElements.size(); i++)
        {
            if(listElements.get(i).checkBox == checked)
                listElements.get(i).hidden = false;
            else
                listElements.get(i).hidden = true;
        }

        mAdapter.notifyDataSetChanged();
        updateRunningTotal();
    }

    public void showAll()
    {
        for(int i = 0; i < listElements.size(); i++)
        {
            listElements.get(i).hidden = false;
        }
        mAdapter.notifyDataSetChanged();
        updateRunningTotal();
    }

    public void filterStorePositiveBtn(String store)
    {
        for(int i = 0; i < listElements.size(); i++)
        {
            if(listElements.get(i).store.equals(store))
                listElements.get(i).hidden = false;
            else
                listElements.get(i).hidden = true;
        }
        mAdapter.notifyDataSetChanged();
        updateRunningTotal();
    }

    public void deleteCurrentlyVisible()
    {

        lockTbd.lock();
        try
        {
            for(int i = 0; i < listElements.size(); i++)
                if(!listElements.get(i).hidden)
                {
                    toBeDeleted.add(listElements.get(i));
                    listElements.remove(i);
                }
        }
        finally
        {
            lockTbd.unlock();
        }


        mAdapter.notifyDataSetChanged();
        updateRunningTotal();

    }


    //Update the running total of all visible items:
    public void updateRunningTotal()
    {
        double sum = 0.0;
        for(int i = 0 ; i < listElements.size(); i++)
        {
            if(!listElements.get(i).hidden)
                sum += listElements.get(i).price;
        }

        TextView et = findViewById(R.id.running_total);
        et.setText(getString(R.string.running_total) + ": " + Double.toString(sum));
    }

}
