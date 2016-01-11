package audio.lisn.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.os.ResultReceiver;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.transition.Slide;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.ms.square.android.expandabletextview.ExpandableTextView;
import com.squareup.picasso.Picasso;
import com.thin.downloadmanager.ThinDownloadManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import audio.lisn.R;
import audio.lisn.adapter.BookReviewViewAdapter;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.model.BookReview;
import audio.lisn.model.DownloadedAudioBook;
import audio.lisn.service.DownloadService;
import audio.lisn.util.AppUtils;
import audio.lisn.util.AudioPlayerService;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;
import audio.lisn.util.CustomTypeFace;
import audio.lisn.util.OnSwipeTouchListener;
import audio.lisn.util.WCLinearLayoutManager;
import audio.lisn.view.PlayerControllerView;
import audio.lisn.webservice.FileDownloadTask;
import audio.lisn.webservice.FileDownloadTaskListener;
import audio.lisn.webservice.JsonUTF8StringRequest;

public class AudioBookDetailActivity extends  AppCompatActivity implements Runnable,FileDownloadTaskListener{

    private static final String TRANSITION_NAME = "audio.lisn.AudioBookDetailActivity";
    private CollapsingToolbarLayout collapsingToolbarLayout;
    AudioBook audioBook;
    ImageButton previewPlayButton;
    ImageButton btnShare;
    Button btnReview;
    //private boolean isPlayingPreview,isLoadingPreview;
    MediaPlayer mediaPlayer = null;
    ConnectionDetector connectionDetector;

    public RelativeLayout previewLayout;
    public TextView previewLabel,timeLabel;
    public ProgressBar spinner;
    private boolean isPlayingPreview,isLoadingPreview;
    String leftTime;
    ProgressDialog mProgressDialog;
    ProgressDialog progressDialog;
    int totalAudioFileCount, downloadedFileCount;
    List<FileDownloadTask> downloadingList = new ArrayList<FileDownloadTask>();
   // List<Intent> downloadingList = new ArrayList<Intent>();
    ImageView bookCoverImage;
    private PopupWindow pwindo;
    int previousDownloadedFileCount;
    PlayerControllerView playerControllerView;
    //TextView downloaded;
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final int PERMISSIONS_REQUEST_READ_PHONE_STATE=101;


    BookReviewViewAdapter bookReviewViewAdapter;
    RecyclerView reviewContainer;
    private static final String KEY_TERMS_ACCEPTED_FOR_CARD="KEY_TERMS_ACCEPTED_FOR_CARD";
    private static final String KEY_TERMS_ACCEPTED_FOR_DIALOG="KEY_TERMS_ACCEPTED_FOR_DIALOG";
    private static final String KEY_TERMS_ACCEPTED_FOR_MOBITEL="KEY_TERMS_ACCEPTED_FOR_MOBITEL";
    private static final String KEY_TERMS_ACCEPTED_FOR_ETISALAT="KEY_TERMS_ACCEPTED_FOR_ETISALAT";

   // NestedScrollView scrollView;

    public enum ServiceProvider {
        PROVIDER_NONE, PROVIDER_MOBITEL,PROVIDER_DIALOG,PROVIDER_ETISALAT
    }

    public enum PaymentOption {
        OPTION_NONE, OPTION_MOBITEL,OPTION_CARD,OPTION_ETISALAT
    }

    ServiceProvider  serviceProvider;
    PaymentOption  paymentOption;
    Thread timerUpdateThread;

    //  ListView reviewListView;
    //  BookReviewListAdapter bookReviewListAdapter;


    public static void navigate(AppCompatActivity activity, View transitionImage, AudioBook audioBook) {
        Intent intent = new Intent(activity, AudioBookDetailActivity.class);
        intent.putExtra("audioBook", audioBook);
     //   Pair<View, String> imagePair = Pair.create((View) transitionImage, activity.getString(R.string.transition_book));
        //Pair<View, String> holderPair = Pair.create((View) placeNameHolder, "tNameHolder");
// 3
       // ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity,imagePair);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, transitionImage,activity.getString(R.string.transition_book));
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @SuppressWarnings("ConstantConditions")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //postponeTransition(); // postpone shared element transition until we release it explicitly

        // initActivityTransitions();
        setContentView(R.layout.activity_audio_book_detail);
       // supportPostponeEnterTransition();
        findServiceProvider();

       // ViewCompat.setTransitionName(findViewById(R.id.app_bar_layout), TRANSITION_NAME);
        downloadedFileCount=0;
        audioBook = (AudioBook) getIntent().getSerializableExtra("audioBook");

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
       // supportStartPostponedEnterTransition();

        String itemTitle = audioBook.getEnglish_title();
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(itemTitle);
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));

        connectionDetector = new ConnectionDetector(getApplicationContext());

        playerControllerView = (PlayerControllerView) findViewById(R.id.audio_player_layout);
        playerControllerView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeRight() {
                playerControllerView.animate()
                        .translationX(playerControllerView.getWidth())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                playerControllerView.setX(0);
                                playerControllerView.setVisibility(View.INVISIBLE);

                            }

                            @Override
                            public void onAnimationStart(Animator animation) {
                                super.onAnimationStart(animation);
                               // setLayoutMargin(false);

                                playerControllerView.stopAudioPlayer();
                            }
                        });

            }

            @Override
            public void onSingleTap() {
                showAudioPlayer();
            }

        });
       // scrollView= (NestedScrollView) findViewById(R.id.scroll);
        updateData();
        // Do all heavy processing here, activity will not enter transition until you explicitly call startPostponedEnterTransition()

        // all heavy init() done
       // startPostponedTransition();

    }
    @Override
    protected void onResume() {
        super.onResume();

        if((AudioPlayerService.mediaPlayer!=null) && AudioPlayerService.hasStartedPlayer){
            playerControllerView.setVisibility(View.VISIBLE);
        }else{
            playerControllerView.setVisibility(View.INVISIBLE);

        }
        playerControllerView.updateView();
        registerPlayerUpdateBroadcastReceiver();
        bookReviewViewAdapter.notifyDataSetChanged();
    }
    @Override
    public void onPause() {
        super.onPause();
        if(mediaPlayer !=null && mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPlayerUpdateReceiver);

    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if ( mProgressDialog!=null && mProgressDialog.isShowing() ){
            mProgressDialog.dismiss();
        }
    }
