package activity;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.PointTarget;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import account.SessionManager;
import adapter.InfoSpotAdapter;
import adapter.NavigationDrawerAdapter;
import database.SQLiteSpot;
import de.hdodenhof.circleimageview.CircleImageView;
import entity.Rmsendpoint;
import entity.model.CollectionResponseSpots;
import entity.model.Users;
import model.MultiSpinner;
import model.MultiSpinner.MultiSpinnerListener;
import model.Spot;
import utils.ToolbarHomeTarget;

public class MapActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback, OnMapLongClickListener, OnMapClickListener, OnMarkerClickListener, MultiSpinnerListener, OnClickListener, OnInfoWindowClickListener, OnShowcaseEventListener, AdapterView.OnItemClickListener{

	private GoogleMap mMap;
	private LocationManager mLocationManager;
	private SessionManager mSessionManager;

	private Marker markerAddSpot;
	private Marker markerUser;
	
	private MultiSpinner multiSpinner;
	private MenuItem mRefresh;

	private SQLiteSpot mDatabaseSpot;

	private Toolbar mMapToolbar;
	
	public List<Spot> mListSpot = new ArrayList<Spot>();
	public HashMap<String, Spot> mHmSpot = new HashMap<String, Spot>();
	
	private AdView mAdView;

    public static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 101;

	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.maps);

		mSessionManager = new SessionManager(this);
		if(!mSessionManager.isLoggedIn()){
			Intent intent = new Intent(this, SplashScreenActivity.class);
			startActivity(intent);
			finish();
		} else {
			mDatabaseSpot = new SQLiteSpot(this);
			new ListSpots(this).execute();

			mMapToolbar = (Toolbar) findViewById(R.id.map_toolbar);
			setSupportActionBar(mMapToolbar);

			mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
			CircleImageView circleImageView = (CircleImageView) mDrawerLayout.findViewById(R.id.left_drawer_user_image);
			if(mSessionManager.getUserDetails().get(SessionManager.KEY_TYPE).equals(getString(R.string.text_roller))){
				circleImageView.setImageDrawable(getResources().getDrawable(R.drawable.icon_roller));
			} else if(mSessionManager.getUserDetails().get(SessionManager.KEY_TYPE).equals(getString(R.string.text_skate))){
				circleImageView.setImageDrawable(getResources().getDrawable(R.drawable.icon_skate));
			} else {
				circleImageView.setImageDrawable(getResources().getDrawable(R.drawable.icon_velo));
			}
			mDrawerList = (ListView) findViewById(R.id.left_drawer_list);
			mDrawerList.setAdapter(new NavigationDrawerAdapter(this));
			mDrawerList.setOnItemClickListener(this);

			PackageInfo pInfo;
			((TextView)findViewById(R.id.left_drawer_user_name)).setText(mSessionManager.getUserDetails().get(SessionManager.KEY_NAME));
			try {
				pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
				((TextView)findViewById(R.id.left_drawer_version)).setText(pInfo.versionName);
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}

			mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mMapToolbar, R.string.text_valider, R.string.text_annuler) {

				public void onDrawerClosed(View view) {
					super.onDrawerClosed(view);
					invalidateOptionsMenu();
				}

				public void onDrawerOpened(View drawerView) {
					super.onDrawerOpened(drawerView);
					invalidateOptionsMenu();
				}
			};
			// Set the drawer toggle as the DrawerListener
			mDrawerLayout.setDrawerListener(mDrawerToggle);
		}

        //Maps Initialization
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Filter Initialization
        List<String> Liste = Arrays.asList(getResources().getStringArray(R.array.maps_filter_list));
        multiSpinner = (MultiSpinner) findViewById(R.id.map_multi_spinner);
        multiSpinner.setItems(Liste, getResources().getString(R.string.text_all_spot), this);

        // Recherchez AdView comme ressource et chargez une demande.
        mAdView = (AdView)this.findViewById(R.id.map_adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


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

	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	final static int HELP_DRAWER = 0;
	final static int HELP_REFRESHMENU = 1;
	final static int HELP_LISTMENU = 2;
	final static int HELP_ADDSPOTMENU = 3;
	final static int HELP_FILTER = 4;
	final static int HELP_GEOLOC = 5;
	final static int HELP_ADDSPOT = 6;

	private int mHelpStep = 0;
	private boolean isTutorialFinished = false;
	private boolean hasToShowLongClick = false;

	private void showTutorial(){
		if(mHelpStep < 6){
			constructShowcaseView(mHelpStep);
			mHelpStep++;
		} else {
			isTutorialFinished = true;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setPositiveButton(getString(R.string.text_valider), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					hasToShowLongClick = true;
				}
			});
			builder.setNegativeButton(getString(R.string.text_annuler), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
				}
			});
			builder.setTitle(getString(R.string.maps_help_popup_title));
			builder.setMessage(getString(R.string.maps_help_popup_addspot_description));
			builder.setIcon(R.drawable.ic_launcher);
			AlertDialog alertDialog = builder.create();
			alertDialog.setCanceledOnTouchOutside(false);
			alertDialog.show();
		}
	}

	private void constructShowcaseView(int step){
		Target target;
		int title, description;

		switch (step){
			case HELP_DRAWER :
				target = new ToolbarHomeTarget(mMapToolbar, R.id.menu_add);
				title = R.string.maps_help_home_title;
				description = R.string.maps_help_home_description;
				break;
			case HELP_GEOLOC :
				target = new ViewTarget(R.id.map_location, MapActivity.this);
				title = R.string.maps_help_geoloc_title;
				description = R.string.maps_help_geoloc_description;
				break;
			case HELP_FILTER :
				target = new ViewTarget(R.id.map_multi_spinner, MapActivity.this);
				title = R.string.maps_help_filter_title;
				description = R.string.maps_help_filter_description;
				break;
			case HELP_REFRESHMENU:
				target = new ViewTarget(mMapToolbar.findViewById(R.id.menu_refresh_spot));
				title = R.string.maps_help_refresh_title;
				description = R.string.maps_help_refresh_description;
				break;
			case HELP_ADDSPOTMENU :
				target = new ViewTarget(mMapToolbar.findViewById(R.id.menu_add));
				title = R.string.maps_help_addspot_title;
				description = R.string.maps_help_addspotmenu_description;
				break;
			case HELP_LISTMENU :
				target = new ViewTarget(mMapToolbar.findViewById(R.id.menu_list));
				title = R.string.maps_help_listmenu_title;
				description = R.string.maps_help_listmenu_description;
				break;
			case HELP_ADDSPOT :
				Projection projection = mMap.getProjection();
				target = new PointTarget(projection.toScreenLocation(markerAddSpot.getPosition()).x, projection.toScreenLocation(markerAddSpot.getPosition()).y + mMapToolbar.getHeight());
				title = R.string.maps_help_addspot_title;
				description = R.string.maps_help_addspot_description;
				break;
			default:
				return;
		}

		new ShowcaseView.Builder(this)
				.setTarget(target)
				.setContentTitle(title)
				.setStyle(R.style.CustomShowcaseTheme)
				.setContentText(description)
				.hideOnTouchOutside()
				.setShowcaseEventListener(this)
				.build();
	}

	@Override
	public void onBackPressed() {
		if(mDrawerLayout.isDrawerOpen(GravityCompat.START)){
			mDrawerLayout.closeDrawers();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public void onShowcaseViewHide(ShowcaseView showcaseView) {
		if(!isTutorialFinished){
			showTutorial();
		}
		if(mHelpStep == HELP_ADDSPOT && markerAddSpot !=null && markerAddSpot.isVisible()){
			markerAddSpot.setVisible(false);
		}
	}

	@Override
	public void onShowcaseViewDidHide(ShowcaseView showcaseView) {}

	@Override
	public void onShowcaseViewShow(ShowcaseView showcaseView) {}

	@Override
	public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {}

	private void populateMap() {

		mDatabaseSpot.OpenDB();
		if(!mListSpot.isEmpty()){
			mListSpot.clear();
		}
		mListSpot = mDatabaseSpot.getListSpot();
		mDatabaseSpot.CloseDB();

		if(mMap != null)
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
			if((containsAny(type, spot.getStringTypes()) || showFavorite(type, spot.isFavorite()))){
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

	private boolean showFavorite(List<String> type, boolean isFavorite) {
		if(!type.contains(getString(R.string.text_favorite))){
			return false;
		} else {
			return isFavorite;
		}
	}

	private boolean containsAny(List<String> type, List<String> spotTypes) {
		for (String text : type){
			if(spotTypes.contains(text))
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

		/*
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		menu.findItem(R.id.menu_refresh_spot).setVisible(!drawerOpen);
		menu.findItem(R.id.menu_list).setVisible(!drawerOpen);
		menu.findItem(R.id.menu_add).setVisible(!drawerOpen);
		*/

		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
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

        findViewById(R.id.map_location).setOnClickListener(this);
	}
	
	@Override
	public void onMapLongClick(LatLng marker) {
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(200);
		removeExistingAddSpot();

		//TODO move camera to zoom
			
		markerAddSpot = mMap.addMarker(new MarkerOptions()
        .position(marker)
        .icon(BitmapDescriptorFactory
        .fromResource(R.drawable.map)));
		markerAddSpot.setDraggable(true);

		if(hasToShowLongClick){
			constructShowcaseView(HELP_ADDSPOT);
			hasToShowLongClick = false;
		}
	}
	
	@Override
	public void onMapClick(LatLng point) {
		removeExistingAddSpot();
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
        marker.hideInfoWindow();
		if(marker.equals(markerAddSpot)) {
            add_spot(marker.getPosition());
        } else if(marker.getPosition().equals(mMap.getCameraPosition().target)){
            intentSpot(marker);
        } else {
            removeExistingAddSpot();
        }
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

		if(!mSessionManager.tutorialMapHasShown()){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setPositiveButton(getString(R.string.text_valider), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					showTutorial();
				}
			});
			builder.setNegativeButton(getString(R.string.text_annuler), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					isTutorialFinished = true;
				}
			});
			builder.setTitle(getString(R.string.app_name));
			builder.setMessage(getString(R.string.maps_help_popup_description));
			builder.setIcon(R.drawable.ic_launcher);
			AlertDialog alertDialog = builder.create();
			alertDialog.setCanceledOnTouchOutside(false);
			alertDialog.show();
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
		
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 14));
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
		intentSpot(marker);
	}

    private void intentSpot(Marker marker){
        Intent intent = new Intent(MapActivity.this, SpotActivity.class);
        intent.putExtra(SpotActivity.EXTRA_SPOT, mHmSpot.get(marker.getId()));
        startActivity(intent);
    }
	
	public void removeExistingAddSpot(){
		if(markerAddSpot!=null)
			if(markerAddSpot.isVisible())
				markerAddSpot.remove();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch (position){
			case 0 :
				alertDialogProfil();
				break;
			//case 1 :
			//	alertDialogParameters();
			//	break;
			case 1 :
				alertDialogHelp();
				break;
			case 2 :
				alertDialogDisclaimer();
				break;
			case 3 :
				Intent email = new Intent(Intent.ACTION_SEND);
				email.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.support_mail)});
				email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject_mail));
				email.putExtra(Intent.EXTRA_TEXT, getString(R.string.message_mail));
				email.setType("message/rfc822");
				startActivity(Intent.createChooser(email, getString(R.string.choose_mail)));
				break;
		}
	}

	private void alertDialogDisclaimer(){
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.disclaimer);



		Button dialogButton = (Button) dialog.findViewById(R.id.disclaimer_validate);
		dialogButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		dialog.show();
	}

	private void alertDialogHelp(){
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.help_tutorial);

		CheckBox mapTutoSwitch = (CheckBox) dialog.findViewById(R.id.help_tutorial_replay_map_switch);
		mapTutoSwitch.setChecked(!mSessionManager.tutorialMapState());
		mapTutoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mSessionManager.tutorialMapShouldReset(!isChecked);
			}
		});

		Button dialogButton = (Button) dialog.findViewById(R.id.help_tutorial_validate);
		dialogButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		Button dialogButtonCancel = (Button) dialog.findViewById(R.id.help_tutorial_cancel);
		dialogButtonCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		dialog.show();
	}

	private void alertDialogParameters(){
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.parameters);



		Button dialogButton = (Button) dialog.findViewById(R.id.parameters_validate);
		dialogButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		dialog.show();
	}

	private void alertDialogProfil(){
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.add_user);

		((TextView) dialog.findViewById(R.id.add_user_header_text)).setText(getString(R.string.text_profil));
		((TextView) dialog.findViewById(R.id.add_user_description_text)).setVisibility(View.GONE);

		final TextView name = (TextView)dialog.findViewById(R.id.add_user_name);
		final TextView email = (TextView)dialog.findViewById(R.id.add_user_email);

		name.setText(mSessionManager.getUserDetails().get(SessionManager.KEY_NAME));
		email.setText(mSessionManager.getUserDetails().get(SessionManager.KEY_EMAIL));
		type = mSessionManager.getUserDetails().get(SessionManager.KEY_TYPE);

		final RadioButton radioRoller = (RadioButton) dialog.findViewById(R.id.add_user_roller);
		final RadioButton radioSkate = (RadioButton) dialog.findViewById(R.id.add_user_skate);
		final RadioButton radioBmx = (RadioButton) dialog.findViewById(R.id.add_user_bmx);
		radioRoller.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				type = getString(R.string.text_roller);
				radioSkate.setChecked(false);
				radioBmx.setChecked(false);
			}
		});
		radioSkate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				type = getString(R.string.text_skate);
				radioRoller.setChecked(false);
				radioBmx.setChecked(false);
			}
		});
		radioBmx.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				type = getString(R.string.text_bmx);
				radioRoller.setChecked(false);
				radioSkate.setChecked(false);
			}
		});
		if(mSessionManager.getUserDetails().get(SessionManager.KEY_TYPE).equals(getString(R.string.text_roller))){
			radioRoller.setChecked(true);
		} else if (mSessionManager.getUserDetails().get(SessionManager.KEY_TYPE).equals(getString(R.string.text_skate))){
			radioSkate.setChecked(true);
		} else {
			radioBmx.setChecked(true);
		}

		Button dialogButton = (Button) dialog.findViewById(R.id.add_user_validate);
		dialogButton.setText(getString(R.string.text_update));
		dialogButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String emailString = email.getText().toString();
				String nameString = name.getText().toString();
				String check = "";
				boolean checkName = false;
				boolean checkType = false;
				boolean checkMail = false;
				if (nameString.length() < 3) {
					check += " " + getString(R.string.add_user_minimum_name);
					checkName = true;
				}
				if (type == null) {
					check += " " + getString(R.string.add_user_minimum_type);
					checkType = true;
				}
				if (!validate(emailString)) {
					check += " " + getString(R.string.add_user_minimum_mail);
					checkMail = true;
				}
				if (checkName || checkType || checkMail) {
					Toast.makeText(MapActivity.this, getString(R.string.add_user_minimum_error_text) + check, Toast.LENGTH_LONG).show();
				} else {
					String[] userInfo = {mSessionManager.getUserDetails().get(SessionManager.KEY_ID), nameString, emailString, type};
					new UpdateUser().execute(userInfo);
					dialog.dismiss();
				}
			}
		});

		Button dialogButtonCancel = (Button) dialog.findViewById(R.id.add_user_cancel);
		dialogButtonCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		dialog.show();
	}

	public static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

	public static boolean validate(String emailStr) {
		Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(emailStr);
		return matcher.find();
	}

	private String type;

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

	private class UpdateUser extends AsyncTask<String, Void, Users>{

		public UpdateUser(){
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Users doInBackground(String... userInfo) {
			Users response = null;
			try{

				Rmsendpoint.Builder builder = new Rmsendpoint.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), null);
				Rmsendpoint service = builder.build();

				Users user = new Users();
				user.setId(Long.valueOf(userInfo[0]));
				user.setName(userInfo[1]);
				user.setAdress(userInfo[2]);
				user.setType(userInfo[3]);
				response = service.updateUsers(user).execute();

			} catch (Exception e){
				Log.d(getString(R.string.drawerlayout_update_user_error_log), e.getMessage(), e);
			}
			return response;
		}

		@Override
		protected void onPostExecute(Users user) {
			if(user != null){
				mSessionManager.updateLogin(user.getName(), user.getAdress(), user.getType());
				((TextView)findViewById(R.id.left_drawer_user_name)).setText(mSessionManager.getUserDetails().get(SessionManager.KEY_NAME));
				CircleImageView circleImageView = (CircleImageView) mDrawerLayout.findViewById(R.id.left_drawer_user_image);
				if(mSessionManager.getUserDetails().get(SessionManager.KEY_TYPE).equals(getString(R.string.text_roller))){
					circleImageView.setImageDrawable(getResources().getDrawable(R.drawable.icon_roller));
				} else if(mSessionManager.getUserDetails().get(SessionManager.KEY_TYPE).equals(getString(R.string.text_skate))){
					circleImageView.setImageDrawable(getResources().getDrawable(R.drawable.icon_skate));
				} else {
					circleImageView.setImageDrawable(getResources().getDrawable(R.drawable.icon_velo));
				}
				Toast.makeText(getBaseContext(), getString(R.string.drawerlayout_update_user_success), Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getBaseContext(), getString(R.string.drawerlayout_update_user_error), Toast.LENGTH_LONG).show();
			}

		}
	}

}


