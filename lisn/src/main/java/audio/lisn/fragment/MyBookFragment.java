package audio.lisn.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import audio.lisn.R;
import audio.lisn.activity.AudioBookDetailActivity;
import audio.lisn.activity.PlayerControllerActivity;
import audio.lisn.adapter.MyBookViewAdapter;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.model.DownloadedAudioBook;
import audio.lisn.util.AudioPlayerService;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.Constants;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link audio.lisn.fragment.MyBookFragment.OnStoreBookSelectedListener} interface
 * to handle interaction events.
 * Use the {@link audio.lisn.fragment.MyBookFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyBookFragment extends Fragment implements MyBookViewAdapter.MyBookSelectListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


    ConnectionDetector connectionDetector;
    private ProgressDialog pDialog;
    private List<AudioBook> bookList = new ArrayList<AudioBook>();
    private MyBookViewAdapter myBookViewAdapter;
    private RecyclerView myBookView;
    private static final String TAG = MyBookFragment.class.getSimpleName();
    private AudioBook selectedBook;
    //AudioBook.BookCategory bookCategory;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StoreFragment.
     */
    public static MyBookFragment newInstance() {
        MyBookFragment fragment = new MyBookFragment();
        return fragment;
    }

    public MyBookFragment() {
        // Required empty public constructor
    }


    private void sendMessage() {
            Intent intent = new Intent("preview_audio-event");
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(
                R.layout.fragment_my_book, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // BEGIN_INCLUDE (setup_viewpager)
        myBookView=(RecyclerView)view.findViewById(R.id.myBookContainer);
        myBookView.setLayoutManager(new GridLayoutManager(view.getContext(), 3));

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            connectionDetector = new ConnectionDetector(activity.getApplicationContext());

        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

    }
    @Override
    public void onResume() {
        super.onResume();
        loadData();
        updateMenu();
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.v("onPause","onPause");

    }

    private void hidePDialog() {
        if (pDialog != null) {
            pDialog.dismiss();
            pDialog = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Add your menu entries here
        super.onCreateOptionsMenu(menu, inflater);
    }


    private void loadData() {
        bookList.clear();
        DownloadedAudioBook downloadedAudioBook=new DownloadedAudioBook(getActivity().getApplicationContext());
       // downloadedAudioBook.readFileFromDisk(getActivity().getApplicationContext());
        HashMap< String, AudioBook> hashMap=downloadedAudioBook.getBookList(getActivity().getApplicationContext());
        for (AudioBook item : hashMap.values()) {
            bookList.add(item);
        }

        myBookViewAdapter = new MyBookViewAdapter(getActivity().getApplicationContext(),bookList);
        myBookViewAdapter.setMyBookSelectListener(this);
        myBookView.setAdapter(myBookViewAdapter);
    }
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

        audioBook.removeDownloadedFile(getContext());
        DownloadedAudioBook downloadedAudioBook = new DownloadedAudioBook(
                getActivity().getApplicationContext());
        downloadedAudioBook.readFileFromDisk(getActivity().getApplicationContext());
        downloadedAudioBook.addBookToList(getActivity().getApplicationContext(),
                audioBook.getBook_id(), audioBook);

        String message=getString(R.string.DELETE_BOOK_SUCCESS)+selectedBook.getEnglish_title()+"' from your device";
        Toast toast = Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.show();
    }
    private void deleteBook(){

        if(selectedBook.isPurchase()) {
            String message="Are you sure you want to delete '"+selectedBook.getEnglish_title()+"' from your device?";

            AlertDialog confirmationDialog = new AlertDialog.Builder(getActivity())
                    //set message, title, and icon
                    .setTitle(R.string.DELETE_CONFIRMATION_TITLE)
                    .setMessage(message)
                    .setPositiveButton(R.string.BUTTON_YES, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            deleteAudioBook(selectedBook);
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
    public void onMyBookSelect(View view, AudioBook audioBook, AudioBook.SelectedAction btnIndex) {
        switch (btnIndex){

            case ACTION_DETAIL:
                AudioBookDetailActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), view.findViewById(R.id.book_cover_thumbnail), audioBook);
                break;

            case ACTION_PLAY:
                PlayerControllerActivity.navigate((android.support.v7.app.AppCompatActivity) getActivity(), view.findViewById(R.id.book_cover_thumbnail), audioBook);
                break;
            case ACTION_DELETE:
                this.selectedBook=audioBook;
                deleteBook();
                break;

            default:
                break;

        }
    }

    public interface OnStoreBookSelectedListener {
        public void onStoreBookSelected(int position);

    }
    private void updateMenu() {
        Intent intent = new Intent(Constants.MENU_ITEM_SELECT);
        intent.putExtra("index", 2);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).sendBroadcast(intent);
    }

}