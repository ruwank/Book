package audio.lisn.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ProgressBar;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

import audio.lisn.R;
import audio.lisn.app.AppController;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;
import audio.lisn.webservice.JsonUTF8ArrayRequest;

public class MainActivity extends Activity {

    ConnectionDetector connectionDetector;
    private final int SPLASH_DISPLAY_LENGTH = 1000;
    ProgressBar progressBar;
    int downloadCount=0,completeCount=0;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        progressBar=(ProgressBar)findViewById(R.id.lodingProgressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(
                ContextCompat.getColor(this, R.color.whiteColor),
                android.graphics.PorterDuff.Mode.SRC_IN);
        //progressBar.getIndeterminateDrawable().setColorFilter(0xFFFFFF00, android.graphics.PorterDuff.Mode.MULTIPLY);

        connectionDetector = new ConnectionDetector(getApplicationContext());


            // change login process
        /*
		if (!isUserLogin()) {
			Intent intent = new Intent(getApplicationContext(),
					LoginActivity.class);
            startActivityForResult(intent, 1);

        } else {
            if (connectionDetector.isConnectingToInternet()) {
                downloadData();
            }else{

                new Handler().postDelayed(new Runnable(){
                    @Override
                    public void run() {
                        loadHome();
                    }
                }, SPLASH_DISPLAY_LENGTH);
            }

		}
        */
        isUserLogin();

        if (connectionDetector.isConnectingToInternet()) {
            downloadData();
        }else{

            new Handler().postDelayed(new Runnable(){
                @Override
                public void run() {
                    loadHome();
                }
            }, SPLASH_DISPLAY_LENGTH);
        }

	}

    private void loadHome(){
        progressBar.setVisibility(View.INVISIBLE);
        Intent intent = new Intent(getApplicationContext(),
                HomeActivity.class);
        startActivity(intent);
        finish();
    }
    private void loadHomeScreen(){
        if(downloadCount == completeCount){
            loadHome();
        }

    }
    private void downloadNewReleaseBookList(){
        String url=getString(R.string.home_book_list_url);

        JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(url, null,

                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {

                        AppController.getInstance().setNewReleaseBookList(jsonArray);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        bookListReq.setShouldCache(true);
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(bookListReq,"tag_new_realease_list");
    }

    private void downloadTopRatedBookList(){
        String url=getString(R.string.top_rated_book_list_url);

        JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(url, null,

                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {

                        AppController.getInstance().setTopRatedBookList(jsonArray);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        bookListReq.setShouldCache(true);
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(bookListReq,"tag_topRated_list");
    }

    private void downloadTopDownloadedBookList(){
        String url=getString(R.string.top_download_book_list_url);

        JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(url, null,

                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {

                        AppController.getInstance().setTopDownloadedBookList(jsonArray);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        bookListReq.setShouldCache(true);
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(bookListReq,"tag_top_download_list");
    }
    private void downloadHomeData(){
        downloadNewReleaseBookList();
        downloadTopDownloadedBookList();
        downloadTopRatedBookList();
    }
    private void downloadBookCategoryData(){
        downloadCount=4;
        completeCount=0;
        for (int i=0;i<4;i++) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("cat", ""+i);

            String url = getString(R.string.book_category_url);

            final int finalI = i;

            JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(Request.Method.POST,url, params,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray jsonArray) {
                            completeCount++;
                            AppController.getInstance().setStoreBookForCategory(finalI,jsonArray);
                            loadHomeScreen();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    completeCount++;
                    loadHomeScreen();

                }
            });

            bookListReq.setShouldCache(true);
            // Adding request to request queue
            AppController.getInstance().addToRequestQueue(bookListReq, "tag_category_list"+i);
        }
    }
    private void downloadData() {
        progressBar.setVisibility(View.VISIBLE);
        downloadHomeData();
        downloadBookCategoryData();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == Constants.RESULT_SUCCESS){
                downloadData();
            }
            if (resultCode == Constants.RESULT_ERROR) {
                //Write your code if there's no result
            }
        }
    }//onActivityResult
    private boolean isUserLogin(){
        SharedPreferences sharedPref =getApplicationContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        boolean loginStatus = sharedPref.getBoolean(getString(R.string.user_login_status), false);
        if(loginStatus){

            String loginId = sharedPref.getString(getString(R.string.user_login_id), "");
            String userName = sharedPref.getString(getString(R.string.user_login_name), "");
            AppController.getInstance().setUserId(loginId);
            AppController.getInstance().setUserName(userName);
        }


        return loginStatus;
    }

}