//    private void setLayoutMargin(boolean setMargin){
//        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)scrollView.getLayoutParams();
//
//        if(setMargin){
//            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, (int) getResources().getDimension(R.dimen.snackbar_height));
//        }else {
//            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, 0);
//
//        }
//
//        scrollView.setLayoutParams(params);
//    }

    private void findServiceProvider() {
        Log.v("deviceID", "findDeviceID");
        serviceProvider = ServiceProvider.PROVIDER_NONE;
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
//        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
//                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                    PERMISSIONS_REQUEST_READ_PHONE_STATE);
        } else {
            setServiceProvider();
        }

    }
    private void setServiceProvider(){
        TelephonyManager m_telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        String subscriberId = m_telephonyManager.getSubscriberId();
        if (subscriberId != null) {
            if (subscriberId.startsWith("41301")) {
                serviceProvider = ServiceProvider.PROVIDER_MOBITEL;
//            } else if (subscriberId.startsWith("41302")) {
//                serviceProvider = ServiceProvider.PROVIDER_DIALOG;
            } else if (subscriberId.startsWith("41303")) {
                serviceProvider = ServiceProvider.PROVIDER_ETISALAT;
            }

        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
//        if (requestCode == PERMISSIONS_REQUEST_READ_PHONE_STATE
//                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            setServiceProvider();
//        }

        switch (requestCode)
        {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    downloadAudioFile();
                    //reload my activity with permission granted or use the features what required the permission
                } else
                {
                    Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                }
            }
            case PERMISSIONS_REQUEST_READ_PHONE_STATE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    setServiceProvider();
                }

            }
        }

    }
    private void termsAndConditionAccepted(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if(paymentOption == PaymentOption.OPTION_MOBITEL){
            prefs.edit().putBoolean(KEY_TERMS_ACCEPTED_FOR_MOBITEL, true).commit();

            addToMobitelBill();

        }else if(paymentOption == PaymentOption.OPTION_ETISALAT){
            prefs.edit().putBoolean(KEY_TERMS_ACCEPTED_FOR_ETISALAT, true).commit();

            addToEtisalatBill();

        }else if(paymentOption == PaymentOption.OPTION_CARD){
            prefs.edit().putBoolean(KEY_TERMS_ACCEPTED_FOR_CARD, true).commit();

            buyFromCardButtonPressed();

        }

    }
    private void showTermsAndCondition(){

        String title = getString(R.string.app_name) ;
        String message = getString(R.string.terms_condition);


        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(Html.fromHtml(message))
                .setCancelable(false)
                .setPositiveButton(R.string.terms_accept,
                        new Dialog.OnClickListener() {

                            @Override
                            public void onClick(
                                    DialogInterface dialogInterface, int i) {
                                // Mark this version as read.
                                termsAndConditionAccepted();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new Dialog.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                            }

                        });
        builder.create().show();

    }



    private void showAudioPlayer(){
        PlayerControllerActivity.navigate(this, playerControllerView, null);

    }
    private void showBookReview(){
        BookReviewActivity.navigate(this, playerControllerView, audioBook.getReviews());

    }
    private void updateData() {

        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        ImageView bookBannerImage = (ImageView) findViewById(R.id.bookBannerImage);
        ViewGroup.LayoutParams params = bookBannerImage.getLayoutParams();
        params.height=(int)(size.x/2);
        bookBannerImage.setLayoutParams(params);

        // String bannerImageUrl="http://lorempixel.com/500/500/animals/8/";
        String bannerImageUrl=audioBook.getBanner_image();

        Picasso.with(this)
                .load(bannerImageUrl)
                .placeholder(R.drawable.default_banner)
                .into(bookBannerImage);

        bookCoverImage = (ImageView) findViewById(R.id.bookCoverImage);
        Picasso.with(this)
                .load(audioBook.getCover_image())
                .placeholder(R.drawable.audiobook_placeholder)
                .into(bookCoverImage);

        // ViewGroup.LayoutParams params = bookCoverImage.getLayoutParams();
        RelativeLayout.LayoutParams bookCoverImageLayoutParams =
                (RelativeLayout.LayoutParams)bookCoverImage.getLayoutParams();
        bookCoverImageLayoutParams.width= (int) ((size.x-60)/3);

        bookCoverImage.setLayoutParams(bookCoverImageLayoutParams);

        String narratorText="";
        String durationText="";
        TextView title = (TextView) findViewById(R.id.title);

//        ExpandableTextView description = (ExpandableTextView) findViewById(R.id.description);
//        TextView descriptionTextView = (TextView) findViewById(R.id.expandable_text);

        TextView description = (TextView) findViewById(R.id.description);

        TextView fileSize = (TextView) findViewById(R.id.fileSize);
        TextView duration = (TextView) findViewById(R.id.duration);

        TextView category = (TextView) findViewById(R.id.category);
        TextView price = (TextView) findViewById(R.id.price);
        TextView author = (TextView) findViewById(R.id.author);
        TextView narrator = (TextView) findViewById(R.id.narrator);
        TextView ratingValue = (TextView) findViewById(R.id.rating_value);
        LinearLayout rateLayout=(LinearLayout)findViewById(R.id.app_rate_layout);


        Button btnDownload=(Button)findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentOption=PaymentOption.OPTION_NONE;
                playGetButtonPressed();

            }
        });

        Button addToBillButton=(Button)findViewById(R.id.addToBillButton);
        addToBillButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToMyBillButtonPressed();
            }
        });

        Button btnPayFromCard=(Button)findViewById(R.id.buyFromCardButton);
        btnPayFromCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentOption=PaymentOption.OPTION_CARD;
                buyFromCardButtonPressed();
            }
        });

       // TextView paymentOptionOr = (TextView) findViewById(R.id.mobitelOption);
       // TextView buyFromCardDescription = (TextView) findViewById(R.id.buyFromCardButtonText);

        RatingBar ratingBar = (RatingBar) findViewById(R.id.rating_bar);
        LayerDrawable stars1 = (LayerDrawable) ratingBar.getProgressDrawable();
        stars1.getDrawable(2).setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP);


        RatingBar userRatingBar = (RatingBar) findViewById(R.id.user_rate_bar);
        LayerDrawable stars = (LayerDrawable) userRatingBar.getProgressDrawable();
        stars.getDrawable(2).setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP);

        userRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {

            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if(fromUser){
                    initiatePopupWindow(rating);
                }

            }

        });
        previewPlayButton = (ImageButton) findViewById(R.id.previewPlayButton);
        previewPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPreviewButtonPressed();

            }
        });

        previewLayout=(RelativeLayout)findViewById(R.id.preview_layout);
        RelativeLayout.LayoutParams previewLayoutLayoutParams =
                (RelativeLayout.LayoutParams)previewLayout.getLayoutParams();
        previewLayoutLayoutParams.width= (int) ((size.x-60)/3);

        previewLayout.setLayoutParams(previewLayoutLayoutParams);
        previewLabel=(TextView)findViewById(R.id.preview_label);
        timeLabel=(TextView)findViewById(R.id.time_label);
        RelativeLayout.LayoutParams timeLabelLayoutParams =
                (RelativeLayout.LayoutParams)timeLabel.getLayoutParams();
        timeLabelLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        // timeLabel.setLayoutParams(timeLabelLayoutParams);

        // params = timeLabel.getLayoutParams();

        spinner = (ProgressBar)findViewById(R.id.progressBar);

        if(audioBook.getLanguageCode()== AudioBook.LanguageCode.LAN_SI){
            description.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            title.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            author.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            category.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            narrator.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            duration.setTypeface(CustomTypeFace.getSinhalaTypeFace(getApplicationContext()));
            narratorText=getString(R.string.narrator_si);
            durationText=getString(R.string.duration_si);
        }else{
            description.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));
            title.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));
            author.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));
            category.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));
            narrator.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));
            duration.setTypeface(CustomTypeFace.getEnglishTypeFace(getApplicationContext()));

            narratorText=getString(R.string.narrator_en);
            durationText=getString(R.string.duration_en);
        }
        title.setText(audioBook.getTitle());
        String priceText="Free";
        if( Float.parseFloat(audioBook.getPrice())>0 ){
            priceText="Rs: "+audioBook.getPrice();
        }

        if(Float.parseFloat(audioBook.getRate())>-1){
            ratingBar.setRating(Float.parseFloat(audioBook.getRate()));
            ratingValue.setText(String.format("%.1f", Float.parseFloat(audioBook.getRate())));


        }

        narratorText=narratorText+" - "+audioBook.getNarrator();
        durationText=durationText+" - "+audioBook.getDuration();
        author.setText(audioBook.getAuthor());
        category.setText(audioBook.getCategory());
        narrator.setText(narratorText);
        fileSize.setText(audioBook.getFileSize()+" Mb");
        price.setText(priceText);
        title.setText(audioBook.getTitle());
        duration.setText(durationText);
        if(audioBook.getDescription() !=null && audioBook.getDescription().length()>1){
            description.setText(audioBook.getDescription());
            description.setVisibility(View.VISIBLE);
        }else{
           // description.setVisibility(View.GONE);
        }

        //buyFromCardDescription.setText("(and get a "+audioBook.getDiscount()+"% discount)");
        btnPayFromCard.setText("Pay by Card (" + audioBook.getDiscount() + "% discount)");

        if(AppController.getInstance().isUserLogin() && audioBook.isPurchase()){
            btnDownload.setText("Download");
            btnDownload.setVisibility(View.VISIBLE);

            if(audioBook.getAudioFileCount() == audioBook.getDownloadedChapter().size()){
                btnDownload.setText("Play");
            }
            rateLayout.setVisibility(View.VISIBLE);

           // addToBillButton.setVisibility(View.GONE);
           // btnPayFromCard.setVisibility(View.GONE);
            // btnDownload.setImageResource(R.drawable.btn_lisn_book_large);
        }else{
            //rateLayout.setVisibility(View.GONE);
            if(Float.parseFloat(audioBook.getPrice())>0) {
               // btnDownload.setVisibility(View.GONE);
                // btnPayFromCard.setText("Get the book at Rs. "+(Float.parseFloat(audioBook.getPrice()) * 0.9)+"(10% discount) by paying through your card ");

                if (serviceProvider !=ServiceProvider.PROVIDER_NONE){
                    // btnAddMobitel.setText("Add to my bill - Rs "+audioBook.getPrice());
                    addToBillButton.setVisibility(View.VISIBLE);
                    //paymentOptionOr.setVisibility(View.VISIBLE);
                    if(serviceProvider ==ServiceProvider.PROVIDER_MOBITEL){
                        addToBillButton.setText("Add to Mobitel bill");
                    }
                    else if(serviceProvider ==ServiceProvider.PROVIDER_ETISALAT){
                        addToBillButton.setText("Add to Etisalat bill");

                    }

                }else{
                  //  addToBillButton.setVisibility(View.GONE);
                }
                btnPayFromCard.setVisibility(View.VISIBLE);


            }else{
                btnDownload.setText("Download");
                btnDownload.setVisibility(View.VISIBLE);
              //  addToBillButton.setVisibility(View.GONE);
               // btnPayFromCard.setVisibility(View.GONE);

            }

        }

        btnShare=(ImageButton)findViewById(R.id.btnShare);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareBook();
            }
        });

        int reviewsCount=0;

        if(audioBook.getReviews() !=null)
            reviewsCount=audioBook.getReviews().size();
        final int finalReviewsCount = reviewsCount;

        btnReview=(Button)findViewById(R.id.btnReview);
        btnReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(finalReviewsCount >0){
                    showBookReview();

                }


            }
        });


        btnReview.setText(""+reviewsCount);

        //downloaded=(TextView)findViewById(R.id.downloaded);
        //downloaded.setText("" + audioBook.getDownloads());

        TextView allReviews=(TextView)findViewById(R.id.all_reviews);
        allReviews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showBookReview();


            }
        });


        reviewContainer=(RecyclerView)findViewById(R.id.reviewContainer);

        ArrayList<BookReview> reviews;

        if(reviewsCount>3){
            reviews= (ArrayList<BookReview>) audioBook.getReviews().subList(0,3);
            allReviews.setVisibility(View.VISIBLE);
        }else{
           // allReviews.setVisibility(View.INVISIBLE);
            reviews=audioBook.getReviews();
        }
        WCLinearLayoutManager linearLayoutManagerVertical =
                new WCLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        reviewContainer.setLayoutManager(linearLayoutManagerVertical);
        // reviewContainer.setLayoutManager(new GridLayoutManager(this, 3));

        bookReviewViewAdapter=new BookReviewViewAdapter(getApplicationContext(),reviews);
        reviewContainer.setAdapter(bookReviewViewAdapter);

        mProgressDialog = new ProgressDialog(AudioBookDetailActivity.this);
        mProgressDialog.setMessage("Downloading file..");
        mProgressDialog.setTitle("Download in progress ...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                stopDownload();
            }
        });

        progressDialog = new ProgressDialog(AudioBookDetailActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Publishing...");




    }

    private void stopDownload(){
        for (int i = 0; i < downloadingList.size(); i++) {
            //Intent intent=downloadingList.get(i);
            //stopService(intent);
            FileDownloadTask downloadTask = downloadingList.get(i);
            downloadTask.cancel(true);

        }
    }


    private void updatePreviewLayout(){
        if(isLoadingPreview || isPlayingPreview){
           // setLayoutMargin(true);
            previewLayout.setVisibility(View.VISIBLE);
            previewPlayButton.setImageResource(R.drawable.btn_play_preview_pause);

            if(isPlayingPreview){
                spinner.setVisibility(View.INVISIBLE);
                previewLabel.setText("Preview");
                timeLabel.setText(leftTime);

            }else{
                spinner.setVisibility(View.VISIBLE);
                previewLabel.setText("Loading...");
                timeLabel.setText("");
            }
        }else{
           // setLayoutMargin(false);
            previewLayout.setVisibility(View.INVISIBLE);
            previewPlayButton.setImageResource(R.drawable.btn_play_preview_start);
        }
    }

    private void playPreviewButtonPressed(){
        if (audioBook.getPreview_audio() !=null && (audioBook.getPreview_audio().length()>0)) {
            boolean stopPlayer = false;
            if(isLoadingPreview || isPlayingPreview ){
                stopPlayer=true;
            }

            if(stopPlayer){
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                    new Thread(this).interrupt();
                }

                mediaPlayer.reset();
                isPlayingPreview=false;
                isLoadingPreview=false;

            }else{
                playPreview();
            }

        }
        updatePreviewLayout();
    }
    private void playPreview( ) {

        isLoadingPreview=true;
        isPlayingPreview=false;
        if (connectionDetector.isConnectingToInternet()) {
            pausePlayer();
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                if( timerUpdateThread != null ) {
                    timerUpdateThread.interrupt();
                }
            }

            mediaPlayer.reset();

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(audioBook.getPreview_audio());
            } catch (IOException e) {
                Log.v("playPreview", "IOException" + e.getMessage());

                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    isPlayingPreview=true;
                    isLoadingPreview=false;
                    startTimer();
                    mp.start();
                    updatePreviewLayout();
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {

                    return false;
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    isPlayingPreview=false;
                    isLoadingPreview=false;
                    stopTimer();
                    updatePreviewLayout();

                }
            });
            mediaPlayer.prepareAsync(); // prepare async to not block main


        } else {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(getString(R.string.NO_ENOUGH_SPACE_MESSAGE)).setPositiveButton(
                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }
    private void updateAudioBook(int chapter){
        if(chapter>0) {
            audioBook.addChapterToDownloadedChapter(chapter);
        }
        DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(
                getApplicationContext());
       // downloadedAudioBook.readFileFromDisk(getApplicationContext());
        downloadedAudioBook.addBookToList(getApplicationContext(),
                audioBook.getBook_id(), audioBook);

    }
    private void logUserDownload(){
        progressDialog.setMessage("Loading...");
        progressDialog.show();
        saveCoverImage();
        Map<String, String> params = new HashMap<String, String>();
        params.put("userid", AppController.getInstance().getUserId());
        params.put("bookid", audioBook.getBook_id());
        params.put("actid", ""+1);
        params.put("content", ""+1);

        String url = getResources().getString(R.string.user_action_url);

        JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url,params,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("response","response:"+response);
                        downloadAudioFile();
                        audioBook.setPurchase(true);
                        updateAudioBook(0);
                        progressDialog.dismiss();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();

            }
        });


        AppController.getInstance().addToRequestQueue(stringRequest, "tag_download_book");

    }

    private void downloadAudioFileFromUrl(int filePart){

        if (connectionDetector.isConnectingToInternet()) {
            String dirPath = AppUtils.getDataDirectory(getApplicationContext())
                    + audioBook.getBook_id()+File.separator;
            String filePath=dirPath + filePart + ".lisn";
            File file = new File(filePath);

            if (file.exists()) {
                file.delete();
            }
            /*

            Intent intent = new Intent(this, DownloadService.class);
            intent.putExtra("book_id", audioBook.getBook_id());
            intent.putExtra("filePart", ""+filePart);
            intent.putExtra("dirPath", dirPath);
            intent.putExtra("receiver", new DownloadReceiver(new Handler()));
            startService(intent);
            downloadingList.add(intent);
            */


            FileDownloadTask downloadTask =  new FileDownloadTask(this,this,audioBook.getBook_id());
            downloadTask.execute(dirPath, "" + filePart);
            downloadingList.add(downloadTask);

//            Uri downloadUri = Uri.parse("http://tcrn.ch/Yu1Ooo1");
//            Uri destinationUri = Uri.parse(filePath);
//            DownloadRequest downloadRequest = new DownloadRequest(downloadUri)
//                    .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
//                    .setDownloadListener(new DownloadStatusListener() {
//                        @Override
//                        public void onDownloadComplete(int id) {
//                            Log.v("DownloadRequest","DownloadRequest onDownloadComplete: "+id);
//
//                        }
//
//                        @Override
//                        public void onDownloadFailed(int id, int errorCode, String errorMessage) {
//                            Log.v("DownloadRequest","DownloadRequest onDownloadFailed: "+id);
//
//                        }
//
//                        @Override
//                        public void onProgress(int id, long totalBytes, long downlaodedBytes, int progress) {
//                            Log.v("DownloadRequest","DownloadRequest onDownloadComplete: "+ id);
//
//                        }
//                    });
//            int downloadId = downloadManager.add(downloadRequest);

        }else{
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                    this);
            builder.setTitle(getString(R.string.NO_INTERNET_TITLE)).setMessage(getString(R.string.NO_INTERNET_MESSAGE)).setPositiveButton(
                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            android.app.AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
    private void downloadAudioFile() {
        boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }else {
            previousDownloadedFileCount = downloadedFileCount;
            String dirPath = AppUtils.getDataDirectory(getApplicationContext())
                    + audioBook.getBook_id() + File.separator;
            File fileDir = new File(dirPath);
            if (!fileDir.exists()) {
                fileDir.mkdirs();

            }

            if (connectionDetector.isConnectingToInternet()) {
                //  updateAudioBook(null,null);


                mProgressDialog.show();


                downloadedFileCount = 0;
                // String [] file_urls=audioBook.getAudio_file_urls();
                //totalAudioFileCount=file_urls.length;
                totalAudioFileCount = 0;
                downloadingList.clear();

                //  HashMap fileList= audioBook.getDownloadedFileList();

                for (int filePart = 1; filePart <= (audioBook.getAudioFileCount()); filePart++) {
                    File file = new File(dirPath + filePart + ".lisn");

                    if (!file.exists() || !(audioBook.getDownloadedChapter().contains(filePart))) {
                        downloadAudioFileFromUrl(filePart);
                        totalAudioFileCount++;
                    }


                }
                if (downloadedFileCount == totalAudioFileCount) {
                    mProgressDialog.dismiss();
                    starAudioPlayer();


                } else {
                    if (AppUtils.getAvailableMemory() < audioBook.getFileSize()) {
                        stopDownload();

                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                this);
                        builder.setTitle(R.string.NO_ENOUGH_SPACE_TITLE).setMessage(R.string.NO_ENOUGH_SPACE_MESSAGE).setPositiveButton(
                                R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // FIRE ZE MISSILES!
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();

                    } else {
                        mProgressDialog.setMessage("Downloading " + (audioBook.getDownloadedChapter().size() + 1) + " of " + audioBook.getAudioFileCount());
                    }
                }

            } else {

                downloadedFileCount = 0;
                totalAudioFileCount = 0;

                for (int filePart = 1; filePart <= (audioBook.getAudioFileCount()); filePart++) {
                    File file = new File(dirPath + filePart + ".lisn");
                    if (!file.exists()) {
                        totalAudioFileCount++;
                    }


                }
                if (downloadedFileCount == totalAudioFileCount) {
                    mProgressDialog.dismiss();
                    starAudioPlayer();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            this);
                    builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(getString(R.string.NO_INTERNET_MESSAGE)).setPositiveButton(
                            getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // FIRE ZE MISSILES!
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        }
    }

    private void starAudioPlayer() {
        if (previousDownloadedFileCount == 0) {
            PlayerControllerActivity.navigate(this, bookCoverImage, audioBook);

        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.DOWNLOAD_COMPLETE_TITLE).setMessage(getString(R.string.DOWNLOAD_COMPLETE_MESSAGE)).setPositiveButton(
                    R.string.BUTTON_YES, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            PlayerControllerActivity.navigate(AudioBookDetailActivity.this, bookCoverImage, audioBook);

                        }
                    })
                    .setNegativeButton(R.string.BUTTON_NO, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }
    private void shareBook() {
        Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        share.putExtra(Intent.EXTRA_SUBJECT, audioBook.getEnglish_title());
        share.putExtra(Intent.EXTRA_TEXT, getString(R.string.play_store_url));

        startActivity(Intent.createChooser(share, "Share app through..."));
    }
    //    private void playGetButtonPressed(){
//
//        if(AppController.getInstance().isUserLogin()){
//
//        if (audioBook.isPurchase()) {
//            downloadAudioFile();
//
//        } else {
//            if (Float.parseFloat(audioBook.getPrice()) > 0) {
//                Intent intent = new Intent(this,
//                        PurchaseActivity.class);
//                intent.putExtra("audioBook", audioBook);
//                startActivityForResult(intent, 2);
//
//            }else{
//                logUserDownload();
//            }
//        }
//        }else{
//            Intent intent = new Intent(getApplicationContext(),
//                    LoginActivity.class);
//            startActivityForResult(intent, 1);
//        }
//    }
    private void playGetButtonPressed(){

        if(AppController.getInstance().isUserLogin()){

            if (audioBook.isPurchase()) {
                downloadAudioFile();

            } else {
                if (Float.parseFloat(audioBook.getPrice()) > 0) {
                    Intent intent = new Intent(this,
                            PurchaseActivity.class);
                    intent.putExtra("audioBook", audioBook);
                    startActivityForResult(intent, 2);

                }else{
                    logUserDownload();
                }
            }
        }else{
            Intent intent = new Intent(getApplicationContext(),
                    LoginActivity.class);
            startActivityForResult(intent, 1);
        }
    }
    private void buyFromCardButtonPressed(){

        if(AppController.getInstance().isUserLogin()){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if(prefs.getBoolean(KEY_TERMS_ACCEPTED_FOR_CARD, false)) {
                Intent intent = new Intent(this,
                        PurchaseActivity.class);
                intent.putExtra("audioBook", audioBook);
                startActivityForResult(intent, 2);

            }else {
                showTermsAndCondition();
            }

        }else{
            Intent intent = new Intent(getApplicationContext(),
                    LoginActivity.class);
            startActivityForResult(intent, 1);
        }
    }

    private void addToMyBillButtonPressed(){
        if(isMobileDataEnable()) {
            if (serviceProvider == ServiceProvider.PROVIDER_MOBITEL) {
                paymentOption = PaymentOption.OPTION_MOBITEL;
                addToMobitelBill();
            } else if (serviceProvider == ServiceProvider.PROVIDER_ETISALAT) {
                paymentOption = PaymentOption.OPTION_ETISALAT;
                addToEtisalatBill();

            }
        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    this);
            builder.setTitle(R.string.NO_MOBILE_DATA_TITLE).setMessage(R.string.NO_MOBILE_DATA_MESSAGE).setPositiveButton(
                    R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }
    private boolean isMobileDataEnable(){

        boolean mobileYN = false;
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
        {
            mobileYN = Settings.Global.getInt(getContentResolver(), "mobile_data", 1) == 1;
        }
        else{
            mobileYN = Settings.Secure.getInt(getContentResolver(), "mobile_data", 1) == 1;
        }
        return mobileYN;

    }
    private void addToBillServerConnect(){

        Log.v("addToBillServerConnect","addToBillServerConnect 1");

        String url = "";

        if(paymentOption==PaymentOption.OPTION_MOBITEL){
            url = getResources().getString(R.string.mobitel_pay_url);
        } else if(paymentOption==PaymentOption.OPTION_ETISALAT){
            url = getResources().getString(R.string.etisalat_pay_url);
        }
        progressDialog.setMessage("Payment Processing...");
        progressDialog.show();

        Map<String, String> params = new HashMap<String, String>();
        params.put("userid", AppController.getInstance().getUserId());
        params.put("bookid", audioBook.getBook_id());
        params.put("amount", audioBook.getPrice());


        JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url, params,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("addToBillServerConnect", "addToBillServerConnect 2");


                        progressDialog.dismiss();
                        if (response.toUpperCase().contains("SUCCESS")) {
                            Log.v("addToBillServerConnect","addToBillServerConnect 3");

                            audioBook.setPurchase(true);
                            updateAudioBook(0);
                            AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                            builder.setTitle(getString(R.string.PAYMENT_COMPLETE_TITLE)).
                                    setMessage(getString(R.string.PAYMENT_COMPLETE_MESSAGE)).setPositiveButton(
                                    getString(R.string.BUTTON_NOW), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            downloadAudioFile();
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.BUTTON_LATER), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // FIRE ZE MISSILES!
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } else if (response.toUpperCase().contains("ALREADY_PAID")) {
                            Log.v("addToBillServerConnect","addToBillServerConnect 3");

                            audioBook.setPurchase(true);
                            updateAudioBook(0);
                            AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                            builder.setTitle(getString(R.string.ALREADY_PAID_TITLE)).
                                    setMessage(getString(R.string.ALREADY_PAID_MESSAGE)).setPositiveButton(
                                    getString(R.string.BUTTON_NOW), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            downloadAudioFile();
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.BUTTON_LATER), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // FIRE ZE MISSILES!
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                        else if (response.toUpperCase().contains("EMPTY_NUMBER")) {
                            String title="";
                            String message="";
                            title=getString(R.string.EMPTY_NUMBER_TITLE);

                            if(paymentOption==PaymentOption.OPTION_MOBITEL){
                                message=getString(R.string.EMPTY_NUMBER_MESSAGE_MOBITEL);
                            } else if(paymentOption==PaymentOption.OPTION_ETISALAT){
                                message=getString(R.string.EMPTY_NUMBER_MESSAGE_ETISALAT);

                            }
                            AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                            builder.setTitle(title).setMessage(message).setPositiveButton(
                                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // FIRE ZE MISSILES!
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } else{
                            Log.v("addToBillServerConnect","addToBillServerConnect 4");

                            AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                            builder.setTitle(getString(R.string.SERVER_ERROR_TITLE)).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // FIRE ZE MISSILES!
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }

                        Log.v("addToBillServerConnect","addToBillServerConnect 5");

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                Log.v("addToBillServerConnect", "addToBillServerConnect 6");

                AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                        getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        RetryPolicy mRetryPolicy = new DefaultRetryPolicy(

                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
       // stringRequest.setRetryPolicy(mRetryPolicy);
        Log.v("addToBillServerConnect", "addToBillServerConnect 7");

        AppController.getInstance().addToRequestQueue(stringRequest, "tag_mobitel_payment");
    }
    private void addToMobitelBill(){
        Log.v("addToMobitelBill","addToMobitelBill 1");

        if(AppController.getInstance().isUserLogin()){
            Log.v("addToMobitelBill","addToMobitelBill 2");

            if (connectionDetector.isConnectingToInternet()) {
                Log.v("addToMobitelBill","addToMobitelBill 3");

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                if(prefs.getBoolean(KEY_TERMS_ACCEPTED_FOR_MOBITEL, false)) {
                    Log.v("addToMobitelBill","addToMobitelBill 4");

                    AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                    builder.setTitle("Confirm Payment").setMessage("Rs:" + audioBook.getPrice() + " will be added to your Mobitel bill. Continue?").setPositiveButton(
                            getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    addToBillServerConnect();
                                }
                            })
                            .setNegativeButton(getString(R.string.BUTTON_CANCEL), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // FIRE ZE MISSILES!
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }else{
                    showTermsAndCondition();
                }

            }else{
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(getString(R.string.NO_INTERNET_MESSAGE)).setPositiveButton(
                        getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();

            }


        }else{
            Intent intent = new Intent(getApplicationContext(),
                    LoginActivity.class);
            startActivityForResult(intent, 1);
        }
    }
    private void addToEtisalatBill(){

        if(AppController.getInstance().isUserLogin()){

            if (connectionDetector.isConnectingToInternet()) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                if(prefs.getBoolean(KEY_TERMS_ACCEPTED_FOR_ETISALAT, false)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                    //Confirm Payment

                    builder.setTitle("Confirm Payment").setMessage("Rs:" + audioBook.getPrice() + " will be added to your Etisalat bill. Continue?").setPositiveButton(
                            getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    addToBillServerConnect();
                                }
                            })
                            .setNegativeButton(getString(R.string.BUTTON_CANCEL), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // FIRE ZE MISSILES!
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }else{
                    showTermsAndCondition();
                }

            }else{
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(getString(R.string.NO_INTERNET_MESSAGE)).setPositiveButton(
                        getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }


        }else{
            Intent intent = new Intent(getApplicationContext(),
                    LoginActivity.class);
            startActivityForResult(intent, 1);
        }
    }

//    private void addToDialogBill(){
//
//
//    }
    private void startTimer(){
        if( timerUpdateThread != null) {
            timerUpdateThread.interrupt();
        }
        timerUpdateThread = new Thread( this );
        timerUpdateThread.start();
    }
    private void stopTimer(){
        if( timerUpdateThread != null ) {
            timerUpdateThread.interrupt();
        }
    }

    private void releaseMediaPlayer(){
        if (mediaPlayer != null){
            if(mediaPlayer.isPlaying())
                mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer=null;

        }
        stopTimer();
    }


    private void initActivityTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide transition = new Slide();
            transition.excludeTarget(android.R.id.statusBarBackground, true);
            getWindow().setEnterTransition(transition);
            getWindow().setReturnTransition(transition);
        }

    }
    private void postponeTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
        } else {
            ActivityCompat.postponeEnterTransition(this);
        }
    }

    private void startPostponedTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startPostponedEnterTransition();
        } else {
            ActivityCompat.startPostponedEnterTransition(this);
        }
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
    private void saveCoverImage() {
        String dirPath = AppUtils.getDataDirectory(getApplicationContext())
                + audioBook.getBook_id()+File.separator;
        bookCoverImage.buildDrawingCache();
        Bitmap bitmapImage=bookCoverImage.getDrawingCache();
        //Bitmap resized = Bitmap.createScaledBitmap(bitmapImage, (int)(160*2), (int)(240*2), true);

        OutputStream fOut = null;
        Uri outputFileUri;
        try {
            File fileDir = new File(dirPath);
            if (!fileDir.exists()) {
                fileDir.mkdirs();

            }
            File filepath = new File(fileDir, "book_cover.jpg");

            FileOutputStream fos = null;
            try {

                fos = new FileOutputStream(filepath);

                // Use the compress method on the BitMap object to write image to the OutputStream
                bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*
        if(bm !=null) {
            File fileDir = new File(dirPath);
            if (!fileDir.exists()) {
                fileDir.mkdirs();

            }
            Random generator = new Random();
            int n = 10000;
            n = generator.nextInt(n);
            String fname = "book_cover.jpg";
            File file = new File(fileDir, fname);
            if (file.exists()) file.delete();
            try {
                FileOutputStream out = new FileOutputStream(file);
                bm.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        */
    }

    @Override
    public void run() {
        int currentPosition = 0;//
        while (mediaPlayer != null && mediaPlayer.isPlaying() && currentPosition < mediaPlayer.getDuration()) {
            try {
                Thread.sleep(1000);
                currentPosition = mediaPlayer.getCurrentPosition();

            } catch (Exception e) {
                e.printStackTrace();
            }
            updateTimer();
        }
    }
    private void updateTimer() {
        int currentPosition = mediaPlayer.getCurrentPosition();
        int totalDuration =mediaPlayer.getDuration();
        leftTime= AppUtils.milliSecondsToTimer(totalDuration - currentPosition);
        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(this.getMainLooper());

        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                updatePreviewLayout();

            } // This is your code
        };
        mainHandler.post(timerRunnable);
    }

    @Override
    public void onPostExecute(String result,String file_name) {
        Log.v("onPostExecute", "onPostExecute" + file_name + "result" + result);
        if (result != null && result.equalsIgnoreCase("UNAUTHORISED")){
            showMessage("UNAUTHORISED");

        }else if(result != null && result.equalsIgnoreCase("NOTFOUND")){
            showMessage("NOTFOUND");

        }else {
            mProgressDialog.setMessage("Downloading " + (audioBook.getDownloadedChapter().size() + 1) + " of " + audioBook.getAudioFileCount());

            downloadedFileCount++;
            if (result == null) {
                updateAudioBook(Integer.parseInt(file_name));

                if (totalAudioFileCount == downloadedFileCount) {
                    downloadAudioFile();
                }
            }
        }
    }

    /* Download AsyncTask*/
    /*
    public class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private String file_name;

        public DownloadTask(Context context, Dialog dialog,
                            ProgressBar progressBar, TextView progressTextView,
                            String destinationPath, String fileName, JSONObject jObject) {
            this.context = context;
        }

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            String directory = sUrl[0];
            String fileName = sUrl[1];
            file_name=fileName;
            String urlString = getResources().getString(R.string.book_download_url);
            // http://app.lisn.audio/api/download.php?userid=1&bookid=1&part=1
            String urlParameters  = "userid="+AppController.getInstance().getUserId()+"&bookid="+audioBook.getBook_id()+"&part="+fileName;
            // String urlParameters =  "{\"userid\": \"1\",\"bookid\":audioBook.getBook_id()}";
            Log.v("urlParameters",""+urlParameters);
            byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
            int    postDataLength = postData.length;
//Create JSONObject here


            //  file_url=sUrl[2];

            // prevent CPU from going off if the user presses the power button
            // during download
            PowerManager pm = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            wl.acquire();

            // download
            try {
                File rootPath = new File(directory);
                if (!rootPath.exists()) {
                    rootPath.mkdirs();
                }
                // new File(directory).mkdirs();
                InputStream input = null;
                OutputStream output = null;
                // CipherOutputStream cos = null;
                HttpURLConnection connection = null;
                try {
                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("userid", AppController.getInstance().getUserId());
                    jsonParam.put("bookid", audioBook.getBook_id());
                    jsonParam.put("part", fileName);

                    // connect to url
                    URL url = new URL(urlString);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    //connection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
                    //connection.setRequestProperty("Content-Type","application/json");
                    connection.setRequestProperty( "charset", "utf-8");
                    connection.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
                    connection.setUseCaches (false);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    DataOutputStream printout = new DataOutputStream(connection.getOutputStream ());
                    // printout.write(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                    printout.writeBytes(urlParameters.toString());

                    printout.flush ();
                    printout.close ();
//                    try( DataOutputStream wr = new DataOutputStream( connection.getOutputStream())) {
//                        wr.write( postData );
//                    }

                    connection.connect();

                    // check for http_ok (200)
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                        return "Server returned HTTP "
                                + connection.getResponseCode() + " "
                                + connection.getResponseMessage();
                    Log.v("connection","connection"+connection.getResponseCode());
                   // Log.v("connection","connection"+connection.get);

                    int fileLength = connection.getContentLength();
                    Log.v("fileLength",""+fileLength);

                    if(fileLength<20){
                        InputStream is = null;
                        try {
                            is = connection.getInputStream();
                            int ch;
                            StringBuffer sb = new StringBuffer();
                            while ((ch = is.read()) != -1) {
                                sb.append((char) ch);
                            }
                            return sb.toString();
                        } catch (IOException e) {
                            throw e;
                        }
                    }
                    // download the file
                    input = connection.getInputStream();
                    output = new FileOutputStream(directory + "/" + fileName
                            + ".lisn");// change extension

                    // copying
                    byte data[] = new byte[4096];
                    int count;
                    long total = 0;

//                    SecretKeySpec sks = new SecretKeySpec(
//                            "Mary has one cat".getBytes(), "AES");
                    // Create cipher
                    //   Cipher cipher = Cipher.getInstance("AES");
                    //  cipher.init(Cipher.ENCRYPT_MODE, sks);
                    // Wrap the output stream
                    //  cos = new CipherOutputStream(output, cipher);

                    while ((count = input.read(data)) != -1) {

                        // allow canceling with back button
                        if (isCancelled()) {
                            input.close();
                            return null;
                        }
                        total += count;
                        // publishing the progress....
                        //if (fileLength > 0) // only if total length is known
                        //	publishProgress((int) (total * 100 / fileLength));
                        output.write(data, 0, count);
                    }

                } catch (Exception e) {
                    return e.toString();
                } finally // closing streams and connection
                {
                    try {


                        if (input != null)
                            input.close();
                    } catch (IOException ignored) {
                    }

                    if (connection != null)
                        connection.disconnect();
                }
                if(isCancelled() && (connection != null)){
                    Log.v("isCancelled","disconnect");
                    connection.disconnect();
                }
            } finally {
                wl.release(); // release the lock screen
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            // PowerManager pm = (PowerManager)
            // context.getSystemService(Context.POWER_SERVICE);
            // mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
            // getClass().getName());
            // mWakeLock.acquire();

        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            //mProgressDialog.setIndeterminate(false);
            //mProgressDialog.setMax(100);
            //mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {

            if (result != null && result.equalsIgnoreCase("UNAUTHORISED")){
                showMessage("UNAUTHORISED");

            }else if(result != null && result.equalsIgnoreCase("NOTFOUND")){
                showMessage("NOTFOUND");

            }else {
                mProgressDialog.setMessage("Downloading " + (audioBook.getDownloadedChapter().size() + 1) + " of " + audioBook.getAudioFileCount());

                downloadedFileCount++;
                if (result == null) {
                    updateAudioBook(Integer.parseInt(file_name));


                    // audioBook.addFileToDownloadedList(file_name, file_url);
                    //updateAudioBook(null,null);
                    if (totalAudioFileCount == downloadedFileCount) {
                        //mProgressDialog.dismiss();
                        downloadAudioFile();
                    }
                }
            }
            // mWakeLock.release();
            //mProgressDialog.dismiss();
            // if (result != null)
            // Toast.makeText(context,"Download error: "+result,
            // Toast.LENGTH_LONG).show();
            // else
            // Toast.makeText(context,"File downloaded",
            // Toast.LENGTH_SHORT).show();
        }

    }
    */
    private void showMessage(String result){
        stopDownload();
        mProgressDialog.dismiss();

        if (result.equalsIgnoreCase("UNAUTHORISED")){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(R.string.USER_UNAUTHORISED_TITLE).setMessage(getString(R.string.USER_UNAUTHORISED_MESSAGE)).setPositiveButton(
                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();

        }else if(result.equalsIgnoreCase("NOTFOUND")){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void initiatePopupWindow(float rating) {
        try {
// We need to get the instance of the LayoutInflater
            LayoutInflater inflater = (LayoutInflater) AudioBookDetailActivity.this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.rate_book_popup,
                    (ViewGroup) findViewById(R.id.popup_element));
            // pwindo = new PopupWindow(layout, 300, 400, true);
            //  pwindo = new PopupWindow(layout);
            pwindo = new PopupWindow(layout, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT,true);

            pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);
            final RatingBar ratingBar = (RatingBar) layout.findViewById(R.id.rating_bar);
            LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
            stars.getDrawable(2).setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP);

            final TextView reviewTitle = (TextView) layout.findViewById(R.id.review_title);
            final TextView reviewDescription = (TextView) layout.findViewById(R.id.review_description);
            ratingBar.setRating(rating);

            Button btnClosePopup = (Button) layout.findViewById(R.id.btn_submit);
            btnClosePopup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    publishUserReview(ratingBar.getRating(),reviewTitle.getText().toString(),reviewDescription.getText().toString());

                }
            });
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pwindo.dismiss();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void updateReview(float rate,String title,String comment){
        ArrayList<BookReview> reviews=audioBook.getReviews();
        BookReview bookReview=new BookReview();
        bookReview.setRateValue("" + rate);
        bookReview.setTitle(title);
        bookReview.setMessage(comment);
        bookReview.setUserId(AppController.getInstance().getUserId());
        bookReview.setUserName(AppController.getInstance().getUserName());
       // bookReview.setTimeString(Date.);

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
        bookReview.setTimeString(dateFormat.format(date));



        reviews.add(bookReview);
        audioBook.setReviews(reviews);

        updateData();
    }
    private void publishUserReview( final float rate, final String title, final String comment){


        if (connectionDetector.isConnectingToInternet()) {
            if (rate > 0 && title.length() > 0 && comment.length() > 0) {

                progressDialog.setMessage("Publishing...");
                progressDialog.show();

                Map<String, String> params = new HashMap<String, String>();
                params.put("userid", AppController.getInstance().getUserId());
                params.put("bookid", audioBook.getBook_id());
                params.put("rate", "" + rate);
                params.put("title", title);
                params.put("comment", comment);

                String url = getResources().getString(R.string.add_review_url);

                JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url, params,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                pwindo.dismiss();
                                progressDialog.dismiss();
                                updateReview(rate, title, comment);

                                Log.v("response", "response:" + response);
                                Toast toast = Toast.makeText(getApplicationContext(), R.string.REVIEW_PUBLISH_SUCCESS, Toast.LENGTH_SHORT);
                                toast.show();


                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pwindo.dismiss();
                        progressDialog.dismiss();

                        Toast toast = Toast.makeText(getApplicationContext(), "Review publish failed try again later", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });

                AppController.getInstance().addToRequestQueue(stringRequest, "tag_review_book");
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_valid_data), Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        else{
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(getString(R.string.NO_ENOUGH_SPACE_MESSAGE)).setPositiveButton(
                    getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v("onActivityResult","onActivityResult");

        if (requestCode == 1) {
            if(resultCode == Constants.RESULT_SUCCESS){
                if(paymentOption == PaymentOption.OPTION_NONE){
                    playGetButtonPressed();
                }else if(paymentOption == PaymentOption.OPTION_MOBITEL){
                    addToMobitelBill();

                }else if(paymentOption == PaymentOption.OPTION_ETISALAT){
                    addToEtisalatBill();

                }else if(paymentOption == PaymentOption.OPTION_CARD){
                    buyFromCardButtonPressed();

                }
            }
            if (resultCode == Constants.RESULT_ERROR) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                        getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
        else if (requestCode == 2) {
            if(resultCode == Constants.RESULT_SUCCESS){
                audioBook.setPurchase(true);
                updateAudioBook(0);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.PAYMENT_COMPLETE_TITLE).setMessage(getString(R.string.PAYMENT_COMPLETE_MESSAGE)).setPositiveButton(
                        R.string.BUTTON_NOW, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                downloadAudioFile();
                            }
                        })
                        .setNegativeButton(R.string.BUTTON_LATER, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();


            }else if (resultCode == Constants.RESULT_SUCCESS_ALREADY) {
                Log.v("addToBillServerConnect","addToBillServerConnect 3");

                audioBook.setPurchase(true);
                updateAudioBook(0);
                AlertDialog.Builder builder = new AlertDialog.Builder(AudioBookDetailActivity.this);
                builder.setTitle(getString(R.string.ALREADY_PAID_TITLE)).
                        setMessage(getString(R.string.ALREADY_PAID_MESSAGE)).setPositiveButton(
                        getString(R.string.BUTTON_NOW), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                downloadAudioFile();
                            }
                        })
                        .setNegativeButton(getString(R.string.BUTTON_LATER), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
            if (resultCode == Constants.RESULT_ERROR) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                        getString(R.string.BUTTON_OK), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }

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
            if(AudioPlayerService.mediaPlayer!=null && AudioPlayerService.mediaPlayer.isPlaying()) {
                releaseMediaPlayer();
            }
        }
    };

    private class DownloadReceiver extends ResultReceiver {
        public DownloadReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if (resultCode == DownloadService.UPDATE_PROGRESS) {
                Log.v("DownloadService","DownloadService :"+resultData);
                int progress = resultData.getInt("progress");
//                mProgressDialog.setProgress(progress);
//                if (progress == 100) {
//                    mProgressDialog.dismiss();
//                }

                String result=resultData.getString("result");
                String file_name=resultData.getString("file_name");


                    if (result != null && result.equalsIgnoreCase("UNAUTHORISED")){
                        showMessage("UNAUTHORISED");

                    }else if(result != null && result.equalsIgnoreCase("NOTFOUND")){
                        showMessage("NOTFOUND");

                    }else if(result != null && result.equalsIgnoreCase("OK")){
                        Log.v("DownloadService","DownloadService getDownloadedChapter :"+audioBook.getDownloadedChapter().size());

                        mProgressDialog.setMessage("Downloading " + (audioBook.getDownloadedChapter().size() + 1) + " of " + audioBook.getAudioFileCount());

                        downloadedFileCount++;
                       // if (result == null) {
                            updateAudioBook(Integer.parseInt(file_name));

                            if (totalAudioFileCount == downloadedFileCount) {
                                downloadAudioFile();
                            }
                       // }
                    }
            }
        }
    }
    private void pausePlayer() {
        Intent intent = new Intent(Constants.PLAYER_STATE_CHANGE);
        intent.putExtra("state", "pause");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }


}
