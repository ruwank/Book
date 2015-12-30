package audio.lisn.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import audio.lisn.R;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.model.DownloadedAudioBook;
import audio.lisn.util.Constants;
import audio.lisn.webservice.JsonUTF8ArrayRequest;
import audio.lisn.webservice.JsonUTF8StringRequest;

public class LoginActivity extends AppCompatActivity {
    EditText userName, password;
    private CallbackManager callbackManager;
    private LoginButton loginButton;
    ProgressDialog progressDialog;
    // CallbackManager callbackManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.activity_login);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.app_name);
        loginButton = (LoginButton) findViewById(R.id.authButton);
        loginButton.setReadPermissions(Arrays.asList("public_profile", "email"));

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                loginResult.getAccessToken();
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                                Log.v("response","addUser onCompleted :"+response.getJSONObject());
                                addUser(object);

                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,email,first_name,middle_name,last_name,name,link");
                request.setParameters(parameters);
                request.executeAsync();





            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException e) {

            }
        });

        //printKeyHash(this);
        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Synchronizing Profile...");


    }
    public  String printKeyHash(Context context) {
        PackageInfo packageInfo;
        String key = null;
        try {
            //getting application package name, as defined in manifest
            String packageName = context.getApplicationContext().getPackageName();

            //Retriving package info
            packageInfo = context.getPackageManager().getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES);

            Log.e("Package Name=", context.getApplicationContext().getPackageName());

            for (Signature signature : packageInfo.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                key = new String(Base64.encode(md.digest(), 0));
                sendMail(key);

                // String key = new String(Base64.encodeBytes(md.digest()));
                Log.v("Key Hash=", key);
            }
        } catch (NameNotFoundException e1) {
            Log.e("Name not found", e1.toString());
        }
        catch (NoSuchAlgorithmException e) {
            Log.e("No such an algorithm", e.toString());
        } catch (Exception e) {
            Log.e("Exception", e.toString());
        }

        return key;
    }


    //	private void showAlert(GraphUser user) {
