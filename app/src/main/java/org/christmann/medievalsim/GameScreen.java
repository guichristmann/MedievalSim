package org.christmann.medievalsim;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class GameScreen extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    final static int MIN_ENCOUNTER_DISTANCE = 100;

    private boolean characterLoaded = false; // when true playerCharacter has been read from DB successfully

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private String characterName;   // will hold character name for database referencing
    private Character playerCharacter;

    private List<Enemy> nearbyEnemies;  // list that holds enemies that are drawn in the screen

    // Firebase stuff
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser user;
    private FirebaseDatabase database;
    private DatabaseReference enemiesRef, usersRef, charactersRef;

    // Markers
    Marker pMarker; // player marker used to update player location

    // Tag used to log
    private static final String TAG = "GameScreen";

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_screen);

        Log.e(TAG, "Starting GameScreen Activity");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Database initialization
        database = FirebaseDatabase.getInstance();

        playerCharacter = new Character();
        //noinspection Convert2Diamond
        nearbyEnemies = new ArrayList<Enemy>();

        firebaseAuthInit();

        googleMapsAPISetup();

        setupDBEnemiesListener();
    }

    // Initializes Firebase Authentication and get User that is logged in
    public void firebaseAuthInit() {
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    getCharacter();
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
    }

    private void googleMapsAPISetup(){
        Log.e(TAG, "Initializing Maps API");
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)        // in milliseconds
                .setFastestInterval(100); // in milliseconds
    }

    // gets character name from inside User in the database
    public void getCharacter(){
        // gets character from inside user in the database
        usersRef = database.getReference("users/" + user.getUid() + "/character");

        // using this because we just need to read once
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                characterName = (String) dataSnapshot.getValue();   // gets character name from database
                Log.e(TAG, "characterName: " + characterName);
                if (characterName != null) {
                    charactersRef = database.getReference("characters/" + characterName);
                    charactersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            playerCharacter = dataSnapshot.getValue(Character.class);

                            // playerCharacter can't be null because it is used to store the location
                            // when google maps is initialing, and firstTimeSetup should be false so
                            // it is called only once, when we get playerCharacterInformation for
                            // the first time
                            if (playerCharacter != null){
                                Log.e(TAG, "########## Got player character ########");
                                playerCharacter.setDisplayName(characterName); // gambiarra do diabo
                                characterLoaded = true;
                                updateUserStatus(true);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e(TAG, "Failed to read value.", databaseError.toException());
                        }
                    });

                    Toast.makeText(getApplicationContext(), "Welcome " +
                            characterName, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });
    }

    // changes user status to online when logged in
    // ** IMPORTANT **
    // The first time this function is called is inside getCharacter called from firebaseAuthInit.
    // this is necessary to make sure we don't get a null value, which happens because firebase
    // reads values asynchronously
    public void updateUserStatus(boolean onlineStatus){
        playerCharacter.setOnline(onlineStatus);    // sets playerCharacter Online to onlineStatus
        writeCharacterToDB();       // calls func to write changes to DB
    }

    // Reads list of enemies from database and set up listener for them
    public void setupDBEnemiesListener(){
        // Read from the database
        enemiesRef = database.getReference("enemies/");
        enemiesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e("Count " , ""+dataSnapshot.getChildrenCount());
                for(DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    Enemy enemy = postSnapshot.getValue(Enemy.class);
                    if (enemy != null){
                        Log.e(TAG, "setupDBEnemiestListener:newEnemy:" + enemy.getName());

                        nearbyEnemies.add(enemy);       // adds new enemy to enemy list
                    }
                }

                // after all enemies are got from database
                handleEnemies();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    // Changes player Marker to new position and updates position in playerCharacter
    private void handleNewPlayerLocation(Location location) {
        Log.d(TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        updateCharacterLocation(currentLatitude, currentLongitude);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

        // checks if an encounter has happened
        checkEncounter(location);

        pMarker.setTitle(playerCharacter.getDisplayName());    // sets marker name
        pMarker.setPosition(latLng); // updates marker with new position
    }

    // Checks if an encounter with a monster has happened and calls goToEncounterScreen
    private void checkEncounter(Location playerLocation){
        for(Enemy iEnemy : nearbyEnemies){
            Location monsterLocation = new Location(iEnemy.getName());
            monsterLocation.setLatitude(iEnemy.getLat());
            monsterLocation.setLongitude(iEnemy.getLng());

            float distance = playerLocation.distanceTo(monsterLocation);

            Log.e(TAG, "Monster: " + iEnemy.getName() + " Dist: " + distance);

            if (distance < MIN_ENCOUNTER_DISTANCE && characterLoaded){
                Toast.makeText(getApplicationContext(), "Encountered a " + iEnemy.getName(),
                        Toast.LENGTH_SHORT).show();
                goToEncounterScreen(iEnemy);
            }
        }
    }

    // calls EncounterScreen, durrr
    private void goToEncounterScreen(Enemy enemy){
        Intent encounterIntent = new Intent(this, EncounterScreen.class);
        encounterIntent.putExtra("player", playerCharacter); // sends player information
        encounterIntent.putExtra("enemy", enemy);   // sends enemy that will be fighted

        Log.e(TAG, "Going to EncounterScreen");
        Log.e(TAG, "Player Character is " + playerCharacter.getDisplayName());

        encounterIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(encounterIntent);
    }

    // Writes the current character object to database
    private void writeCharacterToDB(){
        charactersRef = database.getReference("characters/" + characterName);
        charactersRef.setValue(playerCharacter);        // writes character information to database
    }

    // Sets current position of playerCharacter and writes to the database
    private void updateCharacterLocation(double lat, double lng){
        playerCharacter.setLat(lat);
        playerCharacter.setLng(lng);
        writeCharacterToDB();
    }

    // This is called after enemies are added to nearbyEnemies list
    // The function calculates the distance of enemies to the player and draw markers
    // for them
    private void handleEnemies(){
        for(Enemy current_enemy : nearbyEnemies){
            LatLng monster_pos = new LatLng(current_enemy.getLat(), current_enemy.getLng());

            MarkerOptions options = new MarkerOptions()
                    .position(monster_pos)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.skeleton_icon))   // gets skeleton icon
                    .title(current_enemy.getName());

            mMap.addMarker(options);    // adds monsters marker
        }
    }

