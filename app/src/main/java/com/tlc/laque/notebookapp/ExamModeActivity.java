package com.tlc.laque.notebookapp;


import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class ExamModeActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    SQLiteHelper helper;
    Context context;
    Resources res;
    RadioGroup radioGroup;

    String DEBUG = "DEBUG";
    Button exitButton;
    Button nextButton;
    TextView title;
    int[] providedAnswersArray;
    int[] correctAnswersArray;
    int questionNumber;
    ArrayList wordsShownArrayList;
    ArrayList wordsIndexShownArrayList;
    int numMultipleChoiceAns = 4;
    int[] wordsAnsweredIndex;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        res = getResources();
        context = getApplicationContext();
        helper = new SQLiteHelper(this);
        db = helper.getReadableDatabase();

        setContentView(R.layout.exam_mode);

        title = (TextView) findViewById(R.id.titleExamView);
        title.setText(res.getString(R.string.examModeWelcome));

        radioGroup = (RadioGroup) findViewById(R.id.radioGroupQuestions);

        exitButton = (Button) findViewById(R.id.buttonExamNegative);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        nextButton = (Button) findViewById(R.id.buttonExamPositive);
        nextButton.setText(res.getString(R.string.start));
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(radioGroup.getCheckedRadioButtonId() == -1)
                    makeToast(res.getString(R.string.noAnswerSelected));
                else {
                    if(radioGroup.getCheckedRadioButtonId() == R.id.answer1){
                        nextQuestion(1, 0);
                    }else if(radioGroup.getCheckedRadioButtonId() == R.id.answer2){
                        nextQuestion(2,0);
                    }else if(radioGroup.getCheckedRadioButtonId() == R.id.answer3){
                        nextQuestion(3,0);
                    }else if(radioGroup.getCheckedRadioButtonId() == R.id.answer4){
                        nextQuestion(4,0);
                    }

                }
            }
        });

        startExamMode();
    }

    public void startExamMode(){
        // Each text segment can be in an array
        // Animations between transitions, fade in, fade out
        // Layout: exam_mode.xml
        // Extra: Store results and show best in settings list
        // Extra, choose number of questions with a number selector
        //---------------
        //TODO randomly picks a word, store its translation and find 3 random translations and store
        //TODO if correct is picked, add 1, if not, 0, and next, until end (if count > 10, do only 10)
        //TODO when finished, show final text with % score from counter and total

//        This gives a random integer between 65 (inclusive) and 80 (exclusive), one of 65,66,...,78,79
//        int i1 = r.nextInt(max - min + 1) + min;

        TextView message = (TextView) findViewById(R.id.wordExamView);
        message.setText("");

        Cursor cursor = db.rawQuery("SELECT * FROM " + helper.tableName , null);

        if(cursor.getCount()<numMultipleChoiceAns){
            message = (TextView) findViewById(R.id.wordExamView);
            message.setText(res.getString(R.string.noWordsExam));
            radioGroup.setVisibility(View.GONE);
            return;
        }

        int numberOfQuestions = cursor.getCount()/numMultipleChoiceAns;

        ArrayList possibleAnswersPerQuestion = new ArrayList(numMultipleChoiceAns);
        ArrayList possibleAnswersIndexPerQuestion = new ArrayList(numMultipleChoiceAns);
        wordsShownArrayList = new ArrayList(numberOfQuestions);
        wordsIndexShownArrayList = new ArrayList<>(numberOfQuestions);

        Random r = new Random();

        for(int i = 0; i < numberOfQuestions; i++) {
            for (int j = 0; j < numMultipleChoiceAns; j++) {
                int pos = r.nextInt(cursor.getCount());
                while (possibleAnswersIndexPerQuestion.contains(pos)) {
                    pos = r.nextInt(cursor.getCount());
                }
                cursor.moveToPosition(pos);
                possibleAnswersPerQuestion.add(cursor.getString(cursor.getColumnIndexOrThrow(helper.TRANSLATED_WORD)));
                possibleAnswersIndexPerQuestion.add(pos);
            }
            wordsShownArrayList.add(possibleAnswersPerQuestion);
            wordsIndexShownArrayList.add(possibleAnswersIndexPerQuestion);

            possibleAnswersPerQuestion = new ArrayList(numMultipleChoiceAns);
            possibleAnswersIndexPerQuestion = new ArrayList(numMultipleChoiceAns);
        }

        String[] wordsPickedToAsk = new String[numberOfQuestions];
        int[] wordsPickedIndex = new int[numberOfQuestions];
        int[] wordsPickedIndexInDB = new int[numberOfQuestions];



        for (int i = 0; i < numberOfQuestions; i++) {       //Selecting a random word from questions
            int pos = r.nextInt(numMultipleChoiceAns);         // to become the asked one
            wordsPickedIndex[i] = pos;
            wordsPickedIndexInDB[i] = (int)(((ArrayList)(wordsIndexShownArrayList.get(i))).get(pos));
            cursor.moveToPosition(wordsPickedIndexInDB[i]);
            wordsPickedToAsk[i] = cursor.getString(cursor.getColumnIndexOrThrow(helper.ORIGINAL_WORD));
        }

//        TODO show as possible answers per question
//        debug(wordsShownArrayList.get());
        //TODO this is what to compare the answers to when getting radio button selected
        //toArrays.toString(wordsPickedIndex)); in wordsAnsweredIndex
        //TODO this is what to display in the top textview as question
        // Arrays.toString(wordsPickedToAsk));



//        providedAnswersArray = new int[numberOfQuestions];
//        correctAnswersArray = new int[numberOfQuestions];
//        ArrayList wordsPickedIndex = new ArrayList();
//
//        for (int i = 0; i < numberOfQuestions; i++) {
//            int pos = r.nextInt(cursor.getCount());
//            while(wordsPickedIndex.contains(pos))
//                pos = r.nextInt(cursor.getCount());
//
//            wordsPickedIndex.add(pos); //Positions of correct word in DB
//            cursor.moveToPosition(pos);
//            wordsPicked[i] = cursor.getString(cursor.getColumnIndexOrThrow(helper.ORIGINAL_WORD));
//        }

        cursor.close();
    }

    public void nextQuestion(int answer, int questionNumberIndex){
        providedAnswersArray[questionNumberIndex] = answer;

        //TODO update UI
    }

    public void makeToast(String input){
        Toast toast = Toast.makeText(context, input, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void debug(Object obj){
        Log.d(DEBUG, obj.toString());
    }
}
