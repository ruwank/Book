package audio.lisn.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RatingBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.technikum.mti.fancycoverflow.FancyCoverFlow;
import audio.lisn.R;
import audio.lisn.adapter.CoverFlowAdapter;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.model.DownloadedAudioBook;
import audio.lisn.util.AppUtils;
import audio.lisn.util.AudioPlayerService;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;
import audio.lisn.util.CustomTypeFace;
import audio.lisn.webservice.FileDownloadTask;
import audio.lisn.webservice.FileDownloadTaskListener;
import audio.lisn.webservice.JsonUTF8StringRequest;

//import android.renderscript.RenderScript;
//import android.renderscript.ScriptIntrinsicBlur;

//import it.moondroid.coverflow.components.ui.containers.FeatureCoverFlow;

public class PlayerControllerActivity extends AppCompatActivity implements FileDownloadTaskListener{
    private static final String TRANSITION_NAME = "audio.lisn.PlayerControllerActivity";
   // AudioBook audioBook;
    private FancyCoverFlow mCoverFlow;
    private CoverFlowAdapter mAdapter;
    private TextSwitcher mTitle;
    private List<AudioBook> bookList =new ArrayList<>(0);
    Intent playbackServiceIntent;
    public ImageButton previousItemPlayButton,playPauseButton,
            nextItemPlayButton,playStopButton;
    public  TextView audioTitle,musicCurrentLoc,musicDuration;
    public DiscreteSeekBar musicSeekBar;
    ProgressDialog mProgressDialog;
    int totalAudioFileCount, downloadedFileCount;
    AudioBook audioBook;
    List<FileDownloadTask> downloadingList = new ArrayList<FileDownloadTask>();
    ConnectionDetector connectionDetector;
    ImageView bgImageView;
    private PopupWindow pwindo;
    ProgressDialog progressDialog;
    ImageButton commentButton;
View topOverLayView;



    public static void navigate(AppCompatActivity activity, View transitionView, AudioBook audioBook) {
        Intent intent = new Intent(activity, PlayerControllerActivity.class);
        if(audioBook !=null)
        intent.putExtra("audioBook", audioBook);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, transitionView, TRANSITION_NAME);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActivityTransitions();
        setContentView(R.layout.activity_player_controller);
        ViewCompat.setTransitionName(findViewById(R.id.app_bar_layout), TRANSITION_NAME);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        supportStartPostponedEnterTransition();
        getSupportActionBar().setTitle(R.string.app_name);

        bgImageView=(ImageView)findViewById(R.id.bgImageView);
        topOverLayView=(View)findViewById(R.id.topOverLayView);
        setupData();
        connectionDetector = new ConnectionDetector(getApplicationContext());

        if (this.getIntent().getExtras() != null && this.getIntent().getExtras().containsKey("audioBook")) {
            audioBook = (AudioBook) getIntent().getSerializableExtra("audioBook");
            downloadAudioFile();
        }else{
            audioBook = AppController.getInstance().getCurrentAudioBook();

        }
        TextView textView= (TextView) mTitle.getCurrentView();

        if(audioBook.getLanguageCode()== AudioBook.LanguageCode.LAN_SI){
            textView.setTypeface(CustomTypeFace.getSinhalaTypeFace(this));
        }else{
            textView.setTypeface(CustomTypeFace.getEnglishTypeFace(this));
        }
        mTitle.setText(audioBook.getTitle());
        setCoverFlowPosition();
        progressDialog = new ProgressDialog(PlayerControllerActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Publishing...");



    }

