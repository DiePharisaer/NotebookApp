package com.tlc.laque.notebookapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

//TODO
//sign out option
//import/export csv
//alphabetical order
//Exam mode
// textToSpeech
//SpeechToText
// save last letter for when close app, delete something, change orientation

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient mGoogleApiClient;
    String DEBUG = "DEBUG";
    int RC_SIGN_IN = 10102;
    GoogleSignInAccount acct;
    Resources res;
    ListView alphabetView;
    ListView wordsListView;
    Context context;
    LinearLayout wordsContainer;
    String word1;
    String word2;
    private SQLiteDatabase db;
    SQLiteHelper helper;
    ArrayAdapter<String> wordListAdapter;
    ArrayAdapter<String> alphabetAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        res = getResources();
        context = getApplicationContext();
        helper = new SQLiteHelper(this);
        db = helper.getReadableDatabase();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        wordsContainer = (LinearLayout) findViewById(R.id.layout_for_text);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Add a word pair");
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);

                final EditText input1 = new EditText(context);
                input1.setHint(res.getString(R.string.originalWord));
                input1.setFilters(new InputFilter[] {
                        new InputFilter() {
                            @Override
                            public CharSequence filter(CharSequence cs, int start,
                                                       int end, Spanned spanned, int dStart, int dEnd) {
                                if(cs.toString().matches("[a-zA-Z ]+")){
                                    return cs;
                                }
                                return "";
                            }
                        }
                });


                final EditText input2 = new EditText(context);
                input2.setHint(res.getString(R.string.translatedWord));
                input2.setFilters(new InputFilter[] {
                        new InputFilter() {
                            @Override
                            public CharSequence filter(CharSequence cs, int start,
                                                       int end, Spanned spanned, int dStart, int dEnd) {
                                if(cs.toString().matches("[a-zA-Z ]+")){
                                    return cs;
                                }
                                return "";
                            }
                        }
                });

                layout.addView(input1);
                layout.addView(input2);
                builder.setView(layout);

                builder.setPositiveButton(res.getString(R.string.addDialog), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        word1 = input1.getText().toString();
                        word2 = input2.getText().toString();

                        if(!existsInDb(word1)) {
                            insertRecord(word1, word2);
                            updateWordList((word1.substring(0, 1)).toLowerCase());
                        }else{
                            makeToast("This word exists already!");
                        }
                    }
                });
                builder.setNegativeButton(res.getString(R.string.cancelDialog), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        alphabetAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, res.getStringArray(R.array.alphabet));
        alphabetView = (ListView) findViewById(R.id.listView_for_alphabet);
        alphabetView.setAdapter(alphabetAdapter);

        alphabetView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                updateWordList(((TextView) view).getText().toString().toLowerCase());
