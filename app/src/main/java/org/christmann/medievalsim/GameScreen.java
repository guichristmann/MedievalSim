package org.christmann.medievalsim;

import android.content.IntentSender;
import android.content.pm.PackageManager;
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

import java.util.List;

public class GameScreen extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private String characterName;   // will hold character name for database referencing

    private List<Enemy> nearbyEnemies;

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

        firebaseAuthInit();

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)        // in milliseconds
                .setFastestInterval(100); // in milliseconds

        setupDBListener();
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
                    Toast.makeText(getApplicationContext(), "Welcome " + user.getEmail(), Toast.LENGTH_LONG).show();
                    getCharacterName();
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
    }

    // gets character name from inside User in the database
    public void getCharacterName(){
        // gets character from inside user in the database
        usersRef = database.getReference("users/" + user.getUid() + "/character");

        // using this because we just need to read once
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                characterName = (String) dataSnapshot.getValue();   // gets character name from database
                Log.e(TAG, "characterName: " + characterName);
                if (characterName != null) {
                    updateUserStatus(true);
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
    // The first time this function is called is inside getCharacterName called from firebaseAuthInit.
    // this is necessary to make sure we don't get a null value, which happens because firebase
    // reads values asynchronously
    public void updateUserStatus(boolean onlineStatus){
        charactersRef = database.getReference("characters/" + characterName + "/online");
        charactersRef.setValue(onlineStatus);
    }

    // Reads list of enemies from database
    public void setupDBListener(){
        // Read from the database
        enemiesRef = database.getReference("enemies/");
        enemiesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e("Count " , ""+dataSnapshot.getChildrenCount());
                for(DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    Enemy enemy = postSnapshot.getValue(Enemy.class);
                    if (enemy != null){
                        System.out.println("############# " + enemy.getName());
                        LatLng latLng = new LatLng(enemy.getLat(), enemy.getLng());

                        handleNewEnemy(latLng, enemy.getName());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

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
        Log.i(TAG, "Location services connected.");

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
                    .title("You");

            pMarker = mMap.addMarker(options);  // creates new marker using last position recorded

            handleNewPlayerLocation(last_location);
        }
    }

    // Changes player Marker to new position and writes new position to database
    private void handleNewPlayerLocation(Location location) {
        Log.d(TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        updateLocationDB(currentLatitude, currentLongitude);

        pMarker.setPosition(latLng); // updates marker with new position
    }

    // Writes new location of the player to the database
    private void updateLocationDB(double lat, double lng){
        Log.d(TAG, "Writing new location to database.");

        if (characterName != null){
            charactersRef = database.getReference("characters/" + characterName + "/lat");  // points to latitude
            charactersRef.setValue(lat); // writes new latitude
            charactersRef = database.getReference("characters/" + characterName + "/lng");  // points to longiutde
            charactersRef.setValue(lng); // writes new longitude
        }
    }

    private void handleNewEnemy(LatLng latLng, String name) {
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(name);

        mMap.addMarker(options);
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
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
        updateUserStatus(true);    // sets character to offline
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
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
    }

}