    @Override
    protected void onResume() {
        super.onResume();
        registerBroadcastReceiver();


    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPlayerUpdateReceiver);
    }

    private void setCoverFlowPosition(){

        int position=0;
        if(audioBook !=null){
            for (int i = 0; i <bookList.size() ; i++) {
                AudioBook book=bookList.get(i);
                if( audioBook.getBook_id().equalsIgnoreCase(book.getBook_id()) ){
                    position=i;
                    break;
                }
            }
        }
Log.v("position","position:"+position);
        mCoverFlow.setSelection(position);
        setBookTitle(position);

    }
    private void setupData(){

        bookList.clear();
        DownloadedAudioBook downloadedAudioBook=new DownloadedAudioBook(this);
        downloadedAudioBook.readFileFromDisk(this);
        HashMap< String, AudioBook> hashMap=downloadedAudioBook.getBookList();
        for (AudioBook item : hashMap.values()) {
            bookList.add(item);
        }

        mTitle = (TextSwitcher) findViewById(R.id.play_book_title);
        mTitle.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                LayoutInflater inflater = LayoutInflater.from(PlayerControllerActivity.this);
                TextView textView = (TextView) inflater.inflate(R.layout.play_book_title, null);

                return textView;
            }
        });
        Animation in = AnimationUtils.loadAnimation(this, R.anim.slide_in_top);
        Animation out = AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom);
        mTitle.setInAnimation(in);
        mTitle.setOutAnimation(out);



        mAdapter = new CoverFlowAdapter(this);
        mAdapter.setData(bookList);
        mCoverFlow = (FancyCoverFlow) findViewById(R.id.coverflow);
        mCoverFlow.setAdapter(mAdapter);
        mCoverFlow.setReflectionEnabled(true);
        mCoverFlow.setReflectionRatio(0.3f);
        mCoverFlow.setReflectionGap(0);
        mCoverFlow.setMaxRotation(45);
        mCoverFlow.setSpacing(-50);
        mCoverFlow.setUnselectedScale(0.8f);
       // mCoverFlow.setUnselectedAlpha(1.0f);
       // mCoverFlow.setReflectionHeight(0.2f);
       // mCoverFlow.setShouldRepeat(false);

        mCoverFlow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                audioBook = bookList.get(position);
                downloadAudioFile();
                stopAudioPlayer();
                setBookTitle(position);


                //  mCoverFlow.scrollToPosition(position);
//                Toast.makeText(PlayerControllerActivity.this,bookList.get(position).getEnglish_title(),
//                        Toast.LENGTH_SHORT).show();
            }
        });

//        mCoverFlow.setOnScrollPositionListener(new FeatureCoverFlow.OnScrollPositionListener() {
//            @Override
//            public void onScrolledToPosition(int position) {
//                setBookTitle(position);
//            }
//
//            @Override
//            public void onScrolling() {
//
//                mTitle.setText("");
//            }
//        });



        previousItemPlayButton=(ImageButton)this.findViewById(R.id.previousItemPlayButton);
        playPauseButton=(ImageButton)this.findViewById(R.id.playPauseButton);
        nextItemPlayButton=(ImageButton)this.findViewById(R.id.nextItemPlayButton);
        playStopButton=(ImageButton)this.findViewById(R.id.playstopButton);
        commentButton=(ImageButton)this.findViewById(R.id.commentButton);

        audioTitle=(TextView)this.findViewById(R.id.audioTitle);
        musicCurrentLoc=(TextView)this.findViewById(R.id.musicCurrentLoc);
        musicDuration=(TextView)this.findViewById(R.id.musicDuration);
        musicSeekBar=(DiscreteSeekBar)this.findViewById(R.id.musicSeekBar);
        previousItemPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                AppController.getInstance().playPreviousFile();

            }

        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                playPauseAudio();

            }

        });
        nextItemPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                AppController.getInstance().playNextFile();


            }

        });

        commentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                initiatePopupWindow();

            }

        });

        musicSeekBar.setOnProgressChangeListener(new SeekBarChangeEvent());
       // musicSeekBar.setOnSeekBarChangeListener(new SeekBarChangeEvent());


        playStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                stopAudioPlayer();

            }

        });

        mProgressDialog = new ProgressDialog(PlayerControllerActivity.this);
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


    }
    private void stopDownload(){
        for (int i = 0; i < downloadingList.size(); i++) {
            FileDownloadTask downloadTask = downloadingList.get(i);
            downloadTask.cancel(true);
        }
    }
    private void playPauseAudio(){
        if((AudioPlayerService.mediaPlayer!=null) && AudioPlayerService.mediaPlayer.isPlaying()){
          //  AudioPlayerService.mediaPlayer.pause(); // ?
            playPauseButton.setImageResource(R.drawable.btn_play_start);
            sendStateChange("pause");
        }else if(AudioPlayerService.mediaPlayer!=null){
            playPauseButton.setImageResource(R.drawable.btn_play_pause);
           // AudioPlayerService.mediaPlayer.start(); // ?
            sendStateChange("start");

        }

    }
    private void stopAudioPlayer(){

        if(AudioPlayerService.mediaPlayer!=null){
            playPauseButton.setImageResource(R.drawable.btn_play_start);
            sendStateChange("stop");
            AppController.getInstance().stopPlayer();

        }


    }