//                TypedValue typedValue = new TypedValue();
//                getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
//                int color = typedValue.data;
//                view.setBackgroundColor(color);
//
//                ((TextView) view).setTextColor(Color.WHITE);
                debug((res.getStringArray(R.array.alphabet)[parent.getPositionForView(view)]).toLowerCase());
            }
        });

        wordListAdapter = new ArrayAdapter<>(this, R.layout.row_container);
        wordsListView = (ListView) findViewById(R.id.listView_for_words);
        wordsListView.setAdapter(wordListAdapter);

        wordsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String fullString = ((TextView) view).getText().toString();
                String[] parts = fullString.split("-->");
                final String firstWord = parts[0];
                final String secondWord = parts[1];

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Edit a word pair");
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);

                final EditText input1 = new EditText(context);
                input1.setText(firstWord.toLowerCase());
                input1.setHint(res.getString(R.string.originalWord));
                input1.setFilters(new InputFilter[] {
                        new InputFilter() {
                            @Override
                            public CharSequence filter(CharSequence cs, int start,
                                                       int end, Spanned spanned, int dStart, int dEnd) {
                                if(cs.toString().matches("[a-zA-Z ]+")){
                                    return cs;
                                }
                                return "";
                            }
                        }
                });


                final EditText input2 = new EditText(context);
                input2.setText(secondWord.toLowerCase());
                input2.setHint(res.getString(R.string.translatedWord));
                input2.setFilters(new InputFilter[] {
                        new InputFilter() {
                            @Override
                            public CharSequence filter(CharSequence cs, int start,
                                                       int end, Spanned spanned, int dStart, int dEnd) {
                                if(cs.toString().matches("[a-zA-Z ]+")){
                                    return cs;
                                }
                                return "";
                            }
                        }
                });

                layout.addView(input1);
                layout.addView(input2);
                builder.setView(layout);

                builder.setPositiveButton(res.getString(R.string.addDialog), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        word1 = input1.getText().toString();
                        word2 = input2.getText().toString();
                        updateRecord(word1, word2, findWordPosition(firstWord));
                        updateWordList((word1.substring(0,1)).toLowerCase());
                    }
                });
                builder.setNegativeButton(res.getString(R.string.cancelDialog), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });

        wordsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String fullString = ((TextView) view).getText().toString();
                String[] parts = fullString.split("-->");
                final String firstWord = parts[0];
                final String secondWord = parts[1];

                deleteRecord(findWordPosition(firstWord));
                updateWordList("a"); //TODO update with last letter
                return false;
            }
        });

        updateWordList("a");

        debugDB();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        debug("CONNECTION FAILED SIGN IN");
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }else if (id == R.id.action_signin){
            startSignInProcess();
        }else if(id == R.id.delete_db){
            deleteAllRecords();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void startSignInProcess(){
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    public void handleSignInResult(GoogleSignInResult result) {
        Log.d(DEBUG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            acct = result.getSignInAccount();
            updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }

    public void updateUI(boolean state){
        TextView email = (TextView) findViewById(R.id.emailID);
        TextView name = (TextView) findViewById(R.id.nameID);
        ImageView profilePic = (ImageView) findViewById(R.id.profilePic);
        if(state){
            email.setText(acct.getEmail());
            name.setText(acct.getDisplayName());
            debug("Is picture null? " + String.valueOf(acct.getPhotoUrl() == null));
            if(acct.getPhotoUrl() != null){
                profilePic.setImageURI(acct.getPhotoUrl());
            }
        } else{
            email.setText(Resources.getSystem().getString(R.string.defaultEmail));
            name.setText(Resources.getSystem().getString(R.string.defaultName));
            profilePic.setImageDrawable(res.getDrawable(android.R.drawable.sym_def_app_icon, this.getTheme()));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    public void debugDB(){
        Cursor cursor = db.rawQuery("SELECT * FROM " + helper.tableName, null);

        if(cursor.getCount()>0) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                word1 = cursor.getString(cursor.getColumnIndexOrThrow(helper.ORIGINAL_WORD));
                word2 = cursor.getString(cursor.getColumnIndexOrThrow(helper.TRANSLATED_WORD));
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(helper.COLUMN_ID));
                String firstLetter = cursor.getString(cursor.getColumnIndexOrThrow(helper.FIRST_LETTER));

                debug("entry id: " + id);
                debug("original word: " + word1);
                debug("translated word: " + word2);
                debug("first letter: " + firstLetter);

            }
        }
        cursor.close();
    }

    public void updateWordList(String selectedLetter){
        wordsListView.setAdapter(null);

        Cursor cursor;

        if(selectedLetter.equals("#"))
            cursor = db.rawQuery("SELECT * FROM " + helper.tableName, null);
        else
            cursor = db.rawQuery("SELECT * FROM " + helper.tableName + " WHERE " + helper.FIRST_LETTER + " = '" + selectedLetter + "'", null);

        //cycle and add the textviews
        if(cursor.getCount()>0) {
            String[] rowStringArray = new String[cursor.getCount()];

            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                word1 = cursor.getString(cursor.getColumnIndexOrThrow(helper.ORIGINAL_WORD));
                word2 = cursor.getString(cursor.getColumnIndexOrThrow(helper.TRANSLATED_WORD));

                rowStringArray[i] = word1 + "-->" + word2;
            }

            wordListAdapter = new ArrayAdapter<>(this, R.layout.row_container, rowStringArray);
            wordsListView.setAdapter(wordListAdapter);
            wordListAdapter.notifyDataSetChanged();
        }

        cursor.close();
    }



    public void insertRecord(String originalWord, String translatedWord) {
        db.execSQL("INSERT INTO " + helper.tableName + "(" + helper.ORIGINAL_WORD + "," + helper.TRANSLATED_WORD + "," + helper.FIRST_LETTER + ") VALUES('" + originalWord + "','" + translatedWord + "','" + originalWord.substring(0,1).toLowerCase() + "')");
    }

    public void updateRecord(String originalWord, String translatedWord, int idToDelete) {
        db.execSQL("update " + helper.tableName + " set " + helper.ORIGINAL_WORD + " = '" + originalWord + "', " + helper.TRANSLATED_WORD + " = '" + translatedWord + "', " + helper.FIRST_LETTER + " = '" + originalWord.substring(0,1).toLowerCase() + "' where " + helper.COLUMN_ID + " = '" + idToDelete + "'");
    }

    public void deleteRecord(int idToDelete) {
        db.execSQL("delete from " + helper.tableName + " where " + helper.COLUMN_ID + " = '" + idToDelete + "'");
    }

    public void deleteAllRecords(){
        db.execSQL("delete from " + helper.tableName);
        updateWordList("a");
    }

    public int findWordPosition(String word){
        Cursor cursor;
        cursor = db.rawQuery("SELECT * FROM " + helper.tableName + " WHERE " + helper.ORIGINAL_WORD + " = '" + word + "'", null);
        if(cursor.getCount()>0)
            cursor.moveToFirst();

        int idPos = cursor.getInt(cursor.getColumnIndexOrThrow(helper.COLUMN_ID));
        cursor.close();
        return idPos;
    }

    public boolean existsInDb(String wordToCheck){
        Cursor cursor;
        boolean existsRepeat;
        cursor = db.rawQuery("SELECT * FROM " + helper.tableName + " WHERE " + helper.ORIGINAL_WORD + " = '" + wordToCheck + "'", null);
        if(cursor.getCount()>0)
            existsRepeat = true;
        else
            existsRepeat = false;

        cursor.close();

        return existsRepeat;
    }

    public void makeToast(String input){
        Toast toast = Toast.makeText(context, input, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void debug(Object obj){
        Log.d(DEBUG, obj.toString());
    }
}
