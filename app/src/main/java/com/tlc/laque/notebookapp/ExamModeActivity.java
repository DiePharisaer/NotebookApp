package com.tlc.laque.notebookapp;


import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class ExamModeActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    SQLiteHelper helper;
    Context context;
    Resources res;

    String DEBUG = "DEBUG";
    Button exitButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        res = getResources();
        context = getApplicationContext();
        helper = new SQLiteHelper(this);
        db = helper.getReadableDatabase();

        setContentView(R.layout.exam_mode);

        exitButton = (Button) findViewById(R.id.buttonExamNegative);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

//        startExamMode();


    }

    public void startExamMode(){
        // Each text segment can be in an array, and you go back and forward
        // Animations between transitions, fade in, fade out
        // Layout: exam_mode.xml
        // String
        // Extra: Store results and show best in settings list
        // Extra, choose number of questions with a number selector
        //---------------
        //TODO Open new activity or fullscreen dialog
        //TODO First text view, explaining the rules, button to continue
        //TODO If count > 0, it gets each word and its translation, else, show noWords string
        //TODO randomly picks a word, store its translation and find 3 random translations and store
        //TODO if correct is picked, add 1, if not, 0, and next, until end (if count > 10, do only 10)
        //TODO when finished, show final text with % score from counter and total

//        Random r = new Random();
//        int i1 = r.nextInt(80 - 65) + 65;
//        This gives a random integer between 65 (inclusive) and 80 (exclusive), one of 65,66,...,78,79
//        int i1 = r.nextInt(max - min + 1) + min;



    }

    public void makeToast(String input){
        Toast toast = Toast.makeText(context, input, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void debug(Object obj){
        Log.d(DEBUG, obj.toString());
    }
}