private void setBookTitle(int position){

   // AudioBook book = bookList.get(position);
    TextView textView= (TextView) mTitle.getCurrentView();
   // Log.v("book.getLanguageCode()",""+book.getLanguageCode());

    if(audioBook.getLanguageCode()== AudioBook.LanguageCode.LAN_SI){
        textView.setTypeface(CustomTypeFace.getSinhalaTypeFace(this));
    }else{
        textView.setTypeface(CustomTypeFace.getEnglishTypeFace(this));
    }
    mTitle.setText(audioBook.getTitle());

    String img_path = AppUtils.getDataDirectory(getApplicationContext())
            + audioBook.getBook_id()+ File.separator+"book_cover.jpg";

    File imgFile = new  File(img_path);
    Bitmap bitmap=null;
    if(imgFile.exists()) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), bmOptions);
    }else{
        bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.ui_bg_logo);
    }
    if (Build.VERSION.SDK_INT >= 18) {

        Bitmap blurred = blurRenderScript(bitmap, 25);//second parametre is radius
        bgImageView.setImageBitmap(blurred);
    }else{
        topOverLayView.setBackgroundResource(R.color.color_transparent_alpha_0_8);
        bgImageView.setImageBitmap(bitmap);

    }
   // bgImageView.getDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY );



    // setBookTitle


}


    private void initActivityTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide transition = new Slide();
            transition.excludeTarget(android.R.id.statusBarBackground, true);
            getWindow().setEnterTransition(transition);
            getWindow().setReturnTransition(transition);
        }
    }
    private void starAudioPlayer(){
        mProgressDialog.dismiss();

        if(playbackServiceIntent == null)
            playbackServiceIntent= AppController.getInstance().getPlaybackServiceIntent();

        String[] fileList=new String[audioBook.getDownloadedChapter().size()];
        for (int i=0; i<audioBook.getDownloadedChapter().size();i++){
            fileList[i]= AppUtils.getDataDirectory(getApplicationContext())+audioBook.getBook_id()+"/"+(i+1)+".lisn";

        }
        AppController.getInstance().setFileList(fileList);
        AppController.getInstance().setCurrentAudioBook(audioBook);
        int lastPlayFileIndex=audioBook.getLastPlayFileIndex();
        if(lastPlayFileIndex<1){
            lastPlayFileIndex=0;
        }
        AppController.getInstance().fileIndex=(audioBook.getLastPlayFileIndex()-1);
        stopService(playbackServiceIntent);
        startService(playbackServiceIntent);
        AppController.getInstance().playNextFile();
        Log.v("book.getLanguageCode()",""+audioBook.getLanguageCode());


    }
    //Downlaod file
    private void downloadAudioFileFromUrl(int filePart){

        if (connectionDetector.isConnectingToInternet()) {
            String dirPath = AppUtils.getDataDirectory(getApplicationContext())
                    + audioBook.getBook_id()+ File.separator;
            File file = new File(dirPath + filePart + ".lisn");

            if (file.exists()) {
                file.delete();
            }
            FileDownloadTask downloadTask =  new FileDownloadTask(this,this,audioBook.getBook_id());
            downloadTask.execute(dirPath, "" + filePart);
            downloadingList.add(downloadTask);

        }else{
           AlertDialog.Builder builder = new AlertDialog.Builder(
                    this);
            builder.setMessage(getString(R.string.no_internet)).setPositiveButton(
                    "OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
    private void downloadAudioFile() {
        String dirPath = AppUtils.getDataDirectory(getApplicationContext())
                + audioBook.getBook_id()+File.separator;
        if (connectionDetector.isConnectingToInternet()) {
            //  updateAudioBook(null,null);


            mProgressDialog.show();

            downloadedFileCount=0;
            // String [] file_urls=audioBook.getAudio_file_urls();
            //totalAudioFileCount=file_urls.length;
            totalAudioFileCount=0;
            downloadingList.clear();

            //  HashMap fileList= audioBook.getDownloadedFileList();

            for (int filePart=1; filePart<=(audioBook.getAudioFileCount()); filePart++){
                File file = new File(dirPath +filePart+".lisn");

                if (!file.exists() ||  !(audioBook.getDownloadedChapter().contains(filePart)) ) {
                    downloadAudioFileFromUrl(filePart);
                    totalAudioFileCount++;
                }


            }
            if(downloadedFileCount ==totalAudioFileCount){
                mProgressDialog.dismiss();
                starAudioPlayer();
            }else{
                if(AppUtils.getAvailableMemory() < audioBook.getFileSize()){
                    stopDownload();

                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            this);
                    builder.setMessage(R.string.no_enough_space).setPositiveButton(
                            "OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // FIRE ZE MISSILES!
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();

                }else {
                    mProgressDialog.setMessage("Downloading " + (audioBook.getDownloadedChapter().size() + 1) + " of " + audioBook.getAudioFileCount());
                }

            }

        } else {

            downloadedFileCount=0;
            totalAudioFileCount=0;

            for (int filePart=1; filePart<=(audioBook.getAudioFileCount()); filePart++){
                File file = new File(dirPath +filePart+".lisn");
                if (!file.exists()) {
                    totalAudioFileCount++;
                }


            }
            if(downloadedFileCount ==totalAudioFileCount){
                mProgressDialog.dismiss();
                starAudioPlayer();
            }else{
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                        this);
                builder.setMessage("No Internet Connection").setPositiveButton(
                        "OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                android.app.AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }
    private void registerBroadcastReceiver(){
        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mPlayerUpdateReceiver,
                new IntentFilter("audio-event"));
    }
    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mPlayerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            updateView();
        }
    };
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

    @Override
    public void onPostExecute(String result, String file_name) {
        if (result != null && result.equalsIgnoreCase("UNAUTHORISED")) {
            showMessage("UNAUTHORISED");

        } else if (result != null && result.equalsIgnoreCase("NOTFOUND")) {
            showMessage("NOTFOUND");

        } else {
            mProgressDialog.setMessage("Downloading " + (audioBook.getDownloadedChapter().size() + 1) + " of " + audioBook.getAudioFileCount());

            downloadedFileCount++;
            if (result == null) {
                updateAudioBook(Integer.parseInt(file_name));

                if (totalAudioFileCount == downloadedFileCount) {
                    mProgressDialog.dismiss();
                    downloadAudioFile();
                }
            }


        }

    }

    class SeekBarChangeEvent implements DiscreteSeekBar.OnProgressChangeListener {
        @Override
        public void onProgressChanged(DiscreteSeekBar discreteSeekBar, int progress, boolean fromUser) {
            if (fromUser) {
                if(AudioPlayerService.mediaPlayer!=null) {
                    AudioPlayerService.mediaPlayer.seekTo(progress);// ?
                }
            }
        }

        @Override
        public void onStartTrackingTouch(DiscreteSeekBar discreteSeekBar) {
            if(AudioPlayerService.mediaPlayer!=null) {
                playPauseButton.setImageResource(R.drawable.btn_play_start);
                AudioPlayerService.mediaPlayer.pause(); // ?

            }
        }

        @Override
        public void onStopTrackingTouch(DiscreteSeekBar discreteSeekBar) {
            if(AudioPlayerService.mediaPlayer!=null) {
                playPauseButton.setImageResource(R.drawable.btn_play_pause);
                AudioPlayerService.mediaPlayer.start(); // ?
            }
        }
    }
    /*
    class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {

            if (fromUser) {
                if(AudioPlayerService.mediaPlayer!=null) {
                    AudioPlayerService.mediaPlayer.seekTo(progress);// ?
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if(AudioPlayerService.mediaPlayer!=null) {
                playPauseButton.setImageResource(R.drawable.btn_play_start);
                AudioPlayerService.mediaPlayer.pause(); // ?
                // mState = State.Playing;

            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if(AudioPlayerService.mediaPlayer!=null) {
                playPauseButton.setImageResource(R.drawable.btn_play_pause);
                AudioPlayerService.mediaPlayer.start(); // ?
            }
        }
    }
*/
    public void updateView(){
        audioTitle.setText(AppController.getInstance().getPlayerControllerTitle());

        if(AudioPlayerService.mediaPlayer!=null){

            musicSeekBar.setMax(AudioPlayerService.audioDuration);
            musicSeekBar.setProgress(AudioPlayerService.mediaPlayer.getCurrentPosition());
            musicCurrentLoc.setText(milliSecondsToTimer(AudioPlayerService.mediaPlayer.getCurrentPosition()));
            musicDuration.setText(milliSecondsToTimer(AudioPlayerService.audioDuration));

            if(AudioPlayerService.mediaPlayer.isPlaying()){
                playPauseButton.setImageResource(R.drawable.btn_play_pause);

            }else {
                playPauseButton.setImageResource(R.drawable.btn_play_start);


            }
        }

        audioTitle.setText(AppController.getInstance().getPlayerControllerTitle());
    }

    public String milliSecondsToTimer(long milliseconds){
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int)( milliseconds / (1000*60*60));
        int minutes = (int)(milliseconds % (1000*60*60)) / (1000*60);
        int seconds = (int) ((milliseconds % (1000*60*60)) % (1000*60) / 1000);
        // Add hours if there
        if(hours > 0){
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if(seconds < 10){
            secondsString = "0" + seconds;
        }else{
            secondsString = "" + seconds;}

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }
    // Send an Intent with an action named "my-event".
    private void sendStateChange(String state) {
        Intent intent = new Intent(Constants.PLAYER_STATE_CHANGE);
        // add data
        intent.putExtra("state", state);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void bookmarkAudioBook(){
        AudioBook audioBook=AppController.getInstance().getCurrentAudioBook();
        Log.v("bookmarkAudioBook", "LastPlayFileIndex:" + audioBook.getLastPlayFileIndex());
        Log.v("bookmarkAudioBook","LastSeekPoint:"+audioBook.getLastSeekPoint());

        DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(
                this);
        downloadedAudioBook.addBookToList(this,
                audioBook.getBook_id(), audioBook);

    }
    private void updateAudioBook(int chapter){
        if(chapter>0) {
            audioBook.addChapterToDownloadedChapter(chapter);
        }
        DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(
                getApplicationContext());
        downloadedAudioBook.readFileFromDisk(getApplicationContext());
        downloadedAudioBook.addBookToList(getApplicationContext(),
                audioBook.getBook_id(), audioBook);

    }
    private Bitmap blurRenderScript(Bitmap smallBitmap, int radius) {

        try {
            smallBitmap = RGB565toARGB888(smallBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }


        Bitmap bitmap = Bitmap.createBitmap(
                smallBitmap.getWidth(), smallBitmap.getHeight(),
                Bitmap.Config.ARGB_8888);

        RenderScript renderScript = RenderScript.create(getApplicationContext());

        Allocation blurInput = Allocation.createFromBitmap(renderScript, smallBitmap);
        Allocation blurOutput = Allocation.createFromBitmap(renderScript, bitmap);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript,
                Element.U8_4(renderScript));
        blur.setInput(blurInput);
        blur.setRadius(radius); // radius must be 0 < r <= 25
        blur.forEach(blurOutput);

        blurOutput.copyTo(bitmap);
        renderScript.destroy();

        return bitmap;

    }

    private Bitmap RGB565toARGB888(Bitmap img) throws Exception {
        int numPixels = img.getWidth() * img.getHeight();
        int[] pixels = new int[numPixels];

        //Get JPEG pixels.  Each int is the color values for one pixel.
        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());

        //Create a Bitmap of the appropriate format.
        Bitmap result = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.ARGB_8888);

        //Set RGB pixels.
        result.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());
        return result;
    }
    private void initiatePopupWindow() {
        try {
// We need to get the instance of the LayoutInflater
            LayoutInflater inflater = (LayoutInflater) PlayerControllerActivity.this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.rate_book_popup,
                    (ViewGroup) findViewById(R.id.popup_element));
            pwindo = new PopupWindow(layout, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT,true);

            pwindo.showAtLocation(layout, Gravity.CENTER, 0, 0);
            final RatingBar ratingBar = (RatingBar) layout.findViewById(R.id.rating_bar);
            final TextView reviewTitle = (TextView) layout.findViewById(R.id.review_title);
            final TextView reviewDescription = (TextView) layout.findViewById(R.id.review_description);

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
    private void publishUserReview( float rate,String title,String comment){


        if (connectionDetector.isConnectingToInternet()) {
            if(rate>0 && title.length()>0 && comment.length()>0) {


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

                                Log.v("response", "response:" + response);
                                Toast toast = Toast.makeText(getApplicationContext(), "Review Publish Success", Toast.LENGTH_SHORT);
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
            }else{
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_valid_data), Toast.LENGTH_SHORT);
                toast.show();
            }
        }else{
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_internet), Toast.LENGTH_SHORT);
            toast.show();
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
            file_name = fileName;
            String urlString = getResources().getString(R.string.book_download_url);
            String urlParameters = "userid=1&bookid=" + audioBook.getBook_id() + "&part=" + fileName;
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;

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
                InputStream input = null;
                OutputStream output = null;
                HttpURLConnection connection = null;
                try {
                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("userid", "1");
                    jsonParam.put("bookid", audioBook.getBook_id());
                    jsonParam.put("part", fileName);

                    // connect to url
                    URL url = new URL(urlString);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("charset", "utf-8");
                    connection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                    connection.setUseCaches(false);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    DataOutputStream printout = new DataOutputStream(connection.getOutputStream());
                    printout.writeBytes(urlParameters.toString());

                    printout.flush();
                    printout.close();


                    connection.connect();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                        return "Server returned HTTP "
                                + connection.getResponseCode() + " "
                                + connection.getResponseMessage();

                    int fileLength = connection.getContentLength();
                    if (fileLength < 20) {
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
                    Log.v("fileLength", "" + fileLength);

                    // download the file
                    input = connection.getInputStream();
                    output = new FileOutputStream(directory + "/" + fileName
                            + ".lisn");// change extension

                    // copying
                    byte data[] = new byte[4096];
                    int count;
                    long total = 0;


                    while ((count = input.read(data)) != -1) {

                        if (isCancelled()) {
                            input.close();
                            return null;
                        }
                        total += count;

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
                if (isCancelled() && (connection != null)) {
                    Log.v("isCancelled", "disconnect");
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


        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);

        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && result.equalsIgnoreCase("UNAUTHORISED")) {
                showMessage("UNAUTHORISED");

            } else if (result != null && result.equalsIgnoreCase("NOTFOUND")) {
                showMessage("NOTFOUND");

            } else {
                mProgressDialog.setMessage("Downloading " + (audioBook.getDownloadedChapter().size() + 1) + " of " + audioBook.getAudioFileCount());

                downloadedFileCount++;
                if (result == null) {
                    updateAudioBook(Integer.parseInt(file_name));

                    if (totalAudioFileCount == downloadedFileCount) {
                        mProgressDialog.dismiss();
                        downloadAudioFile();
                    }
                }


            }

        }
    }
    */
        private void showMessage(String result){
            stopDownload();
            mProgressDialog.dismiss();

            if (result.equalsIgnoreCase("UNAUTHORISED")){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.USER_UNAUTHORISED)).setPositiveButton(
                        "OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();

            }else if(result.equalsIgnoreCase("NOTFOUND")){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.FILE_NOTFOUND)).setPositiveButton(
                        "OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }



}
