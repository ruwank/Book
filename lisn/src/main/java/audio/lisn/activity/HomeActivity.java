package audio.lisn.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import audio.lisn.R;
import audio.lisn.app.AppController;
import audio.lisn.fragment.HomeFragment;
import audio.lisn.fragment.MyBookFragment;
import audio.lisn.fragment.StoreBaseFragment;
import audio.lisn.fragment.StoreFragment;
import audio.lisn.util.AudioPlayerService;
import audio.lisn.util.Constants;
import audio.lisn.util.OnSwipeTouchListener;
import audio.lisn.view.PlayerControllerView;

//import android.support.v7.widget.SearchView;

//import android.support.v7.widget.SearchView;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,HomeFragment.OnHomeItemSelectedListener,StoreFragment.OnStoreBookSelectedListener {

    private static final String TAG = HomeActivity.class.getSimpleName();

    private DrawerLayout drawer;
    NavigationView navigationView;
//    PlayerControllerView audioPlayerLayout;
    private int mNavItemId;
    boolean isUserLogin;
    PlayerControllerView playerControllerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setActionBarIcon(R.drawable.ic_drawer);

        setContentView(R.layout.activity_home);

        initToolbar();
        drawer = (DrawerLayout) findViewById(R.id.drawer);
        drawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        getSupportActionBar().setTitle(R.string.title_home);

        // listen for navigation events
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        updateNavigationView();



        mNavItemId=R.id.drawer_home;

        playerControllerView = (PlayerControllerView) findViewById(R.id.audio_player_layout);

        playerControllerView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeRight() {
                Log.v(TAG,"onSwipeRight");
                playerControllerView.animate()
                        .translationX(playerControllerView.getWidth())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                playerControllerView.setX(0);
                                playerControllerView.setVisibility(View.GONE);

                            }

                            @Override
                            public void onAnimationStart(Animator animation) {
                                super.onAnimationStart(animation);
                                playerControllerView.stopAudioPlayer();
                            }
                        });

            }
            @Override
            public void onSingleTap() {
                showAudioPlayer();
            }

        });

        navigateFragment(mNavItemId);
        registerBroadcastReceiver();


    }
    private void showAudioPlayer(){
        PlayerControllerActivity.navigate(this, playerControllerView, null);

    }

    private void updateNavigationView() {
        isUserLogin= AppController.getInstance().isUserLogin();
        navigationView.getMenu().clear();
        View headerLayout = navigationView.getHeaderView(0); // 0-index header
        TextView userName = (TextView) headerLayout.findViewById(R.id.user_name);
        if (isUserLogin){
            navigationView.inflateMenu(R.menu.navigation_menu_member);

            userName.setText(AppController.getInstance().getUserName());
        }else{
            navigationView.inflateMenu(R.menu.navigation_menu_none_member);
            userName.setText("");

        }
    }

    private void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_drawer);
            actionBar.setDisplayHomeAsUpEnabled(true);
          //  getSupportActionBar().setLogo(R.drawable.ic_app_top_bar);
        }
    }


