package org.christmann.medievalsim;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class NewAccountScreen extends AppCompatActivity implements View.OnClickListener {

    // Tag used to log messages
    public static final String TAG = "NewAccountScreen";

    // Firebase stuff
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser user;
    private FirebaseDatabase database;
    private DatabaseReference usersRef;

    EditText emailET, passwordET, repeatPwdET, displayNameET;
    Button newAccountBtn;

    // Character Display Name
    private String displayName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_account_screen);

        setupUI();

        setupAuthentication();
    }

    // Setup the buttons and text fields in the UI with their variables and set Listeners
    public void setupUI(){
        emailET = (EditText) findViewById(R.id.emailEditText);
        passwordET = (EditText) findViewById(R.id.passwordEditText);
        repeatPwdET = (EditText) findViewById(R.id.repeatPasswordEditText);
        displayNameET = (EditText) findViewById(R.id.characterNameEditText);

        newAccountBtn = (Button) findViewById(R.id.newAccountBtn);

        newAccountBtn.setOnClickListener(this);
    }

    // Setup Firebase authentication system
    public void setupAuthentication(){
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
    }

    // Firebase create account authentication
    public void createAccount(String email, String password){
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(NewAccountScreen.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(NewAccountScreen.this, "Created new account successfully.",
                                    Toast.LENGTH_SHORT).show();
                            gotoGameScreen();
                        }
                    }
                });
    }

    // Writes relevant information to database and calls GameScreen
    public void gotoGameScreen(){
        // Before changing activity the current user information needs to be written to the database
        user = FirebaseAuth.getInstance().getCurrentUser(); // getting current logged user
        database = FirebaseDatabase.getInstance();  // gets Database instance
        usersRef = database.getReference("users/" + user.getUid()); // points database ref to /users/
        if (user != null) {
            usersRef.setValue(user);    // Writes user information to database
        }

        // To create a link between the user account and the character information
        // a new database value called 'character' is created inside the user that was just written,
        // where the value is the reference at /characters/value
        usersRef = database.getReference("users/" + user.getUid() + "/character/");
        usersRef.setValue(displayName);

        // Creating the character information
        Character characterInformation = new Character(displayName);        // Creates a new instance of Character
        Log.d(TAG, "#### Writing character information to database ######");
        usersRef = database.getReference("characters/" + characterInformation.getDisplayName());    // gets database referemce at /characters/
        usersRef.setValue(characterInformation);        // writes characterInformation to database
        Log.d(TAG, "#### Character information written to database? ######");

        // Finally GameScreen is called
        Intent gameScreenIntent = new Intent(this, GameScreen.class);
        startActivity(gameScreenIntent);
    }

    // Gets information from text fields when the button is clicked
    @Override
    public void onClick(View v) {
        String email = emailET.getText().toString();
        String password = passwordET.getText().toString();
        String repeatPwd = repeatPwdET.getText().toString();
        displayName = displayNameET.getText().toString();

        if(password.equals(repeatPwd)){
            createAccount(email, password);
        } else {
            Toast.makeText(getApplicationContext(), "Passwords don't match", Toast.LENGTH_SHORT).show();
        }
    }
}
