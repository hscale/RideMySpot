package activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.gson.GsonFactory;
import com.w3m.ridemyspot.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import account.SessionManager;
import adapter.InfoSpotAdapter;
import database.SQLiteSpot;
import entity.Rmsendpoint;
import entity.model.CollectionResponseSpots;
import model.MultiSpinner;
import model.MultiSpinner.MultiSpinnerListener;
import model.Spot;

public class MapActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback, OnMapLongClickListener, OnMapClickListener, OnMarkerClickListener, MultiSpinnerListener, OnClickListener, OnInfoWindowClickListener{

	private GoogleMap mMap;
	private LocationManager mLocationManager;
	private SessionManager mSessionManager;

	private Marker markerAddSpot;
	private Marker markerUser;
	
	private MultiSpinner multiSpinner;
	private MenuItem mRefresh;
	
	private SQLiteSpot mDatabaseSpot;
	
	public List<Spot> mListSpot = new ArrayList<>();
	public HashMap<String, Spot> mHmSpot = new HashMap<>();
	
	private AdView mAdView;

    public static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 101;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.maps);

		mSessionManager = new SessionManager(this);
		if(!mSessionManager.isLoggedIn()){
			Intent intent = new Intent(this, SplashScreenActivity.class);
		    startActivity(intent);
		    finish();
		}

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Log.d("", "");
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            findLocation();
        }

		//Maps Initialization
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
		
		//Filter Initialization
		List<String> Liste = Arrays.asList(getResources().getStringArray(R.array.maps_filter_list));
		multiSpinner = (MultiSpinner) findViewById(R.id.map_multi_spinner);
		multiSpinner.setItems(Liste, getResources().getString(R.string.text_all_spot), this);
		
		findViewById(R.id.map_location).setOnClickListener(this);

		mDatabaseSpot = new SQLiteSpot(this);
		
		// Recherchez AdView comme ressource et chargez une demande.
	    mAdView = (AdView)this.findViewById(R.id.map_adView);
	    AdRequest adRequest = new AdRequest.Builder().build();
	    mAdView.loadAd(adRequest);
	    
	    new ListSpots(this).execute();
	}
	
	private void populateMap() {
		
		mDatabaseSpot.OpenDB();
		if(!mListSpot.isEmpty()){
			mListSpot.clear();
		}
		mListSpot = mDatabaseSpot.getListSpot();
		mDatabaseSpot.CloseDB();
		
		mMap.clear();    //On peut aussi jouer sur la visibilité... optimise le fais de pas avoir a recreer les marker!!
							//Du coup un populatemap() pour tout les points et un filtermarker() pour le filtre (le faire sur le clicklistener du filtre!)
		
		List<String> type = Arrays.asList(multiSpinner.getSelectedItem().toString().split(", "));
		if(type.contains(getResources().getString(R.string.text_all_spot))){
			type = Arrays.asList(getResources().getStringArray(R.array.maps_filter_list));
		}
			
		if(!mHmSpot.isEmpty()){
			mHmSpot.clear();
		}
		
		for (Spot spot : mListSpot) {
			//If the spot is in the type scope
			if(containsAny(type, spot.getStringTypes(), spot.isFavorite())){
				//We add it to the map and retrieve his ID
				String markerID = mMap.addMarker(new MarkerOptions()
	    			.position(spot.getPosition())
	    			.title(spot.getName())
	    			.snippet(String.valueOf(spot.getID()))
	    			.icon(BitmapDescriptorFactory.fromResource(R.drawable.map))
				).getId();
				//We set to the spot the marker's ID to retrieve the connection when it will hit
				mHmSpot.put(markerID,spot);
			}
		}
		
		//Redraw user's last know location
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            Location userLocation = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if(userLocation != null){
                markerUser = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin))
                );
            }
        }
		

		mMap.setInfoWindowAdapter(new InfoSpotAdapter(this, mHmSpot));
	}
	
	private boolean containsAny(List<String> type, List<String> stringTypes, boolean fav) {
		for (String text : type){
			if(stringTypes.contains(text) || fav)
				return true;
		}
		return false;
	}

	@Override
	protected void onStart() {
		super.onStart();

//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED){
//            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
//        }
	}
	
	@Override
	protected void onStop() {
        if (ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED && mLocationManager != null){
            mLocationManager.removeUpdates(this);
        }
		super.onStop();
	}
	
	@Override
	protected void onPause() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && mLocationManager != null){
            mLocationManager.removeUpdates(this);
        }
	    mAdView.pause();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		removeExistingAddSpot();
		if(mListSpot.size() != 0){
			populateMap();
		}
		mAdView.resume();
	}

	@Override
	protected void onDestroy() {
		mAdView.destroy();
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_maps, menu);	
		mRefresh = menu.findItem(R.id.menu_refresh_spot);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh_spot:
			new ListSpots(this).execute();
			return true;
		case R.id.menu_list:
			Intent intent = new Intent(MapActivity.this, ListSpotActivity.class);
			startActivity(intent);
			return true;
		case R.id.menu_add:
			add_spot(mMap.getCameraPosition().target);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;

        mMap.setOnMapLongClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
	}
	
	@Override
	public void onMapLongClick(LatLng marker) {
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(200);
		removeExistingAddSpot();
			
		markerAddSpot = mMap.addMarker(new MarkerOptions()
        .position(marker)
        .icon(BitmapDescriptorFactory
        .fromResource(R.drawable.map)));
		markerAddSpot.setDraggable(true);
	}
	
	@Override
	public void onMapClick(LatLng point) {
		removeExistingAddSpot();				
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		marker.hideInfoWindow();
		if(marker.equals(markerAddSpot))
			add_spot(marker.getPosition());
		else
			removeExistingAddSpot();
		return false;
	}

	private void add_spot(LatLng position){
		Intent intent = new Intent(MapActivity.this, AddSpotActivity.class);
		intent.putExtra(AddSpotActivity.EXTRA_POSITION, position);
		startActivity(intent);
	}

	@Override
	public void onItemsSelected(boolean[] selected) {
		//Permet de rafraichir l'affichage selon le type de spot selectionner
		populateMap();
	}

    private void findLocation(){
        //Location Initialization

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    findLocation();

                } else {
                    Log.d("","");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

	@Override
	public void onLocationChanged(Location location) {
		if(markerUser!=null)
			if(markerUser.isVisible())
				markerUser.remove();
		
		markerUser = mMap.addMarker(new MarkerOptions()
        	.position(new LatLng(location.getLatitude(), location.getLongitude()))
        	.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin))
        );
		
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()),14));
		//mMap.animateCamera(CameraUpdateFactory.zoomTo(14), 2000, null);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.removeUpdates(this);
        }
	}

	@Override
	public void onProviderDisabled(String provider) {}

	@Override
	public void onProviderEnabled(String provider) {}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.map_location:
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED){
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
                }
			break;
		}
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		Intent intent = new Intent(MapActivity.this, SpotActivity.class);
		intent.putExtra(SpotActivity.EXTRA_SPOT, mHmSpot.get(marker.getId()));
		startActivity(intent);
	}
	
	public void removeExistingAddSpot(){
		if(markerAddSpot!=null)
			if(markerAddSpot.isVisible())
				markerAddSpot.remove();
	}
	
	private class ListSpots extends AsyncTask<Void, Void, CollectionResponseSpots>{
		private Context m_context;
		
		public ListSpots(Context context){
			m_context = context;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if(mRefresh != null){
				((AnimationDrawable)mRefresh.getIcon()).start();
			}
		}
		
		@Override
		protected CollectionResponseSpots doInBackground(Void... params) {
			CollectionResponseSpots spots = null;
			try{
				Rmsendpoint.Builder builder = new Rmsendpoint.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), null);
				Rmsendpoint service = builder.build();
				spots = service.listSpots().setPIdUser(Long.parseLong(mSessionManager.getUserDetails().get(SessionManager.KEY_ID))).execute();
			} catch (Exception e){
				Log.d(getString(R.string.maps_loading_spot_error_log), e.getMessage(), e);
			}
			return spots;
		}
		
		@Override
		protected void onPostExecute(CollectionResponseSpots spots) {
			super.onPostExecute(spots);
			
			if(mRefresh != null){
				((AnimationDrawable)mRefresh.getIcon()).stop();
				mRefresh.setIcon(ContextCompat.getDrawable(m_context, R.drawable.map_loader));
			}
			
			if(spots != null && spots.getItems() != null){
			    
				mDatabaseSpot.OpenDB();
				mDatabaseSpot.insertListEntitySpots(spots.getItems());
				mDatabaseSpot.CloseDB();
				
				populateMap();
			} else {
				Toast.makeText(m_context, getString(R.string.maps_loading_spot_error), Toast.LENGTH_SHORT).show();
			}
		}
	}

}


