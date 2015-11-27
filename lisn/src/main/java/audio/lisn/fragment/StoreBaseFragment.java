package audio.lisn.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

import audio.lisn.R;
import audio.lisn.model.AudioBook;
import audio.lisn.view.SlidingTabLayout;

import static audio.lisn.R.id.sliding_tabs;
import static audio.lisn.R.id.viewpager;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link StoreBaseFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StoreBaseFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


    private SlidingTabLayout mSlidingTabLayout;
    private ViewPager mViewPager;
    private static final String TAG = StoreBaseFragment.class.getSimpleName();
    private StoreFragment currentFragment;
    //private Map mPageReferenceMap;
    Map<Integer, StoreFragment> mPageReferenceMap = new HashMap<>();


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StoreBaseFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StoreBaseFragment newInstance() {
        StoreBaseFragment fragment = new StoreBaseFragment();
        return fragment;
    }

    public StoreBaseFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_store_base, container, false);
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // BEGIN_INCLUDE (setup_viewpager)
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        mViewPager = (ViewPager) view.findViewById(viewpager);
       // mViewPager.setAdapter(new SamplePagerAdapter());
        mViewPager.setAdapter(new SectionsPagerAdapter(getChildFragmentManager()));

        // END_INCLUDE (setup_viewpager)

        // BEGIN_INCLUDE (setup_slidingtablayout)
        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {

            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.whiteColor);    //define any color in xml resources and set it here, I have used white
            }

            @Override
            public int getDividerColor(int position) {
                return getResources().getColor(R.color.whiteColor);
            }
        });
        // END_INCLUDE (setup_slidingtablayout)
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.store_book_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
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



    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        private final String[] CATEGORIES = { "Novels","Educational","Short Stories","Other"};

        @Override
        public Fragment getItem(int position) {

            if(currentFragment !=null){
                currentFragment.removePlayer();
                currentFragment=null;

            }
            AudioBook.BookCategory bookCategory= AudioBook.BookCategory.CATEGORY_1;

            if(position==0){
                bookCategory= AudioBook.BookCategory.CATEGORY_1;
            }
            else if(position==1){
                bookCategory= AudioBook.BookCategory.CATEGORY_2;
            }
            else if(position==2){
                bookCategory= AudioBook.BookCategory.CATEGORY_3;
            }

            else if(position==3){
                bookCategory= AudioBook.BookCategory.CATEGORY_OTHER;
            }


            currentFragment=new  StoreFragment(bookCategory);
            mPageReferenceMap.put(position, currentFragment);

            return currentFragment;

        }

        @Override
        public int getCount() {
            // Show  total pages.
            return CATEGORIES.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return CATEGORIES[position];
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            StoreFragment storeFragment = (StoreFragment) object;
            if(storeFragment != null){
                storeFragment.removePlayer();
            }
            mPageReferenceMap.remove(position);


        }
        public StoreFragment getFragment(int key) {
            return (StoreFragment) mPageReferenceMap.get(key);
        }

    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v("onActivityResult","onActivityResult StoreBaseFragment resultCode"+requestCode);

        int index = mViewPager.getCurrentItem();
        SectionsPagerAdapter adapter = ((SectionsPagerAdapter)mViewPager.getAdapter());
        StoreFragment fragment = adapter.getFragment(index);

        if(fragment !=null)
            fragment.onActivityResult(requestCode, resultCode, data);

        super.onActivityResult(requestCode, resultCode, data);
    }

}
