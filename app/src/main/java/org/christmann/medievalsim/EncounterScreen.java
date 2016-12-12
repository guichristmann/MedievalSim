package org.christmann.medievalsim;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.ThreadLocalRandom;

public class EncounterScreen extends AppCompatActivity {

    public final static String TAG = "EncouterScreen";

    private final static int TURN_DELAY = 1000; // delay between changing turns of attack

    private Handler timeHandler = new Handler();

    Character player;
    Enemy enemy;

    boolean playerFirst = false; // True - player attacks first, False - Enemy attacks first

    // ui stuff
    private Button attackBtn, runBtn;
    private TextView playerName, playerHPText, enemyName, enemyHPText, statusText;
    private ProgressBar playerHPBar, enemyHPBar;

    // gotta have some sounds
    MediaPlayer hit, slash;

    // gambiarrrraa
    Intent mapsIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encounter_screen);

        Log.i(TAG, "Entered EncounterScreen");

        hit = MediaPlayer.create(getApplicationContext(), R.raw.hit);       // getting sound references
        slash = MediaPlayer.create(getApplicationContext(), R.raw.slash);

        player = (Character) getIntent().getSerializableExtra("player");    // gets player
        enemy = (Enemy) getIntent().getSerializableExtra("enemy");          // gets enemy

        playerFirst = player.getSpd() > enemy.getSpd();     // decides who attacks first

        Log.i(TAG, "playerFirst: " + playerFirst);

        setupUI();
    }

    // sets up button and text references on screen as well listeners for buttons
    private void setupUI(){
        attackBtn = (Button) findViewById(R.id.attack_button);
        runBtn = (Button) findViewById(R.id.run_button);

        attackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attack();
            }
        });

        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                run();
            }
        });

        playerName = (TextView) findViewById(R.id.player_name);
        playerHPText = (TextView) findViewById(R.id.player_hp_text);
        enemyName = (TextView) findViewById(R.id.enemy_name);
        enemyHPText = (TextView) findViewById(R.id.enemy_hp_text);
        statusText = (TextView) findViewById(R.id.status_textview);

        playerName.setText(player.getDisplayName() + " Lv. " + player.getLevel());
        playerHPText.setText(player.getCurrentHP() + "/" + player.getMaxhp());
        enemyName.setText(enemy.getName() + " Lv. " + enemy.getLevel());
        enemyHPText.setText(enemy.getCurrentHP() + "/" + enemy.getMaxhp());

        playerHPBar = (ProgressBar) findViewById(R.id.player_hp_bar);
        enemyHPBar = (ProgressBar) findViewById(R.id.enemy_hp_bar);

        enemyHPBar.setMax(enemy.getMaxhp());
        enemyHPBar.setProgress(enemy.getCurrentHP());
        playerHPBar.setMax(player.getMaxhp());
        playerHPBar.setProgress(player.getCurrentHP());
    }

    // Called everytime a turn has taken place in the combat to update bars and check if player or
    // enemy is dead
    private void update(){
        // sets player new health
        playerHPText.setText(player.getCurrentHP() + "/" + player.getMaxhp());
        playerHPBar.setProgress(player.getCurrentHP());

        // sets enemy health
        enemyHPText.setText(enemy.getCurrentHP() + "/" + enemy.getMaxhp());
        enemyHPBar.setProgress(enemy.getCurrentHP());
    }

    // This function is called when the attack button is clicked
    private void attack(){
        // prevents button being clicked repeatedly while combat is still being resolved
        attackBtn.setClickable(false);

        if(playerFirst){
            // player attacks first
            playerAttack();

            timeHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(enemy.getCurrentHP() <= 0){  // enemy is dead
                        statusText.setText("Enemy Defeated!");
                        enemy.setAlive(false);         // this kills the enemy
                        endCombat();
                    } else {
                        // then enemy attacks
                        enemyAttack();
                    }
                }
            }, TURN_DELAY);

            timeHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(player.getCurrentHP() <= 0){
                        statusText.setText("You Died!");
                        endCombat();
                    } else {    // player is alive
                        if(enemy.isAlive()){    // enemy is alive
                            attackBtn.setClickable(true);   // combat continues
                        }
                    }
                }
            }, TURN_DELAY);


        } else {
            // enemy attacks first
            enemyAttack();

            timeHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(player.getCurrentHP() <= 0){  // player is dead
                        statusText.setText("You Died!");
                        endCombat();
                    } else {
                        // then player attacks
                        playerAttack();
                    }
                }
            }, TURN_DELAY);

            timeHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(enemy.getCurrentHP() <= 0){
                        statusText.setText("Enemy Defeated!");
                        enemy.setAlive(false);         // this kills the enemy
                        endCombat();
                    } else {    // enemy is alive
                        if(player.getCurrentHP() > 0){    // player is alive
                            attackBtn.setClickable(true); // combat continues
                        }
                    }
                }
            }, TURN_DELAY);
        }
    }

    @SuppressLint("NewApi")
    public void playerAttack(){
        // calculates player damage
        int damage = ThreadLocalRandom.current().nextInt(player.getLevel(), player.getAtk()); // only works in API 21+
        // calculates enemy damage absorption
        int damageAbsorption = ThreadLocalRandom.current().nextInt(enemy.getLevel(), enemy.getDef());
        int hit_damage = Math.max(0, damage - damageAbsorption);
        // determines new enemy HP
        int damageResult = enemy.getCurrentHP() - hit_damage;

        String hitMessage = player.getDisplayName() + " hits for " + hit_damage + " damage";
        statusText.setText(hitMessage);     // display status message
        slash.start();  // plays slash sound

        enemy.setCurrentHP(damageResult);
        update();   // updates health bars
    }

    @SuppressLint("NewApi")
    public void enemyAttack(){
        // calculates player damage
        int damage = ThreadLocalRandom.current().nextInt(enemy.getLevel(), enemy.getAtk()); // only works in API 21+
        // calculates enemy damage absorption
        int damageAbsorption = ThreadLocalRandom.current().nextInt(player.getLevel(), player.getDef());
        int hit_damage = Math.max(0, damage - damageAbsorption);
        // determines new enemy HP
        int damageResult = player.getCurrentHP() - hit_damage;

        String hitMessage = enemy.getName() + " hits for " + hit_damage + " damage";
        statusText.setText(hitMessage);     // display status message
        hit.start();  // plays hit sound

        player.setCurrentHP(damageResult);
        update(); // updates health bars
    }

    // This is called when the run button is clicked
    private void run(){

    }

    // deals with whatever the fuck happened in the combat and goes back to the map screen
    private void endCombat(){

        // got rekt
        if(player.getCurrentHP() <= 0){
            // for now this doesn't do anything
            // you got lucky scrub
        }

        Log.e(TAG, "enemy.isAlive():" + enemy.isAlive());
        // congratulations, you are a winrar
        if(!enemy.isAlive()) {
            Log.e(TAG, "Removing enemy from database");

            // Banish this weak enemy from the database
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference weak_enemy = database.getReference("enemies/" + enemy.getName());
            weak_enemy.removeValue();
        }

        mapsIntent = new Intent(this, GameScreen.class);
        timeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(mapsIntent);
            }
        }, 3000);
    }
}
