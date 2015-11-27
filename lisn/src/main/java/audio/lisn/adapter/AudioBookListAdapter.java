package audio.lisn.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.util.List;

import audio.lisn.R;
import audio.lisn.app.AppController;
import audio.lisn.model.AudioBook;
import audio.lisn.model.AudioBook.LanguageCode;
import audio.lisn.util.CustomTypeFace;

public class AudioBookListAdapter extends BaseAdapter  {

	private Context context;
    private LayoutInflater inflater;
    private List<AudioBook>audioBooks;
    private ItemSelectListener listener;
    ImageLoader imageLoader = AppController.getInstance().getImageLoader();

 
    public AudioBookListAdapter(Context context, List<AudioBook> audioBooks) {
        this.context = context;
        this.audioBooks = audioBooks;
    }
 
    @Override
    public int getCount() {
        return audioBooks.size();
    }
 
    @Override
    public Object getItem(int location) {
        return audioBooks.get(location);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
 
    	final int bookIndex = position;
    	final ViewHolder holder;
    	TextView title,author,price,downloadedCount;
    	NetworkImageView thumbNail;
        RatingBar ratingBar;
        ImageButton btnAction;
        AudioBook book = audioBooks.get(position);
       
    	if (imageLoader == null)
			imageLoader = AppController.getInstance().getImageLoader();
    	
        if (convertView == null) {
        	 if (inflater == null)
                 inflater = (LayoutInflater) context
                         .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            
        	 convertView = inflater.inflate(R.layout.audio_book_list_row, null);
             thumbNail=(NetworkImageView) convertView
             .findViewById(R.id.book_cover_thumbnail);
             title= (TextView) convertView.findViewById(R.id.book_title);
             author= (TextView) convertView.findViewById(R.id.book_author);
             price= (TextView) convertView.findViewById(R.id.book_price);
            ratingBar=(RatingBar)convertView.findViewById(R.id.rating_bar);
            btnAction=(ImageButton)convertView.findViewById(R.id.btnAction);
            downloadedCount= (TextView) convertView.findViewById(R.id.downloaded_count);

            holder = new ViewHolder(thumbNail, title, author,price,btnAction,ratingBar,downloadedCount);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            thumbNail=holder.thumbNail;
            title=holder.title;
            author=holder.authour;
            price=holder.price;
            btnAction=holder.btnAction;
            ratingBar=holder.ratingBar;
            downloadedCount=holder.downloadedCount;
        }
        if(book.getLanguageCode()== LanguageCode.LAN_SI){
        	Log.v("language_code abc : ", "si");
        	title.setTypeface(CustomTypeFace.getSinhalaTypeFace(context));
        	author.setTypeface(CustomTypeFace.getSinhalaTypeFace(context));
        	//price.setTypeface(CustomTypeFace.getEnglishTypeFace(context));
     	}else{
     		title.setTypeface(CustomTypeFace.getEnglishTypeFace(context));
     		author.setTypeface(CustomTypeFace.getEnglishTypeFace(context));
     		//price.setTypeface(CustomTypeFace.getEnglishTypeFace(context));
     	}
        
 
        title.setText(book.getTitle());
        author.setText(book.getAuthor());
        downloadedCount.setText(book.getDownloadCount()+" downloads");
        String priceText="Free";
		if( Float.parseFloat(book.getPrice())>0 ){
			priceText="Rs: "+book.getPrice();
		}
        price.setText(priceText);
        if(Float.parseFloat(book.getRate())>-1){
            ratingBar.setRating(Float.parseFloat(book.getRate()));
        }
        if(book.isPurchase()){
            ratingBar.setIsIndicator(false);
        }else{
            ratingBar.setIsIndicator(true);
        }
        
        // thumbnail image
        thumbNail.setImageUrl(book.getCover_image(), imageLoader);

        if(book.isPurchase()){
           // btnAction.setImageResource(R.drawable.btn_lisn_book);
        }else{
           // btnAction.setImageResource(R.drawable.btn_buy_book);


        }


        btnAction.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
            	AudioBook audioBook = audioBooks.get(bookIndex);
            	if(listener != null){
            		listener.onSelect(audioBook);
            	}

            }
        });
        convertView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AudioBook audioBook = audioBooks.get(bookIndex);
                if(listener != null){
                    listener.onSelect(audioBook);
                }

            }
        });
        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View arg0) {
               Log.v("convertView","setOnLongClickListener");
                AudioBook audioBook = audioBooks.get(bookIndex);

                if(listener != null){
                    listener.OnLongClickListener(audioBook);
                }
                return true;    // <- set to true
            }
        });

        return convertView;
    }

    public ItemSelectListener getListener() {
		return listener;
	}

	public void setListener(ItemSelectListener listener) {
		this.listener = listener;
	}
	private static class ViewHolder {
        public  NetworkImageView thumbNail;
        public  TextView title,authour,price,downloadedCount;
        ImageButton btnAction;
        RatingBar ratingBar;

        public ViewHolder(NetworkImageView thumbNail, TextView title,TextView author,
        		TextView price,ImageButton btnAction,RatingBar ratingBar,TextView downloadedCount) {
            this.thumbNail = thumbNail;
            this.title = title;
            this.authour = author;
            this.price = price;
            this.btnAction=btnAction;
            this.ratingBar=ratingBar;
            this.downloadedCount=downloadedCount;
        }

    }
    /* define its own listners */
	public interface ItemSelectListener
    {
		public void onSelect(AudioBook audioBook);
        public void OnLongClickListener(final AudioBook audioBook);
	}
}
