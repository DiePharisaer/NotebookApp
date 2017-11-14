package com.tlc.laque.notebookapp;


import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


public class ExamModeActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    SQLiteHelper helper;
    Context context;
    Resources res;
    RadioGroup radioGroup;
    RadioButton radio1;
    RadioButton radio2;
    RadioButton radio3;
    RadioButton radio4;

    String DEBUG = "DEBUG";
    Button exitButton;
    Button nextButton;
    TextView title;
    TextView message;
    int[] providedAnswersArray;
    ArrayList wordsShownArrayList;
    ArrayList wordsIndexShownArrayList;
    int numMultipleChoiceAns = 4;
    int currentQuestion = -1;
    String[] wordsPickedToAsk;
    int numberOfQuestions;
    int[] wordsPickedIndex;
    int testDone = 0;

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

        radioGroup = (RadioGroup) findViewById(R.id.radioGroupQuestions);                       // Initialize buttons, textviews, and radio group
        radio1 = (RadioButton) findViewById(R.id.answer1);
        radio2 = (RadioButton) findViewById(R.id.answer2);
        radio3 = (RadioButton) findViewById(R.id.answer3);
        radio4 = (RadioButton) findViewById(R.id.answer4);

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
            public void onClick(View v) {                                          // Listener of the 'Next' button
                currentQuestion++;

                if(testDone == 1) {                                                    // If test is finished, clear all and start from beginning
                    recreate();
                    radioGroup.clearCheck();
                    return;
                }

                if (radioGroup.getVisibility() == View.GONE) {                        // If radioGroup is GONE, it means that we have to start the test
                    radioGroup.setVisibility(View.VISIBLE);

                    message.setText(wordsPickedToAsk[currentQuestion]);
                    title.setText(res.getString(R.string.clickNext));               // Prepare the first question (currentQuestion) UI
                    nextButton.setText(res.getString(R.string.next));
                    radio1.setText((String)(((ArrayList)(wordsShownArrayList.get(currentQuestion))).get(0)));
                    radio2.setText((String)(((ArrayList)(wordsShownArrayList.get(currentQuestion))).get(1)));
                    radio3.setText((String)(((ArrayList)(wordsShownArrayList.get(currentQuestion))).get(2)));
                    radio4.setText((String)(((ArrayList)(wordsShownArrayList.get(currentQuestion))).get(3)));
                } else if(currentQuestion > numberOfQuestions){
                    testDone = 1;                                                    // If all the questions have been asked, then finish test by displaying result
                    radioGroup.setVisibility(View.GONE);

                    double result = calcuateResults();
                    message.setText(String.valueOf(result) + "%");
                    nextButton.setText(res.getString(R.string.newTest));
                    if(result < 60)
                        title.setText(res.getString(R.string.congrats) + " " + res.getString(R.string.bad));
                    else if(result < 75)
                        title.setText(res.getString(R.string.congrats) + " " + res.getString(R.string.satisfactory));
                    else if(result < 90)
                        title.setText(res.getString(R.string.congrats) + " " + res.getString(R.string.good));
                    else if(result < 99)
                        title.setText(res.getString(R.string.congrats) + " " + res.getString(R.string.excellent));
                    else
                        title.setText(res.getString(R.string.congrats) + " " + res.getString(R.string.impeccable));
                    return;
                } else {

                    if (radioGroup.getCheckedRadioButtonId() == -1) {                            // Make toast if no answer selected
                        makeToast(res.getString(R.string.noAnswerSelected));
                        currentQuestion--;
                    }else {

                        if (radioGroup.getCheckedRadioButtonId() == R.id.answer1) {             // Select change question when answered, and start method to show next question
                            nextQuestion(0, currentQuestion);
                        } else if (radioGroup.getCheckedRadioButtonId() == R.id.answer2) {
                            nextQuestion(1, currentQuestion);
                        } else if (radioGroup.getCheckedRadioButtonId() == R.id.answer3) {
                            nextQuestion(2, currentQuestion);
                        } else if (radioGroup.getCheckedRadioButtonId() == R.id.answer4) {
                            nextQuestion(3, currentQuestion);
                        }
                    }
                }
                // END ONCLICK
            }
        });

        startExamMode();
    }

    public void startExamMode(){                                                    // Set up of exam mode
        message = (TextView) findViewById(R.id.wordExamView);
        message.setText(res.getString(R.string.clickStart));

        radioGroup.setVisibility(View.GONE);

        Cursor cursor = db.rawQuery("SELECT * FROM " + helper.tableName , null);

        if(cursor.getCount()<numMultipleChoiceAns){                                 // Check if there's at least numMultipleChoiceAns words in notebook, to be able to ask 1 question
            message = (TextView) findViewById(R.id.wordExamView);
            message.setText(res.getString(R.string.noWordsExam));
            radioGroup.setVisibility(View.GONE);
            nextButton.setClickable(false);
            return;
        }

        nextButton.setClickable(true);
        numberOfQuestions = cursor.getCount()/numMultipleChoiceAns;               // Number of questions is equal to number of words in DB, divided by four and rounded down

        ArrayList possibleAnswersPerQuestion = new ArrayList(numMultipleChoiceAns);         // List of possible answers for each question
        ArrayList possibleAnswersIndexPerQuestion = new ArrayList(numMultipleChoiceAns);    // List of above, but indexes from DB position
        wordsShownArrayList = new ArrayList(numberOfQuestions);                             // Words shown on each question (temporal array)
        wordsIndexShownArrayList = new ArrayList<>(numberOfQuestions);                      // Words' indices shown on each question (temporal array)

        Random r = new Random();

        for(int i = 0; i < numberOfQuestions; i++) {
            for (int j = 0; j < numMultipleChoiceAns; j++) {
                int pos = r.nextInt(cursor.getCount());
                while (possibleAnswersIndexPerQuestion.contains(pos)) {                     // Create random number to get word from DB
                    pos = r.nextInt(cursor.getCount());
                }
                cursor.moveToPosition(pos);
                possibleAnswersPerQuestion.add(cursor.getString(cursor.getColumnIndexOrThrow(helper.TRANSLATED_WORD)));
                possibleAnswersIndexPerQuestion.add(pos);
            }
            wordsShownArrayList.add(possibleAnswersPerQuestion);                            // When done enough words for 1 question, move to next question (i) and cycle again (j)
            wordsIndexShownArrayList.add(possibleAnswersIndexPerQuestion);

            possibleAnswersPerQuestion = new ArrayList(numMultipleChoiceAns);
            possibleAnswersIndexPerQuestion = new ArrayList(numMultipleChoiceAns);
        }

        wordsPickedToAsk = new String[numberOfQuestions];                                  // Examined words, 1 per question
        wordsPickedIndex = new int[numberOfQuestions];                                     // Examined words' indices, 1 per question
        int[] wordsPickedIndexInDB = new int[numberOfQuestions];



        for (int i = 0; i < numberOfQuestions; i++) {       //Selecting a random word from questions
            int pos = r.nextInt(numMultipleChoiceAns);         // to become the asked one
            wordsPickedIndex[i] = pos;
            wordsPickedIndexInDB[i] = (int)(((ArrayList)(wordsIndexShownArrayList.get(i))).get(pos));
            cursor.moveToPosition(wordsPickedIndexInDB[i]);
            wordsPickedToAsk[i] = cursor.getString(cursor.getColumnIndexOrThrow(helper.ORIGINAL_WORD));     // Get original word which will be asked to translate
        }

        providedAnswersArray = new int[numberOfQuestions];

        cursor.close();
    }

    public void nextQuestion(int answer, int questionNumberIndex){              // Method to update UI for next question


        providedAnswersArray[questionNumberIndex-1] = answer;

        if(questionNumberIndex == providedAnswersArray.length){                 // If question is answered correctly, add 1 point
            currentQuestion++;
            nextButton.callOnClick();
            return;
        }

        message.setText(wordsPickedToAsk[questionNumberIndex]);
        radio1.setText((String)(((ArrayList)(wordsShownArrayList.get(questionNumberIndex))).get(0)));
        radio2.setText((String)(((ArrayList)(wordsShownArrayList.get(questionNumberIndex))).get(1)));
        radio3.setText((String)(((ArrayList)(wordsShownArrayList.get(questionNumberIndex))).get(2)));
        radio4.setText((String)(((ArrayList)(wordsShownArrayList.get(questionNumberIndex))).get(3)));

        radioGroup.clearCheck();
    }

    public double calcuateResults(){

        float counter = 0;
        for (int i = 0; i < providedAnswersArray.length; i++) {             // Add up all points and divide by total number of questions to obtain result
            if (providedAnswersArray[i] == wordsPickedIndex[i])
                counter++;
        }

        DecimalFormat twoDForm = new DecimalFormat("#.#");

        return Double.valueOf(twoDForm.format((counter/providedAnswersArray.length)*100));
    }

    public void makeToast(String input){
        Toast toast = Toast.makeText(context, input, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void debug(Object obj){
        Log.d(DEBUG, obj.toString());
    }
}
