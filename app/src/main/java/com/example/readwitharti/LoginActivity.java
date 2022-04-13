package com.example.readwitharti;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    protected EditText mail;
    protected EditText password;
    protected FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        password = (EditText) findViewById(R.id.editTextPassword);
        mail = (EditText) findViewById(R.id.editTextMail);
        mAuth = FirebaseAuth.getInstance();
    }

    public void onClickLogin(View view) {
        String stringMail = mail.getText().toString();
        String stringPassword = password.getText().toString();
        System.out.println(stringMail);
        System.out.println(stringPassword);
        mAuth.signInWithEmailAndPassword(stringMail, stringPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "You are successfully logged in", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(loginIntent);
                } else {
                    Toast.makeText(LoginActivity.this, "!!!ERROR!!! Please try again later", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void onClickSignup(View view) {
        Intent signUpIntent = new Intent(LoginActivity.this, SignUpActivity.class);
        startActivity(signUpIntent);
    }

    public void onClickForgot(View view) {
        Intent signUpIntent = new Intent(LoginActivity.this, ForgotPassword.class);
        startActivity(signUpIntent);
    }
}