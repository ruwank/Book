/*
 * Copyright (C) 2015 Antonio Leiva
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package audio.lisn.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import audio.lisn.R;
import audio.lisn.model.AudioBook;
import audio.lisn.util.AppUtils;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.CustomTypeFace;

public class MyBookViewAdapter extends RecyclerView.Adapter<MyBookViewAdapter.ViewHolder>{

    private List<AudioBook> items;
    private MyBookSelectListener listener;
    ConnectionDetector connectionDetector;
    int selectedBookIndex;
    private Context context;



    public MyBookViewAdapter(Context context, List<AudioBook> items) {
        this.items = items;
        this.context=context;
        connectionDetector = new ConnectionDetector(context);

    }

    public void setMyBookSelectListener(MyBookSelectListener onItemClickListener) {
        this.listener = onItemClickListener;
    }

    @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_my_book, parent, false);


        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                if (listener != null) {

                    new Handler().postDelayed(new Runnable() {
                        @Override public void run() {
                            listener.onMyBookSelect(v, (AudioBook) v.getTag(), AudioBook.SelectedAction.ACTION_DETAIL);
                        }
                    }, 200);
                }
            }
        });


        return new ViewHolder(view);
    }



    @Override public void onBindViewHolder(final ViewHolder holder, int position) {
        AudioBook book = items.get(position);
        selectedBookIndex=position;
        holder.bookId=book.getBook_id();


        if(book.getLanguageCode()== AudioBook.LanguageCode.LAN_SI){
            holder.title.setTypeface(CustomTypeFace.getSinhalaTypeFace(holder.title.getContext()));
            holder.author.setTypeface(CustomTypeFace.getSinhalaTypeFace(holder.author.getContext()));
        }else{
            holder.title.setTypeface(CustomTypeFace.getEnglishTypeFace(holder.title.getContext()));
            holder.author.setTypeface(CustomTypeFace.getEnglishTypeFace(holder.author.getContext()));
        }
        holder.title.setText(book.getTitle());
        holder.author.setText(book.getAuthor());


        holder.thumbNail.setImageBitmap(null);

        String img_path = AppUtils.getDataDirectory(context)
                + book.getBook_id()+File.separator+"book_cover.jpg";


        File imgFile = new  File(img_path);




        if(imgFile.exists()){
            Picasso.with(context)
                    .load(imgFile)
                    .into(holder.thumbNail);

        }
        else {
            Picasso.with(context)
                    .load(book.getCover_image())
                    .placeholder(R.drawable.audiobook_placeholder)
                    .into(new Target() {

                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            Log.v("onBitmapLoaded", "onBitmapLoaded" + bitmap.getHeight());
                            holder.thumbNail.setImageBitmap(bitmap);
                            SaveCoverImage(holder.bookId,bitmap);

                        }

                        @Override
                        public void onBitmapFailed(Drawable errorDrawable) {
                            holder.thumbNail.setImageResource(R.drawable.audiobook_placeholder);

                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {

                        }
                    });
//            Picasso.with(holder.thumbNail.getContext())
//                    .load(book.getCover_image())
//                    .placeholder(R.drawable.audiobook_placeholder)
//                    .into(holder.thumbNail);
        }

        holder.optionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                popupMenu.inflate(R.menu.my_book_menu);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {

                        switch (item.getItemId()) {
                            case R.id.action_detail:
                                if (listener != null) {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override public void run() {
                                            listener.onMyBookSelect(holder.itemView, (AudioBook) holder.itemView.getTag(), AudioBook.SelectedAction.ACTION_DETAIL);
                                        }
                                    }, 200);
                                }
                                break;
                            case R.id.action_play:
                                if (listener != null) {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override public void run() {
                                            listener.onMyBookSelect(holder.itemView, (AudioBook) holder.itemView.getTag(), AudioBook.SelectedAction.ACTION_PLAY);
                                        }
                                    }, 200);
                                }
                                break;
                            case R.id.action_delete:
                                if (listener != null) {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override public void run() {
                                            listener.onMyBookSelect(holder.itemView, (AudioBook) holder.itemView.getTag(), AudioBook.SelectedAction.ACTION_DELETE);
                                        }
                                    }, 200);
                                }
                                break;
                            default:
                                break;

                        }

                        return true;
                    }
                });
                popupMenu.show();



            }
        });


        holder.itemView.setTag(book);
    }

    @Override public int getItemCount() {
        return items.size();
    }



    protected static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView thumbNail;
        public TextView title, author;
        public ImageButton optionButton;
        public String bookId;



        public ViewHolder(View itemView) {
            super(itemView);

            thumbNail=(ImageView) itemView
                    .findViewById(R.id.book_cover_thumbnail);
            title= (TextView) itemView.findViewById(R.id.book_title);
            author= (TextView) itemView.findViewById(R.id.book_author);
            optionButton=(ImageButton)itemView.findViewById(R.id.btn_action);



        }
    }
    private void SaveCoverImage(String bookId,Bitmap bitmapImage) {
        String dirPath = AppUtils.getDataDirectory(context)
                + bookId + File.separator;

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
                bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public interface MyBookSelectListener
    {
        public void onMyBookSelect(View view, AudioBook audioBook, AudioBook.SelectedAction btnIndex);
    }


}
