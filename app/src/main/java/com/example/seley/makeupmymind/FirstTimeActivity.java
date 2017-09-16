package com.example.seley.makeupmymind;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class FirstTimeActivity extends AppCompatActivity {

    private Button mYesButton;
    private Button mNoButton;
    private TextView mQuestion;
    private int questionsAnswered = 0;
    private char[] userResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        setContentView(R.layout.activity_first_time);

        mYesButton = (Button) findViewById(R.id.yes_button);
        mNoButton = (Button) findViewById(R.id.no_button);
        mQuestion = (TextView) findViewById(R.id.question_text_view);
        userResponse = new char[2];

        mYesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQuestion.setText(R.string.second_question);
                userResponse[questionsAnswered] = 'y';
                questionsAnswered++;
                if (questionsAnswered==2)
                {
                    Intent intent = new Intent(FirstTimeActivity.this, RecommendationsActivity.class);
                    intent.putExtra("answers", String.valueOf(userResponse));
                    startActivityForResult(intent, 1);
                }
            }
        });

        mNoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQuestion.setText(R.string.second_question);
                userResponse[questionsAnswered] = 'n';
                questionsAnswered++;
                if (questionsAnswered==2)
                {
                    Intent intent = new Intent(FirstTimeActivity.this, RecommendationsActivity.class);
                    intent.putExtra("answers", String.valueOf(userResponse));
                    startActivityForResult(intent, 1);
                }
            }
        });
    }
}
