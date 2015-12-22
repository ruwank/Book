package audio.lisn.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import audio.lisn.R;
import audio.lisn.activity.AudioBookDetailActivity;
import audio.lisn.activity.PlayerControllerActivity;
import audio.lisn.activity.PurchaseActivity;
import audio.lisn.adapter.MyBookViewAdapter;
import audio.lisn.adapter.StoreBookViewAdapter;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.model.DownloadedAudioBook;
import audio.lisn.util.AppUtils;
import audio.lisn.util.AudioPlayerService;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;
import audio.lisn.webservice.FileDownloadTask;
import audio.lisn.webservice.FileDownloadTaskListener;
import audio.lisn.webservice.JsonUTF8ArrayRequest;
import audio.lisn.webservice.JsonUTF8StringRequest;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HomeFragment.OnHomeItemSelectedListener} interface
 * to handle interaction events.
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment implements StoreBookViewAdapter.StoreBookSelectListener, MyBookViewAdapter.MyBookSelectListener,FileDownloadTaskListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


    private OnHomeItemSelectedListener mListener;
    ConnectionDetector connectionDetector;
    private ProgressDialog pDialog;
    private List<AudioBook> newReleaseBookList = new ArrayList<AudioBook>();
    private List<AudioBook> topRatedBookList = new ArrayList<AudioBook>();
    private List<AudioBook> topDownloadBookList = new ArrayList<AudioBook>();
    private List<AudioBook> myBookList = new ArrayList<AudioBook>();
    private StoreBookViewAdapter newReleaseBookViewAdapter,topRatedBookViewAdapter,topDownloadBookViewAdapter;
    private RecyclerView myBookContainer,newReleaseBookContainer,topRatedBookContainer,topDownloadBookContainer;
    private static final String TAG = HomeFragment.class.getSimpleName();
    private AudioBook selectedBook;
    private MyBookViewAdapter myBookViewAdapter;
    private Button myBookMore;
    LinearLayout homeContainer;
    LinearLayout myBooksLinearLayout;
    int requestCount,respondCount;
    View selectedView;
    ProgressDialog mProgressDialog;
    List<FileDownloadTask> downloadingList = new ArrayList<FileDownloadTask>();
    int totalAudioFileCount, downloadedFileCount;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HomeFragment.
     */
    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }

    public HomeFragment() {
        // Required empty public constructor
        setHasOptionsMenu(true);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        View rootView = inflater.inflate(
//                R.layout.fragment_home, container, false);
//
//        gridViewStoreBook=(GridView)rootView.findViewById(R.id.gridViewStoreBook);
//        gridViewMyBook=(GridView)rootView.findViewById(R.id.gridViewMyBook);
//        return rootView;
//    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(
                R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // BEGIN_INCLUDE (setup_viewpager)
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        WindowManager wm = (WindowManager) getActivity().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        homeContainer=(LinearLayout)view.findViewById(R.id.homeContainer);
        myBookContainer=(RecyclerView)view.findViewById(R.id.myBookContainer);
        newReleaseBookContainer =(RecyclerView)view.findViewById(R.id.newReleaseBookContainer);
        topDownloadBookContainer =(RecyclerView)view.findViewById(R.id.topDownloadBookContainer);
        topRatedBookContainer =(RecyclerView)view.findViewById(R.id.topRatedBookContainer);
        myBooksLinearLayout=(LinearLayout)view.findViewById(R.id.my_book_layout);


        // get layout parameters for that view




        ViewGroup.LayoutParams myBookContainerLayoutParams = myBookContainer.getLayoutParams();
        int  myBookContainerLayoutParamsHeight=(int)(((size.x-60)/3)*1.5) +(int)(metrics.density * 48f);

        myBookContainerLayoutParams.height= myBookContainerLayoutParamsHeight;
        myBookContainer.setLayoutParams(myBookContainerLayoutParams);

        myBookContainer.setLayoutManager(new GridLayoutManager(view.getContext(), 3) {
//           // int itemIndex=0;
//            @Override
//            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
//                super.onLayoutChildren(recycler, state);
////                itemIndex++;
////
////                if(itemIndex==state.getItemCount()){
////                   // updateMyBookContainer();
////                }
//
//
//            }


            @Override
            public boolean canScrollVertically() {
                return false;
            }

            @Override
            public boolean canScrollHorizontally() {
                return false;
            }
        });

        myBookMore=(Button)view.findViewById(R.id.more_myBook);
        myBookMore.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if(mListener != null){
                    mListener.onOptionButtonClicked(2);
                }

            }
        });

        Button storeMore=(Button)view.findViewById(R.id.more_store);
        storeMore.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if(mListener != null){
                    mListener.onOptionButtonClicked(1);
                }

            }
        });
        ViewGroup.LayoutParams params = newReleaseBookContainer.getLayoutParams();
        int height=(int)(((size.x-60)/3)*1.5) +(int)(metrics.density * 52f);
        Log.v("height", "height:" + height);
        params.height= height;

        newReleaseBookContainer.setLayoutParams(new LinearLayout.LayoutParams(params));

        newReleaseBookContainer.setLayoutManager(new GridLayoutManager(view.getContext(), 3) {
//            int itemIndex=0;
//            @Override
//            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
//                super.onLayoutChildren(recycler, state);
//                itemIndex++;
//
//                if(itemIndex==state.getItemCount()){
//                    updateStoreBookContainer();
//                }
//
//            }


            @Override
            public boolean canScrollVertically() {
                return false;
            }

            @Override
            public boolean canScrollHorizontally() {
                return false;
            }
        });
        topRatedBookContainer.setLayoutParams(new LinearLayout.LayoutParams(params));
        topRatedBookContainer.setLayoutManager(new GridLayoutManager(view.getContext(), 3) {

            @Override
            public boolean canScrollVertically() {
                return false;
            }

            @Override
            public boolean canScrollHorizontally() {
                return false;
            }
        });
        topDownloadBookContainer.setLayoutParams(new LinearLayout.LayoutParams(params));
        topDownloadBookContainer.setLayoutManager(new GridLayoutManager(view.getContext(), 3) {

            @Override
            public boolean canScrollVertically() {
                return false;
            }

            @Override
            public boolean canScrollHorizontally() {
                return false;
            }
        });


    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            connectionDetector = new ConnectionDetector(context);
            mListener = (OnHomeItemSelectedListener) context;
            mProgressDialog = new ProgressDialog(context);
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
            pDialog = new ProgressDialog(context);
            pDialog.setMessage(getString(R.string.loading_text));

        } catch (Exception e) {
        }
    }



    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    @Override
    public void onResume() {
        super.onResume();


        loadData();
        loadMyBookData();

    }
    @Override
    public void onPause() {
        super.onPause();
        removePlayer();

        Log.v(TAG, "onPause");

    }
    private void hidePDialog() {
        if (pDialog != null) {
            pDialog.dismiss();
        }
    }
    public void removePlayer(){
        if(newReleaseBookViewAdapter !=null)
        {
            newReleaseBookViewAdapter.releaseMediaPlayer();
        }
        if(topRatedBookViewAdapter !=null)
        {
            topRatedBookViewAdapter.releaseMediaPlayer();
        }
        if(topDownloadBookViewAdapter !=null)
        {
            topDownloadBookViewAdapter.releaseMediaPlayer();
        }
    }
    private void downloadNewReleaseData() {
        String url=getString(R.string.home_book_list_url);

        JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(url, null,

                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {

                        AppController.getInstance().setNewReleaseBookList(jsonArray);
                        setNewReleaseData(jsonArray);
                        respondCount++;
                        showHiddenProgressBar(false);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                respondCount++;
                showHiddenProgressBar(false);
                VolleyLog.d(TAG, "Error: " + error.getMessage());

            }
        });
        bookListReq.setShouldCache(true);
        AppController.getInstance().addToRequestQueue(bookListReq, "tag_book_list");
    }

    private void setNewReleaseData(JSONArray jsonArray){
        newReleaseBookList.clear();
        // Parsing json
       for (int i = 0; (i < jsonArray.length() && i< 3) ; i++) {
            try {

                JSONObject obj = jsonArray.getJSONObject(i);
                AudioBook book=new AudioBook(obj,i,getContext());
                newReleaseBookList.add(book);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        newReleaseBookViewAdapter.notifyDataSetChanged();

    }

    private void downloadTopRatedData() {
        String url=getString(R.string.top_rated_book_list_url);

        JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(url, null,

                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {

                        AppController.getInstance().setTopRatedBookList(jsonArray);
                        setTopRatedData(jsonArray);
                        respondCount++;
                        showHiddenProgressBar(false);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                respondCount++;
                showHiddenProgressBar(false);

            }
        });
        bookListReq.setShouldCache(true);
        AppController.getInstance().addToRequestQueue(bookListReq, "tag_top_rated_book_list");
    }

    private void setTopRatedData(JSONArray jsonArray){
        topRatedBookList.clear();
        // Parsing json
        for (int i = 0; (i < jsonArray.length() && i< 3) ; i++) {
            try {

                JSONObject obj = jsonArray.getJSONObject(i);
                AudioBook book=new AudioBook(obj,i,getContext());
                topRatedBookList.add(book);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        topRatedBookViewAdapter.notifyDataSetChanged();

    }

    private void downloadTopDownloadData() {
        String url=getString(R.string.top_download_book_list_url);

        JsonUTF8ArrayRequest bookListReq = new JsonUTF8ArrayRequest(url, null,

                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {

                        AppController.getInstance().setTopDownloadedBookList(jsonArray);
                        setTopDownloadedData(jsonArray);
                        respondCount++;
                        showHiddenProgressBar(false);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                respondCount++;
                showHiddenProgressBar(false);

            }
        });
        bookListReq.setShouldCache(true);
        AppController.getInstance().addToRequestQueue(bookListReq, "tag_top_download_book_list");
    }

    private void setTopDownloadedData(JSONArray jsonArray){
        topDownloadBookList.clear();
        for (int i = 0; (i < jsonArray.length() && i< 3) ; i++) {
            try {

                JSONObject obj = jsonArray.getJSONObject(i);
                AudioBook book=new AudioBook(obj,i,getContext());
                topDownloadBookList.add(book);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        topDownloadBookViewAdapter.notifyDataSetChanged();

    }
    private void loadData() {

        newReleaseBookViewAdapter = new StoreBookViewAdapter(getActivity().getApplicationContext(),newReleaseBookList);
        newReleaseBookViewAdapter.setStoreBookSelectListener(this);
        newReleaseBookContainer.setAdapter(newReleaseBookViewAdapter);

        topDownloadBookViewAdapter = new StoreBookViewAdapter(getActivity().getApplicationContext(),topDownloadBookList);
        topDownloadBookViewAdapter.setStoreBookSelectListener(this);
        topDownloadBookContainer.setAdapter(topDownloadBookViewAdapter);

        topRatedBookViewAdapter = new StoreBookViewAdapter(getActivity().getApplicationContext(),topRatedBookList);
        topRatedBookViewAdapter.setStoreBookSelectListener(this);
        topRatedBookContainer.setAdapter(topRatedBookViewAdapter);

        requestCount=0;
        respondCount=0;
        JSONArray newReleaseJsonArray = AppController.getInstance().getNewReleaseBookList();
        if (newReleaseJsonArray != null) {
            setNewReleaseData(newReleaseJsonArray);
        } else {
        if (connectionDetector.isConnectingToInternet()) {
            downloadNewReleaseData();
            requestCount++;
            }

        }

        JSONArray topDownloadedJsonArray = AppController.getInstance().getTopDownloadedBookList();
        if (topDownloadedJsonArray != null) {
            setTopDownloadedData(topDownloadedJsonArray);
        } else {
            if (connectionDetector.isConnectingToInternet()) {
                downloadTopDownloadData();
                requestCount++;
            }

        }


        JSONArray topRatedJsonArray = AppController.getInstance().getTopRatedBookList();
        if (topRatedJsonArray != null) {
            setTopRatedData(topRatedJsonArray);
        } else {
            if (connectionDetector.isConnectingToInternet()) {
                downloadTopRatedData();
                requestCount++;
            }

        }
        if(requestCount>0){
            showHiddenProgressBar(true);

        }



    }
    private void showHiddenProgressBar(boolean show){

        if(show){

            pDialog.show();
        }else{
            if(requestCount ==respondCount)
                hidePDialog();
        }

    }
    private void loadMyBookData(){
        if(AppController.getInstance().isUserLogin()) {

            myBookList.clear();
            DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(getActivity().getApplicationContext());
            downloadedAudioBook.readFileFromDisk(getActivity().getApplicationContext());
            HashMap<String, AudioBook> hashMap = downloadedAudioBook.getBookList();
            int count = 0;
            for (AudioBook item : hashMap.values()) {
                count++;
                myBookList.add(item);
                if (count == 3) {
                    break;
                }
            }
            if (count < 1) {
                myBooksLinearLayout.setVisibility(View.GONE);
            } else {
                myBooksLinearLayout.setVisibility(View.VISIBLE);

            }
            if (hashMap.size() > 3) {
                myBookMore.setVisibility(View.VISIBLE);

            } else {
                myBookMore.setVisibility(View.GONE);
            }
            myBookViewAdapter = new MyBookViewAdapter(getActivity().getApplicationContext(), myBookList);
            myBookViewAdapter.setMyBookSelectListener(this);
            myBookContainer.setAdapter(myBookViewAdapter);
        }else{
            myBooksLinearLayout.setVisibility(View.GONE);

        }

    }
    private void updateMyBookContainer(){
        Log.v("myBookContainer","myBookContainer"+myBookContainer.getChildCount());
        if(myBookContainer.getChildCount()>0){
            View view=myBookContainer.getChildAt(0);
            Log.v("height","myBookContainer height"+view.getHeight());
            ViewGroup.LayoutParams myBookContainerLayoutParams = myBookContainer.getLayoutParams();
             myBookContainerLayoutParams.height=view.getHeight();
            //myBookContainer.setLayoutParams(myBookContainerLayoutParams);
          //  homeContainer.updateViewLayout(myBookContainer, myBookContainerLayoutParams);

        }

    }
    private void updateStoreBookContainer(){
        Log.v("storeBookContainer","storeBookContainer"+myBookContainer.getChildCount());
        if(newReleaseBookContainer.getChildCount()>0){
            View view=newReleaseBookContainer.getChildAt(0);
            Log.v("height","height"+view.getHeight());
            ViewGroup.LayoutParams storeBookContainerLayoutParams = newReleaseBookContainer.getLayoutParams();
            storeBookContainerLayoutParams.height=view.getHeight();
            newReleaseBookContainer.setLayoutParams(storeBookContainerLayoutParams);
            homeContainer.updateViewLayout(newReleaseBookContainer, storeBookContainerLayoutParams);

        }

    }

//store book select
//private void showOption() {
//
//
//    PopupMenu popupMenu = new PopupMenu(getActivity(), gridViewStoreBook);
//    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//        public boolean onMenuItemClick(MenuItem item) {
//            Toast.makeText(
//                    getActivity(),
//                    "You Clicked : " + item.getTitle(),
//                    Toast.LENGTH_SHORT
//            ).show();
//            return true;
//        }
//    });    popupMenu.inflate(R.menu.global);
//    popupMenu.show();
//}

//private void showDetailView(){
//        Intent intent = new Intent(getActivity().getApplicationContext(),
//                AudioBookDetailActivity.class);
//        intent.putExtra("audioBook", selectedBook);
//        startActivity(intent);
//    }
    private void stopPlayer(AudioBook audioBook) {
        if (AudioPlayerService.mediaPlayer != null) {
            String playingBookId= AppController.getInstance().getPlayingBookId();
            if(playingBookId.equalsIgnoreCase(audioBook.getBook_id()) ){
                AudioPlayerService.mediaPlayer.stop();
                AudioPlayerService.mediaPlayer.release();
                AudioPlayerService.mediaPlayer=null;
            }
        }
    }
    private void deleteAudioBook(AudioBook audioBook){
        stopPlayer(audioBook);

        audioBook.removeDownloadedFile();
        DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(
                getActivity().getApplicationContext());
        downloadedAudioBook.readFileFromDisk(getActivity().getApplicationContext());
        downloadedAudioBook.addBookToList(getActivity().getApplicationContext(),
                audioBook.getBook_id(), audioBook);

    }
    private void deleteBook(final AudioBook audioBook){
        String message="Are you sure you want to delete '"+audioBook.getEnglish_title()+"' from your device?";
        if(audioBook.isPurchase()) {
            AlertDialog confirmationDialog = new AlertDialog.Builder(getActivity())
                    //set message, title, and icon

                    .setTitle(getString(R.string.DELETE_CONFIRMATION_TITLE))
                    .setMessage(message)
                    .setPositiveButton(R.string.BUTTON_YES, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            //your deleting code
                            deleteAudioBook(audioBook);
                            dialog.dismiss();
                        }

                    })

                    .setNegativeButton(R.string.BUTTON_NO, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();

                        }
                    })
                    .create();

            confirmationDialog.show();
        }

    }



    @Override
    public void onStoreBookSelect(View view, AudioBook audioBook, AudioBook.SelectedAction btnIndex) {
        switch (btnIndex){
            case ACTION_PURCHASE:{
                /*
                this.selectedBook=audioBook;
                this.selectedView=view;
                if(AppController.getInstance().isUserLogin()){

                    if (Float.parseFloat(selectedBook.getPrice()) > 0) {
                        Intent intent = new Intent(getActivity(),
                                PurchaseActivity.class);
                        intent.putExtra("audioBook", selectedBook);
                        startActivityForResult(intent, 24);

                    }else{
                        logUserDownload();
                    }
                }else{
                    Intent intent = new Intent(getActivity(),
                            LoginActivity.class);
                    startActivityForResult(intent, 23);
                }
                */
                AudioBookDetailActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), view.findViewById(R.id.book_cover_thumbnail), audioBook);

            }


                // AudioBookDetailActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), view.findViewById(R.id.book_cover_thumbnail), audioBook);
                break;
            case ACTION_DETAIL:
                AudioBookDetailActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), view.findViewById(R.id.book_cover_thumbnail), audioBook);
                break;

            case ACTION_PLAY:
                PlayerControllerActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), view.findViewById(R.id.book_cover_thumbnail), audioBook);
                break;

            default:
                break;


        }
    }

    @Override
    public void onMyBookSelect(View view, AudioBook audioBook, AudioBook.SelectedAction btnIndex) {
        switch (btnIndex){

            case ACTION_DETAIL:
                AudioBookDetailActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), view.findViewById(R.id.book_cover_thumbnail), audioBook);
                break;

            case ACTION_PLAY:
                PlayerControllerActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), view.findViewById(R.id.book_cover_thumbnail), audioBook);
                break;
            case ACTION_DELETE:
                deleteBook(audioBook);
                break;

            default:
                break;


        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v("onActivityResult", "onActivityResult HomeFragment");

        if (requestCode == 23) {
            if(resultCode ==  Constants.RESULT_SUCCESS){
                if (Float.parseFloat(selectedBook.getPrice()) > 0) {
                    Intent intent = new Intent(getActivity(),
                            PurchaseActivity.class);
                    intent.putExtra("audioBook", selectedBook);
                    startActivityForResult(intent, 24);

                }else{
                    logUserDownload();
                }

            }
            if (resultCode ==  Constants.RESULT_ERROR) {
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                        R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                android.support.v7.app.AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
        else if (requestCode == 24) {
            if(resultCode == Constants.RESULT_SUCCESS){
                if(selectedBook !=null) {
                    selectedBook.setPurchase(true);
                    updateAudioBook(0);
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
                }
            }
            if (resultCode == Constants.RESULT_ERROR) {
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                        R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                android.support.v7.app.AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }
    private void downloadAudioFile() {
        String dirPath = AppUtils.getDataDirectory(getContext())
                + selectedBook.getBook_id()+ File.separator;
        File fileDir = new File(dirPath);
        if (!fileDir.exists()) {
            fileDir.mkdirs();

        }

        if (connectionDetector.isConnectingToInternet()) {
            //  updateAudioBook(null,null);


            mProgressDialog.show();

            downloadedFileCount=0;
            // String [] file_urls=audioBook.getAudio_file_urls();
            //totalAudioFileCount=file_urls.length;
            totalAudioFileCount=0;
            downloadingList.clear();

            //  HashMap fileList= audioBook.getDownloadedFileList();

            for (int filePart=1; filePart<=(selectedBook.getAudioFileCount()); filePart++){
                File file = new File(dirPath +filePart+".lisn");

                if (!file.exists() ||  !(selectedBook.getDownloadedChapter().contains(filePart)) ) {
                    downloadAudioFileFromUrl(filePart);
                    totalAudioFileCount++;
                }


            }
            if(downloadedFileCount == totalAudioFileCount){
                mProgressDialog.dismiss();
                starAudioPlayer();


            }else{
                if(AppUtils.getAvailableMemory() < selectedBook.getFileSize()){
                    stopDownload();

                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            getActivity());
                    builder.setTitle(R.string.NO_ENOUGH_SPACE_TITLE)
                    .setMessage(R.string.NO_ENOUGH_SPACE_MESSAGE).setPositiveButton(
                            R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // FIRE ZE MISSILES!
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();

                }else {
                    mProgressDialog.setMessage("Downloading " + (selectedBook.getDownloadedChapter().size() + 1) + " of " + selectedBook.getAudioFileCount());
                }

            }

        } else {

            downloadedFileCount=0;
            totalAudioFileCount=0;

            for (int filePart=1; filePart<=(selectedBook.getAudioFileCount()); filePart++){
                File file = new File(dirPath +filePart+".lisn");
                if (!file.exists()) {
                    totalAudioFileCount++;
                }


            }
            if(downloadedFileCount ==totalAudioFileCount){
                mProgressDialog.dismiss();
                starAudioPlayer();
            }else{
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(
                        getActivity());
                builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(R.string.NO_INTERNET_MESSAGE).setPositiveButton(
                        R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // FIRE ZE MISSILES!
                            }
                        });
                android.support.v7.app.AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }
    private void downloadAudioFileFromUrl(int filePart){

        if (connectionDetector.isConnectingToInternet()) {
            String dirPath = AppUtils.getDataDirectory(getContext())
                    + selectedBook.getBook_id()+File.separator;
            File file = new File(dirPath + filePart + ".lisn");

            if (file.exists()) {
                file.delete();
            }
            FileDownloadTask downloadTask =  new FileDownloadTask(getContext(),this,selectedBook.getBook_id());
            downloadTask.execute(dirPath, "" + filePart);
            downloadingList.add(downloadTask);

        }else{
            android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(
                    getActivity());
            builder.setTitle(R.string.NO_INTERNET_TITLE).setMessage(R.string.NO_INTERNET_MESSAGE).setPositiveButton(
                    R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            android.support.v7.app.AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
    private void starAudioPlayer() {

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.DOWNLOAD_COMPLETE_TITLE).setMessage(getString(R.string.DOWNLOAD_COMPLETE_MESSAGE)).setPositiveButton(
                R.string.BUTTON_YES, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PlayerControllerActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), selectedView.findViewById(R.id.book_cover_thumbnail), selectedBook);

                        //  PlayerControllerActivity.navigate(AudioBookDetailActivity.this,bookCoverImage, audioBook);

                    }
                })
                .setNegativeButton(R.string.BUTTON_NO, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                    }
                });
        android.support.v7.app.AlertDialog dialog = builder.create();
        dialog.show();


    }
    private void updateAudioBook(int chapter){
        if(chapter>0) {
            selectedBook.addChapterToDownloadedChapter(chapter);
        }
        DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(
                getContext());
        downloadedAudioBook.readFileFromDisk(getContext());
        downloadedAudioBook.addBookToList(getContext(),
                selectedBook.getBook_id(), selectedBook);

    }

    private void logUserDownload(){
        saveCoverImage();
        showHiddenProgressBar(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("userid", AppController.getInstance().getUserId());
        params.put("bookid", selectedBook.getBook_id());
        params.put("actid", ""+1);
        params.put("content", "" + 1);

        String url = getResources().getString(R.string.user_action_url);

        JsonUTF8StringRequest stringRequest = new JsonUTF8StringRequest(Request.Method.POST, url,params,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.v("response","response:"+response);
                        hidePDialog();
                        downloadAudioFile();
                        selectedBook.setPurchase(true);
                        updateAudioBook(0);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                hidePDialog();

            }
        });


        AppController.getInstance().addToRequestQueue(stringRequest, "tag_download_book");

    }
    private void saveCoverImage() {
        String dirPath = AppUtils.getDataDirectory(selectedView.getContext())
                + selectedBook.getBook_id() + File.separator;
        ImageView bookCoverImage=(ImageView)selectedView.findViewById(R.id.book_cover_thumbnail);
        bookCoverImage.buildDrawingCache();
        Bitmap bitmapImage = bookCoverImage.getDrawingCache();
        //Bitmap resized = Bitmap.createScaledBitmap(bitmapImage, (int) (160 * 2), (int) (240 * 2), true);

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
    }

    @Override
    public void onPostExecute(String result, String file_name) {
        if (result != null && result.equalsIgnoreCase("UNAUTHORISED")){
            showMessage("UNAUTHORISED");

        }else if(result != null && result.equalsIgnoreCase("NOTFOUND")){
            showMessage("NOTFOUND");

        }else {
            mProgressDialog.setMessage("Downloading " + (selectedBook.getDownloadedChapter().size() + 1) + " of " + selectedBook.getAudioFileCount());

            downloadedFileCount++;
            if (result == null) {
                updateAudioBook(Integer.parseInt(file_name));

                if (totalAudioFileCount == downloadedFileCount) {
                    downloadAudioFile();
                }
            }
        }
    }
    private void showMessage(String result){
        stopDownload();
        mProgressDialog.dismiss();

        if (result.toUpperCase().contains("UNAUTHORISED")){
            android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.USER_UNAUTHORISED_TITLE).setMessage(getString(R.string.USER_UNAUTHORISED_MESSAGE)).setPositiveButton(
                    R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });

            android.support.v7.app.AlertDialog dialog = builder.create();
            dialog.show();

        }else if(result.toUpperCase().contains("NOTFOUND")){
            android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.SERVER_ERROR_TITLE).setMessage(getString(R.string.SERVER_ERROR_MESSAGE)).setPositiveButton(
                    R.string.BUTTON_OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });

            android.support.v7.app.AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
    private void stopDownload(){
        for (int i = 0; i < downloadingList.size(); i++) {
            FileDownloadTask downloadTask = downloadingList.get(i);
            downloadTask.cancel(true);
        }
    }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnHomeItemSelectedListener {
        // TODO: Update argument type and name
        public void onHomeItemSelected(int position,boolean isDownloadedBook);
        public void onOptionButtonClicked(int buttonIndex);

    }

}
