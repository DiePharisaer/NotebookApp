package com.tlc.laque.notebookapp;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient mGoogleApiClient;
    String DEBUG = "DEBUG";
    int RC_SIGN_IN = 10102;
    GoogleSignInAccount acct;
    Resources res;
    ListView alphabetView;
    ListView wordsListView;
    ListView wordsListItemsView;
    Context context;
    String word1;
    String word2;
    private SQLiteDatabase db;
    SQLiteHelper helper;
    ArrayAdapter<String> wordListAdapter;
    ArrayAdapter<String> wordListAdapterItems;  
    ArrayAdapter<String> alphabetAdapter;
    TextToSpeech tts;
    TextToSpeech myTTS;
    int CHECK_CODE = 0101;
    String wordToSpeech;
    TextToSpeech.OnInitListener initListener;

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);       // Floating action button to add new word pair
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);       // Create Dialog to write words
                builder.setTitle("Add a word pair");
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);

                final EditText input1 = new EditText(context);
                input1.setHint(res.getString(R.string.originalWord));
                input1.setFilters(new InputFilter[] {
                        new InputFilter() {
                            @Override
                            public CharSequence filter(CharSequence cs, int start,
                                                       int end, Spanned spanned, int dStart, int dEnd) {    // Check that only letters are included in the words
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
                                if(cs.toString().matches("[a-zA-Z ]+")){                // Check that only letters are included in the words
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
                        word1 = input1.getText().toString();                        // Confirm words inputed
                        word2 = input2.getText().toString();

                        if(word1.isEmpty() || word2.isEmpty()) {
                            makeToast(res.getString(R.string.empty));
                            return;
                        }

                        if(!existsInDb(word1)) {
                            insertRecord(word1, word2);                             // If word doesn't exist in DB, add it
                            updateWordList((word1.substring(0, 1)).toLowerCase());  // Update UI
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


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)      // Sign in Google Account
                .requestEmail()
                .requestProfile()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        alphabetAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, res.getStringArray(R.array.alphabet));
        alphabetView = (ListView) findViewById(R.id.listView_for_alphabet);                                 //Alphabet on sidebar
        alphabetView.setAdapter(alphabetAdapter);

        alphabetView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                updateWordList(((TextView) view).getText().toString().toLowerCase());                       // Update UI on click of letter
            }
        });

        wordListAdapter = new ArrayAdapter<>(this, R.layout.row_container);                     // Words in notebook
        wordsListView = (ListView) findViewById(R.id.listView_for_words);
        wordsListView.setAdapter(wordListAdapter);

        wordsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {                    // Onclick for words, to edit them
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

                        if(word1.isEmpty() || word2.isEmpty()) {
                            makeToast(res.getString(R.string.empty));
                            return;
                        }

                        if(!existsInDb(word1)) {
                            updateRecord(word1, word2, findWordPosition(firstWord));
                            updateWordList((word1.substring(0,1)).toLowerCase());             // Update UI
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

        wordsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {           // Long click listener to to delete word
                String fullString = ((TextView) view).getText().toString();
                String[] parts = fullString.split("-->");
                final String firstWord = parts[0];

                deleteRecord(findWordPosition(firstWord));
                updateWordList((parts[0].substring(0,1)).toLowerCase()); //TODO update with last letter
                return false;
            }
        });

        wordListAdapterItems = new ArrayAdapter<>(this, R.layout.row_container);            // Array for textToSpeech function
        wordsListItemsView = (ListView) findViewById(R.id.listView_for_words2);
        wordsListItemsView.setAdapter(wordListAdapterItems);

        wordsListItemsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String fullString = ((TextView)(wordsListView.getChildAt(position))).getText().toString();
                String[] parts = fullString.split("-->");
                String firstWord = parts[0];

                wordToSpeech = firstWord;

                sendIntentTTS();                                                // Start TTS engine
            }
        });

        wordsListItemsView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {               // Long click to TTS the second word, the translation
                String fullString = ((TextView)(wordsListView.getChildAt(position))).getText().toString();
                String[] parts = fullString.split("-->");
                String secondWord = parts[1];

                wordToSpeech = secondWord;

                sendIntentTTS();
                return false;
            }
        });

        updateWordList("a");
        textToSpeech();
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

        if (id == R.id.action_signin){
            startSignInProcess();
        }else if(id == R.id.delete_db){
            deleteAllRecords();
        }else if(id == R.id.exam_mode){
            Intent intent = new Intent(MainActivity.this, ExamModeActivity.class);
            startActivity(intent);
        }else if(id == R.id.importCSV){
            importCSV();
        }else if(id == R.id.exportCSV){
            checkForWritePermission();
        }

        return super.onOptionsItemSelected(item);
    }


    private void startSignInProcess(){
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {                                                            // Check intent to Sign In
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }else if(requestCode == CHECK_CODE){                                                        // Check intent for text to speech
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // the user has the necessary data - create the TTS

                tts.speak(wordToSpeech, TextToSpeech.QUEUE_FLUSH, null,null);
            } else {
                // no data - install it now
                Intent installTTSIntent = new Intent();
                installTTSIntent
                        .setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }

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
        if (tts != null) {
                tts.stop();
                tts.shutdown();
            }

        super.onDestroy();
        db.close();
    }

    public void updateWordList(String selectedLetter){                          // Update word list UI, by cycling through DB entries and adding to list view
        wordsListView.setAdapter(null);
        wordsListItemsView.setAdapter(null);

        Cursor cursor;

        if(selectedLetter.equals("#"))
            cursor = db.rawQuery("SELECT * FROM " + helper.tableName, null);
        else
            cursor = db.rawQuery("SELECT * FROM " + helper.tableName + " WHERE " + helper.FIRST_LETTER + " = '" + selectedLetter + "'", null);

        //cycle and add the textviews
        if(cursor.getCount()>0) {
            String[] rowStringArray = new String[cursor.getCount()];
            String[] rowStringArrayItems = new String[cursor.getCount()];

            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                word1 = cursor.getString(cursor.getColumnIndexOrThrow(helper.ORIGINAL_WORD));
                word2 = cursor.getString(cursor.getColumnIndexOrThrow(helper.TRANSLATED_WORD));

                rowStringArray[i] = word1 + "-->" + word2;
                rowStringArrayItems[i] = res.getString(R.string.S);
            }

            wordListAdapter = new ArrayAdapter<>(this, R.layout.row_container, rowStringArray);
            wordsListView.setAdapter(wordListAdapter);
            wordListAdapter.notifyDataSetChanged();

            wordListAdapterItems = new ArrayAdapter<>(this, R.layout.row_container, rowStringArrayItems);
            wordsListItemsView.setAdapter(wordListAdapterItems);
            wordsListItemsView.deferNotifyDataSetChanged();
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

    public void importCSV() {
        File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);        // Import CSV file from default download location
        String line = "";
        debug("method");                                                                                        // Reads each line and split the words, adds them to DB
        try {
            FileReader file = new FileReader(exportDir + "/NotebookDatabaseImport.csv");
            BufferedReader buffer = new BufferedReader(file);
            debug("try");
            while ((line = buffer.readLine()) != null) {
                String[] str = line.split(",");
                debug("str " + str);

                insertRecord(str[0] ,str[1]);
            }
        } catch (Exception exc) {
            debug(exc);
        }

        updateWordList("#");
    }

    public boolean exportCSV() {                                                            // Cycle through DB and write each word to CSV file
        String state = Environment.getExternalStorageState();
        debug("1");
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            debug("2");
            return false;
        } else {
            debug("3");

            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!exportDir.exists()) {
                debug("4");
                exportDir.mkdirs();
            }
            File file;
            PrintWriter printWriter = null;
            debug("5");
            try {
                debug("6");

                file = new File(exportDir, "NotebookDatabase.csv");
                printWriter = new PrintWriter(new FileWriter(file));
                Cursor cursor = db.rawQuery("SELECT * FROM " + helper.tableName, null);
                printWriter.println("ORIGINAL WORD,TRANSLATED WORD");
                if (cursor.getCount() < 1)
                        return false;

                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    String orgWord = cursor.getString(cursor.getColumnIndexOrThrow(helper.ORIGINAL_WORD));
                    String transWord = cursor.getString(cursor.getColumnIndexOrThrow(helper.TRANSLATED_WORD));
                    String record;
                    record = (orgWord + "," + transWord);
                    printWriter.println(record);

                    if (!cursor.isLast())
                        cursor.moveToNext();
                }
                cursor.close();
            } catch (Exception exc) {
                debug("7");
                debug(exc.toString());
                return false;
            } finally {
                debug("8");
                if (printWriter != null) printWriter.close();
            }
            debug("9");
            return true;
        }
    }



    @TargetApi(Build.VERSION_CODES.M)
    public void checkForWritePermission(){                                      // In newer Android versions, ask for permission to write to storage
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            final boolean writeGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if(writeGranted){ //Already have both necessary permissions
                if (exportCSV()) {
                    makeToast(res.getString(R.string.exportSucc));
                } else
                    makeToast(res.getString(R.string.exportFail));
                return;
            }else{ //ask for permission
                boolean alreadyAskedForPermission  = shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(alreadyAskedForPermission){
                    requestLocPermissions(writeGranted);
                }else {
                    requestLocPermissions(writeGranted);
                }
            }
        }else{
            if (exportCSV()) {
                makeToast(res.getString(R.string.exportSucc));
            } else
                makeToast(res.getString(R.string.exportFail));
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void requestLocPermissions(boolean writeGranted){
        ArrayList<String> permissions = new ArrayList<>();
        if (!writeGranted)
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        requestPermissions(permissions.toArray(new String[permissions.size()]), 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            boolean permsGranted = true;
            for (int i = 0; i < grantResults.length; i++)
                permsGranted = permsGranted && (grantResults[i] == PackageManager.PERMISSION_GRANTED);
            if (permsGranted) {
                if (exportCSV()) {
                    makeToast(res.getString(R.string.exportSucc));
                } else
                    makeToast(res.getString(R.string.exportFail));
                return;
            } else {
                makeToast(res.getString(R.string.exportFail));
                return;
            }
        }
    }

    public void textToSpeech(){                                     // Text to speech method
        debug("text to speech");

        initListener = new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                debug("onInit in listener");
                if (status == TextToSpeech.SUCCESS) {
                    debug("success");
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported");
                    }

                } else {
                    debug("error");

                    Log.e("TTS", "Initilization Failed!");
                }
            }
        };

        tts = new TextToSpeech(this, initListener);

        debug("after initlistener");
    }

    public void sendIntentTTS(){                                                         // Send TTS intent while waiting engine set up
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, CHECK_CODE);
    }

    public void makeToast(String input){
        Toast toast = Toast.makeText(context, input, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void debug(Object obj){
        Log.d(DEBUG, obj.toString());
    }
}
