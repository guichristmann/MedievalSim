package org.christmann.medievalsim;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class LoginScreen extends AppCompatActivity {

    Button loginButton, newAccountButton;
    EditText emailET, pwdET;

    MediaPlayer mp;

    // Tag used to log
    private static final String TAG = "LoginScreen";

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);

        mp = MediaPlayer.create(getApplicationContext(), R.raw.darksouls);  // <3
        mp.start();

        setupUI();

        mAuth = FirebaseAuth.getInstance();         // Gets Authentication reference

        mAuthListener = new FirebaseAuth.AuthStateListener(){
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // Character is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // Character is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    //@Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    // Setup UI elements and button listeners
    public void setupUI(){
        loginButton = (Button) findViewById(R.id.loginButton);
        newAccountButton = (Button) findViewById(R.id.newAccountButton);
        emailET = (EditText) findViewById(R.id.emailEditText);
        pwdET = (EditText) findViewById(R.id.passwordEditText);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailET.getText().toString();
                if (email.equals("")){
                    // email is empty
                    emailET.requestFocus();
                } else {
                    String password = pwdET.getText().toString();
                    if(password.equals("")) {
                        pwdET.requestFocus();
                    } else {
                        login(email, password);
                    }
                }
            }
        });

        newAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               gotoCreateAccountScreen();
            }
        });

    }

    // Firebase Login Authentication
    public void login(String email, String password){
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail", task.getException());
                            Toast.makeText(LoginScreen.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginScreen.this, "Login successful.",
                                    Toast.LENGTH_SHORT).show();
                            gotoGameScreen();
                        }

                    }
                });
    }

    // Calls GameScreen
    public void gotoGameScreen(){
        mp.stop();  // stops music
        Intent gameScreenIntent = new Intent(this, GameScreen.class);
        startActivity(gameScreenIntent);
    }

    // Calls NewAccountScreen
    public void gotoCreateAccountScreen(){
        mp.stop();  // stops music
        Intent createAccountIntent = new Intent(this, NewAccountScreen.class);
        startActivity(createAccountIntent);
    }
}