//    @Override protected int getLayoutResource() {
//        return R.layout.activity_home;
//    }



    @Override
    protected void onResume() {
        super.onResume();
        if((AudioPlayerService.mediaPlayer!=null) && AudioPlayerService.hasStartedPlayer){
            playerControllerView.setVisibility(View.VISIBLE);
        }else{
            playerControllerView.setVisibility(View.GONE);

        }
        playerControllerView.updateView();
        registerPlayerUpdateBroadcastReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPlayerUpdateReceiver);

    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {

	        
			MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.home, menu);
            MenuItem searchItem = menu.findItem(R.id.action_search);

            SearchManager searchManager = (SearchManager) HomeActivity.this.getSystemService(Context.SEARCH_SERVICE);

            SearchView searchView = null;
            if (searchItem != null) {
                searchView = (SearchView) searchItem.getActionView();
            }
            if (searchView != null) {
                searchView.setSearchableInfo(searchManager.getSearchableInfo(HomeActivity.this.getComponentName()));
            }

            return true;



	}
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawer.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

        return super.onPrepareOptionsMenu(menu);
	}


    private void navigateFragment(int position) {
        Fragment fragment = null;
        String title = getString(R.string.app_name);
        switch (position) {
            case  R.id.drawer_home:
                fragment = HomeFragment.newInstance();
                title = getString(R.string.title_home);
                break;
            case R.id.drawer_store:
                fragment = StoreBaseFragment.newInstance();
                title = getString(R.string.title_store);
                break;
            case R.id.drawer_my_book:
                fragment = MyBookFragment.newInstance();
                title = getString(R.string.title_my_book);
                break;
            case R.id.drawer_settings:
                fragment = HomeFragment.newInstance();
               // title = getString(R.string.title_settings);
                break;
            case R.id.drawer_feedback:
                fragment = StoreBaseFragment.newInstance();
                // title = getString(R.string.title_settings);
                break;
            case R.id.drawer_about_us:
                fragment = MyBookFragment.newInstance();
                // title = getString(R.string.title_settings);
                break;

            default:
                break;
        }

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.container_body, fragment);
            fragmentTransaction.commit();

            // set the toolbar title
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        if(menuItem.getItemId() ==R.id.drawer_about_us){

            Intent intent = new Intent(this,
                    AboutUsActivity.class);
            startActivity(intent);

        }
        else if(menuItem.getItemId() ==R.id.drawer_feedback){

            Intent intent = new Intent(this,
                    FeedBackActivity.class);
            startActivity(intent);

        }
        else if(menuItem.getItemId() ==R.id.drawer_contact_us){

            Intent intent = new Intent(this,
                    ContactusActivity.class);
            startActivity(intent);

        }
        else if(menuItem.getItemId() ==R.id.drawer_settings){
            AppController.getInstance().logOutUser();
            updateNavigationView();

        }else if(menuItem.getItemId() ==R.id.drawer_my_book){
            if(AppController.getInstance().isUserLogin()){
                menuItem.setChecked(true);
                mNavItemId = menuItem.getItemId();
                navigateFragment(mNavItemId);
            }else{
                Intent intent = new Intent(this,
                        LoginActivity.class);
                startActivityForResult(intent, 13);
            }

        }
        else {
            menuItem.setChecked(true);
            mNavItemId = menuItem.getItemId();
            navigateFragment(mNavItemId);
        }
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }


    @Override
    public void onHomeItemSelected(int position, boolean isDownloadedBook) {
       // navigationView.getMenu().getItem(3).setChecked(true);


    }

    @Override
    public void onOptionButtonClicked(int buttonIndex) {

        MenuItem menuItem=navigationView.getMenu().getItem(buttonIndex);

            menuItem.setChecked(true);
            mNavItemId = menuItem.getItemId();
            navigateFragment(mNavItemId);


    }

    @Override
    public void onStoreBookSelected(int position) {

    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 13) {
            if(resultCode ==  Constants.RESULT_SUCCESS){
                new Handler().post(new Runnable() {
                    public void run() {
                        onOptionButtonClicked(2);
                    }
                });



            }
            if (resultCode ==  Constants.RESULT_ERROR) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                        R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                android.support.v7.app.AlertDialog dialog = builder.create();
                dialog.show();
            }
        }else {
            for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        }


            super.onActivityResult(requestCode,  resultCode,  data);
    }
        private void registerBroadcastReceiver(){
        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mLoginUpdateReceiver,
                new IntentFilter("login-status-event"));
    }
    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mLoginUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            Log.v("mLoginUpdateReceiver","mLoginUpdateReceiver");
            updateNavigationView();
            MenuItem menuItem=navigationView.getMenu().getItem(1);
            menuItem.setChecked(true);
        }
    };
    private void registerPlayerUpdateBroadcastReceiver(){
        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mPlayerUpdateReceiver,
                new IntentFilter("audio-event"));
    }
    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mPlayerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            playerControllerView.updateView();
        }
    };
}