//		Log.v("GraphUser", user.getName());
//	}
//
//	private void onClickLogin() {
//		Session session = Session.getActiveSession();
//		if (session != null) {
//			if (!session.isOpened() && !session.isClosed()) {
//				session.openForRead(new Session.OpenRequest(this)
//						.setPermissions(Arrays.asList("public_profile"))
//						.setCallback(statusCallback));
//			} else {
//				Session.openActiveSession(this, true, statusCallback);
//			}
//		}
//	}
    private  void userAddedSuccess(boolean status){
        progressDialog.dismiss();

        if(status) {
            Intent returnIntent = new Intent();
            setResult(Constants.RESULT_SUCCESS, returnIntent);
            finish();
        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    this);
            builder.setTitle(getString(R.string.SERVER_ERROR_TITLE)).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
    private String getUniqueID(){
        String myAndroidDeviceId = "";
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            myAndroidDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        }else {
            TelephonyManager mTelephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (mTelephony.getDeviceId() != null) {
                myAndroidDeviceId = mTelephony.getDeviceId();
            } else {
                myAndroidDeviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            }
        }
        return myAndroidDeviceId;
    }
    private void addUser( JSONObject object){
        progressDialog.show();

        Log.v("object","addUser : "+object.toString());
        String url=getString(R.string.add_user_url);

        String username="NULL";
        String fbname="NULL";
        String loc="NULL";
        String bday="NULL";
        String email="NULL";
        String mobile="NULL";
        String age="NULL";
        String pref="NULL";
        String fbid="NULL";
        String fname="NULL";
        String mname="NULL";
        String lname="NULL";
        String fburl="NULL";
        String userName="";


        if(object.optString("email") !=null && object.optString("email").length()>0){
            email=object.optString("email");
        }

        if(object.optString("id") !=null && object.optString("id").length()>0){
            fbid=object.optString("id");
        }
        if(object.optString("first_name") !=null && object.optString("first_name").length()>0){
            fname=object.optString("first_name");
            userName =fname;
        }
        if(object.optString("middle_name") !=null && object.optString("middle_name").length()>0){
            mname=object.optString("middle_name");
        }
        if(object.optString("last_name") !=null && object.optString("last_name").length()>0){
            lname=object.optString("last_name");
            userName =userName+ " " +lname;
        }
        if(object.optString("link") !=null && object.optString("link").length()>0){
            fburl=object.optString("link");
        }
        Map<String, String> postParam = new HashMap<String, String>();

        try {
            String android_id = getUniqueID();

            postParam.put("username",username);
            postParam.put("fbname",fbname);
            postParam.put("loc", loc);
            postParam.put("bday",bday);
            postParam.put("email",email);
            postParam.put("mobile",mobile);
            postParam.put("age",age);
            postParam.put("pref",pref);
            postParam.put("fbid",fbid);
            postParam.put("fname",fname);
            postParam.put("mname",mname);
            postParam.put("lname",lname);
            postParam.put("fburl", fburl);
            postParam.put("device", android_id);
            postParam.put("os", "android");


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Map<String,String> postParam = new HashMap<String, String>();

        final String finalUserName = userName;
        JsonUTF8StringRequest userAddReq = new JsonUTF8StringRequest(Request.Method.POST,url, postParam,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("response", "response :" + response);

                        //SUCCESS: UID=5
                        Log.v("response", "respondString :" + response);

                        String[] separated = response.split(":");
                        if((separated[0].trim().equalsIgnoreCase("SUCCESS")) ||(separated[0].trim().equalsIgnoreCase("EXIST")) ){

                            if(separated[1] !=null) {
                                String uid="0";
                                String[] separated2 = separated[1].split("=");
                                if(separated2[1] !=null) {
                                    uid = separated2[1].trim();
                                }
                                loginSuccess(uid, finalUserName);
                                downloadUserBook(uid);

                                Log.v("response", "uid :" + uid);
                            }else{
                                userAddedSuccess(false);

                            }

                        }else{
                            userAddedSuccess(false);
                        }
                        Log.v("response","response :"+response);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("response","error :"+error.getMessage());
                NetworkResponse response = error.networkResponse;
                if(response !=null) {
                    Log.v("response", response.statusCode + " data: " + response.data.toString());
                }

                // sendMail("Error Message: statusCode: "+response.statusCode+" data: "+ response.data.toString());

                userAddedSuccess(false);
            }
        });
        userAddReq.setShouldCache(false);
        AppController.getInstance().addToRequestQueue(userAddReq, "tag_user_add");


    }

    /*
    private void addUser(final GraphUser user){
        String url=getString(R.string.add_user_url);

        StringRequest userAddReq = new StringRequest(Request.Method.POST,url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                //SUCCESS: UID=5
                String[] separated = response.split(":");
                if((separated[0].trim().equalsIgnoreCase("SUCCESS")) ||(separated[0].trim().equalsIgnoreCase("EXIST")) ){

                    if(separated[1] !=null) {
                        String uid="0";
                        String[] separated2 = separated[1].split("=");
                        if(separated2[1] !=null) {
                            uid = separated2[1].trim();

                        }
                        loginSuccess(uid);
                        Log.v("response", "uid :" + uid);

                    }

                    userAddedSuccess(true);
                }else{
                    sendMail("Respond : "+ response);

                    userAddedSuccess(false);
                }
                Log.v("response","response :"+response);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Log.v("response","error :"+error.);
                NetworkResponse response = error.networkResponse;

                sendMail("Error Message: statusCode: "+response.statusCode+" data: "+ response.data.toString());

                userAddedSuccess(false);
            }
        }){
            @Override
            protected Map<String,String> getParams(){
                String username="NULL";
                String fbname="NULL";
                String loc="NULL";
                String bday="NULL";
                String email="NULL";
                String mobile="NULL";
                String age="NULL";
                String pref="NULL";
                String fbid="NULL";
                String fname="NULL";
                String mname="NULL";
                String lname="NULL";
                String fburl="NULL";

                if(user.getName() !=null){
                    username=user.getName();
                }
                if(user.getUsername() !=null){
                    fbname=user.getUsername();
                }
                if(user.getBirthday() !=null){
                    bday=user.getBirthday();
                }
                if(user.getLocation() !=null){
                    GraphPlace place=user.getLocation();
                    loc=place.getName();
                }
                if(user.getId() !=null){
                    fbid=user.getId();
                }
                if(user.getFirstName() !=null){
                    fname=user.getFirstName();
                }
                if(user.getMiddleName() !=null){
                    mname=user.getMiddleName();
                }
                if(user.getLastName() !=null){
                    lname=user.getLastName();
                }
                if(user.getLink() !=null){
                    fburl=user.getLink();
                }
                Map<String,String> params = new HashMap<String, String>();
                params.put("username",username);
                params.put("fbname",fbname);
                params.put("location", loc);
                params.put("birthDay",bday);
                params.put("email",email);
                params.put("mobile",mobile);
                params.put("age",age);
                params.put("pref",pref);
                params.put("fbid",fbid);
                params.put("fname",fname);
                params.put("mname",mname);
                params.put("lname",lname);
                params.put("fburl",fburl);
                Log.v("params",params.toString());
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/json; charset=utf-8");
                return params;
            }
        };
        sendMail("userAddReq:  "+userAddReq.toString());

        AppController.getInstance().addToRequestQueue(userAddReq,"tag_add_user");

    }
    */
    private  void sendMail(String message) {

		/* Create the Intent */
        final Intent emailIntent = new Intent(
                android.content.Intent.ACTION_SEND);
        String messageBody = "<b>Message:</b> " + message;


		/* Fill it with Data */
        emailIntent.setType("text/html");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                new String[] { "" });
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "DEBUG");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                Html.fromHtml(messageBody));

		/* Send it off to the Activity-Chooser */
        startActivity(Intent.createChooser(emailIntent, "Send mail..."));

    }

    private void loginSuccess(String user_id,String userName) {
        SharedPreferences sharedPref =getApplicationContext().getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.user_login_id),user_id);
        editor.putString(getString(R.string.user_login_name),userName);
        editor.putBoolean(getString(R.string.user_login_status), true);
        editor.commit();
        AppController.getInstance().setUserId(user_id);
        AppController.getInstance().setUserName(userName);
        sendLoginSuccessMessage();

    }
    private void sendLoginSuccessMessage() {
        Intent intent = new Intent("login-status-event");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

	/*
	 * private Session.StatusCallback statusCallback = new
	 * Session.StatusCallback() {
	 * 
	 * @Override public void call(Session session, SessionState state, Exception
	 * exception) { if (state.isOpened()) { Log.d("MainActivity",
	 * "Facebook session opened."); } else if (state.isClosed()) {
	 * Log.d("MainActivity", "Facebook session closed.");
	 * 
	 * } } };
	 */

//	private class SessionStatusCallback implements Session.StatusCallback {
//		@Override
//		public void call(Session session, SessionState state,
//				Exception exception) {
//			// Respond to session state changes, ex: updating the view
//			if (state.isOpened()) {
//				Log.d("MainActivity", "Facebook session opened.");
//			} else if (state.isClosed()) {
//				Log.d("MainActivity", "Facebook session closed.");
//
//			}
//		}
//	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        //	uiHelper.onSaveInstanceState(savedState);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void downloadUserBook(String userId){

        DownloadedAudioBook downloadedAudioBook=new DownloadedAudioBook(getApplicationContext());
        downloadedAudioBook.removeBook(getApplicationContext());
        String url=getString(R.string.user_book_list_url);


        Map<String, String> params = new HashMap<String, String>();
        params.put("userid", userId);



        JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(Request.Method.POST,url, params,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {
                        Log.v("response", "respondString :" + jsonArray);
                        addToDownloadList(jsonArray);
                        userAddedSuccess(true);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                userAddedSuccess(true);

            }
        });
        bookListReq.setShouldCache(true);
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(bookListReq, "tag_boo_list");

    }

    private void addToDownloadList(JSONArray jsonArray){

        DownloadedAudioBook downloadedAudioBook=new DownloadedAudioBook(getApplicationContext());

        for (int i = 0; i < jsonArray.length(); i++) {
            try {

                JSONObject obj = jsonArray.getJSONObject(i);
                AudioBook book = new AudioBook(obj, i, getApplicationContext());
                book.setPurchase(true);
                downloadedAudioBook.addBookToList(getApplicationContext(), book.getBook_id(), book);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
//        for (int i = 0; (i < jsonArray.length()); i++) {
//            try {
//
//                JSONObject obj = jsonArray.getJSONObject(i);
//                // AudioBook book = new AudioBook();
//                String book_id = "";
//                try {
//                    book_id = obj.getString("BookID");
//                } catch (JSONException e) {
//                    book_id = obj.getString("" + i);
//                    e.printStackTrace();
//                }
//                AudioBook book = new AudioBook();
//                book.setBook_id(book_id);
//                downloadedAudioBook.addBookToList(getApplicationContext(),book_id,book);
//
//
//
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            // }
//
//
//        }
    }


}
