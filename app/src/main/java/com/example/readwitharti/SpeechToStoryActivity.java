package com.example.readwitharti;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class SpeechToStoryActivity extends AppCompatActivity {
    private TextView title;
    private String[] userTalks;
    private String[] splitStory;
    private ArrayList<String> wrongWords;
    private String strStory;
    private String strTitle;
    private TextView story;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private double userScore;
    private ImageView micButton;
    private Boolean isClicked;
    private Chronometer timer;
    private SpeechRecognizer speechRecognizer;
    private long totalTime;
    private boolean isRunning;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private TextToSpeech textToSpeech;
    private String wrongToDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_to_story);
        title = findViewById(R.id.textTitle);
        story = findViewById(R.id.textStory);
        micButton = findViewById(R.id.imageView20);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        wrongWords = new ArrayList<>();
        mAuth = FirebaseAuth.getInstance();
        isClicked = false;
        timer = findViewById(R.id.chronometer);
        timer.setFormat("Time: %s");
        timer.setBase(SystemClock.elapsedRealtime());
        if (mAuth.getUid() == null) {
            Toast.makeText(SpeechToStoryActivity.this, "Please Login First", Toast.LENGTH_LONG).show();
            Intent intentt = new Intent(this, WelcomeActivity.class);
            startActivity(intentt);
            finish();
        }
        Intent intent = getIntent();
        strTitle = intent.getStringExtra("chosenTitle");
        title.setText(strTitle);
        getStoryFromDatabase();

        micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isClicked = true;
                speechToTextMethod();
            }
        });
    }

    public void onClickBackk(View v) {
        Intent intent1 = new Intent(SpeechToStoryActivity.this, MainActivity.class);
        startActivity(intent1);
        finish();
    }

    public void saveDatas() {
        if (isClicked) {
            mDatabase.child("Users").child(mAuth.getUid()).child("Stories").child(strTitle).child("time").setValue(totalTime);
            mDatabase.child("Users").child(mAuth.getUid()).child("Stories").child(strTitle).child("score").setValue(userScore);
            Toast.makeText(SpeechToStoryActivity.this, "Story Saved", Toast.LENGTH_SHORT).show();
            mDatabase.child("Users").child(mAuth.getUid()).child("isStoryRead").setValue(true);
            if (!wrongWords.isEmpty()) {
                mDatabase.child("Users").child(mAuth.getUid()).child("Stories").child(strTitle).child("wrong words").setValue(wrongToDB);
            }
        } else {
            Toast.makeText(SpeechToStoryActivity.this, "Please Read Story First", Toast.LENGTH_LONG).show();
        }
    }

    private void getStoryFromDatabase() {
        mDatabase.child("Stories").child(strTitle).child("story").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                strStory = dataSnapshot.getValue().toString();
                story.setText(strStory);
                splitStory = strStory.split(" ");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SpeechToStoryActivity.this, "!Error! Try again later", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void speechToTextMethod() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Start to Read");
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(SpeechToStoryActivity.this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(SpeechToStoryActivity.this, "Listening...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {
                if (error == 3 || error == 9) {
                    Toast.makeText(SpeechToStoryActivity.this, "Please Give Record Permission to Google and ReadWithArti",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SpeechToStoryActivity.this, "Speech Not Detected", Toast.LENGTH_SHORT).show();
                }
                System.out.println("Error" + error);
                micButton.setImageResource(R.drawable.no_sound);
                pauseTimer();
                resetTimer();
                totalTime = 0;
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String userTalk = data.get(0);
                System.out.println(userTalk);
                micButton.setImageResource(R.drawable.no_sound);
                userTalks = userTalk.split(" ");
                pauseTimer();
                compareTalks();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
        speechRecognizer.startListening(intent);
        resetTimer();
        startTimer();
        micButton.setImageResource(R.drawable.microphone);
    }

    private void compareTalks() {
        wrongWords.clear();
        for (int i = 0; i < splitStory.length; i++) {
            try {
                if (!Objects.equals(userTalks[i], splitStory[i])) {
                    wrongWords.add(splitStory[i]);
                }
            } catch (ArrayIndexOutOfBoundsException exception) {
                break;
            }
        }
        if (!wrongWords.isEmpty()) {
            wrongToDB = "";
            for (int i = 0; i < wrongWords.size(); i++) {
                System.out.println(wrongWords.get(i));
                wrongToDB += wrongWords.get(i) + ",";
            }
        }
        final DecimalFormat df = new DecimalFormat("0.00");
        userScore = 100.0 * (((double) splitStory.length - (double) wrongWords.size()) / ((double) splitStory.length));
        userScore = Double.parseDouble(df.format(userScore));
        createNewDialog();
    }

    private void startTimer() {
        if (!isRunning) {
            timer.setBase(SystemClock.elapsedRealtime() - totalTime);
            timer.start();
            isRunning = true;
        }
    }

    private void pauseTimer() {
        if (isRunning) {
            timer.stop();
            totalTime = SystemClock.elapsedRealtime() - timer.getBase();
            isRunning = false;
        }
    }

    private void resetTimer() {
        timer.setBase(SystemClock.elapsedRealtime());
        totalTime = 0;
    }

    public void createNewDialog() {
        dialogBuilder = new AlertDialog.Builder(this);
        View popUp = getLayoutInflater().inflate(R.layout.popup, null);
        TextView viewScore = popUp.findViewById(R.id.textView16);
        viewScore.setText(String.valueOf(userScore));
        TextView viewTime = popUp.findViewById(R.id.textView28);
        viewTime.setText(String.valueOf(totalTime));
        Button savee = popUp.findViewById(R.id.button14);
        Button listenAll = popUp.findViewById(R.id.button12);
        Button listenWrongs = popUp.findViewById(R.id.button13);
        ImageView back = popUp.findViewById(R.id.imageView32);
        dialogBuilder.setView(popUp);
        dialog = dialogBuilder.create();
        dialog.show();

        savee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDatas();
                dialog.dismiss();
            }
        });
        listenAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textToSpeech(strStory);
            }
        });
        listenWrongs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textToSpeech(wrongToDB);
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    public void textToSpeech(String word) {
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.getDefault());
                    textToSpeech.setSpeechRate(0.7f);
                    textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onPause();
    }
}