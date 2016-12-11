package org.christmann.medievalsim;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class EncounterScreen extends AppCompatActivity {

    public final static String TAG = "EncouterScreen";

    Character player;
    Enemy enemy;

    // ui stuff
    private Button attackBtn, runBtn;
    private TextView playerName, playerHPText, enemyName, enemyHPText;
    private ProgressBar playerHPBar, enemyHPBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encounter_screen);

        Log.i(TAG, "Entered EncounterScreen");

        player = (Character) getIntent().getSerializableExtra("player");
        enemy = (Enemy) getIntent().getSerializableExtra("enemy");

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

        playerName.setText(player.getDisplayName() + " Lv. " + player.getLevel());
        playerHPText.setText(player.getCurrentHP() + "/" + player.getMaxhp());
        enemyName.setText(enemy.getName() + " Lv. " + enemy.getLevel());
        enemyHPText.setText(enemy.getCurrentHP() + "/" + enemy.getMaxhp());

        playerHPBar = (ProgressBar) findViewById(R.id.player_hp_bar);
        enemyHPBar = (ProgressBar) findViewById(R.id.enemy_hp_bar);

        enemyHPBar.setMax(enemy.getMaxhp());
        enemyHPBar.setProgress(enemy.getCurrentHP());
        playerHPBar.setMax(player.getMaxhp());
        playerHPBar.setProgress(10);
    }

    private void attack(){

    }

    private void run(){

    }
}