//    // Calculate distance between 2 LatLng objects
//    private double calculateDistance(LatLng startP, LatLng endP){
//        int Radius = 6371;// radius of earth in Km
//        double lat1 = startP.latitude;
//        double lat2 = endP.latitude;
//        double lon1 = startP.longitude;
//        double lon2 = endP.longitude;
//        double dLat = Math.toRadians(lat2 - lat1);
//        double dLon = Math.toRadians(lon2 - lon1);
//        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
//                + Math.cos(Math.toRadians(lat1))
//                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
//                * Math.sin(dLon / 2);
//        double c = 2 * Math.asin(Math.sqrt(a));
//        double valueResult = Radius * c;
//        double km = valueResult / 1;
//        DecimalFormat newFormat = new DecimalFormat("####");
//        int kmInDec = Integer.valueOf(newFormat.format(km));
//        double meter = valueResult % 1000;
//        int meterInDec = Integer.valueOf(newFormat.format(meter));
//        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
//                + " Meter   " + meterInDec);
//
//        return Radius * c;
//    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e(TAG, "Location services connected.");

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(),
                    "No permission for GPS or GPS not on High Accuracy Mode", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No permission.");
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        Location last_location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        Log.d(TAG, "Location: " + last_location);

        if (last_location != null) {
            // gets last position recorded
            LatLng lastPos =  new LatLng(last_location.getLatitude(), last_location.getLongitude());

            MarkerOptions options = new MarkerOptions()
                    .position(lastPos)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.player_icon)) // gets playerIcon
                    .title(characterName);

            pMarker = mMap.addMarker(options);  // creates new marker using last position recorded

            handleNewPlayerLocation(last_location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewPlayerLocation(location);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        updateUserStatus(true);    // sets character to online
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
        updateUserStatus(false);    // sets character to offline
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mAuth != null) {
            mAuth.addAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
        updateUserStatus(false);    // sets character to offline
        pMarker.remove();   // remove marker when connection is lost so we don't get duplicate
        // markers when the service is reconnected
    }

}
