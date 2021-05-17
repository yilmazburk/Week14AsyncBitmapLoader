package com.istinye.week13rss.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.istinye.week13rss.R;
import com.istinye.week13rss.model.News;
import com.istinye.week13rss.util.LRUBitmapCache;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ListView newsListView;
    private LinearLayout noDataView;

    private BaseAdapter listAdapter;
    private List<News> news;

    private LRUBitmapCache lruBitmapCache;

    public static final String DETAIL_INTENT_KEY = "DETAIL_INTENT_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lruBitmapCache = new LRUBitmapCache();

        setTitle(getString(R.string.app_name));

        initData();
        setViews();
        setAdapters();

        getNews();
    }

    private void initData() {
        news = new ArrayList<News>();
    }

    public InputStream getInputStream(URL url) throws IOException {
        return  url.openConnection().getInputStream();
    }

    private void getNews() {
        new NewsAsyncTask().execute();
    }

    private void setAdapters() {
        listAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return news.size();
            }

            @Override
            public Object getItem(int position) {
                return news.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                ViewHolder viewHolder;

                if (convertView == null) {
                    convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_list_item, parent, false);
                    viewHolder = new ViewHolder(convertView);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }

                if (position % 2 == 0) {
                    convertView.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                } else {
                    convertView.setBackgroundColor(getResources().getColor(android.R.color.white));
                }

                News myNew = news.get(position);
                viewHolder.title.setText(myNew.getTitle());
                viewHolder.description.setText(myNew.getDescription());

                if (myNew.getImageUrl() != null) {
                    try {
                        URL imageUrl = new URL(myNew.getImageUrl());
                        lruBitmapCache.loadBitmap(imageUrl, viewHolder.backgroundImageView);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }

                return convertView;
            }
        };

        newsListView.setAdapter(listAdapter);
    }

    private void setViews() {
        newsListView = findViewById(R.id.newsListView);
        noDataView = findViewById(R.id.no_data_view);
        newsListView.setEmptyView(noDataView);

        newsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                /*
                    Loads urls in Browser

                    Uri linkUri =  Uri.parse(news.get(position).getLink());
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, linkUri);
                    startActivity(browserIntent);
                */

                Intent detailIntent = new Intent(MainActivity.this, DetailActivity.class);
                detailIntent.putExtra(DETAIL_INTENT_KEY,  news.get(position).getLink());
                startActivity(detailIntent);

            }
        });
    }

    private class ViewHolder {
        private TextView title;
        private TextView description;
        private ImageView backgroundImageView;

        public ViewHolder(View convertView) {
            title = convertView.findViewById(R.id.titleTextView);
            description = convertView.findViewById(R.id.descriptionTextView);
            backgroundImageView = convertView.findViewById(R.id.backgroundImageView);
        }
    }


    public class NewsAsyncTask extends AsyncTask<Void, Void, Exception> {

        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        Exception exception = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog.setMessage("News fetching...");
            progressDialog.show();

        }

        @Override
        protected Exception doInBackground(Void... voids) {

            try {
                URL newsUrl = new URL("https://www.aa.com.tr/tr/rss/default?cat=guncel");

                XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
                xmlPullParserFactory.setNamespaceAware(true); // Default false
                XmlPullParser parser = xmlPullParserFactory.newPullParser();
                parser.setInput(getInputStream(newsUrl),"UTF_8");

                boolean inItemTag = false;

                int eventType = parser.getEventType();
                News newsObject = null;
                while (eventType != XmlPullParser.END_DOCUMENT) {

                    if (eventType == XmlPullParser.START_TAG) {
                        if (parser.getName().equalsIgnoreCase("item")) {
                            inItemTag = true;
                            newsObject = new News();
                        } else if (parser.getName().equalsIgnoreCase("guid")) {
                            if (inItemTag) {
                                newsObject.setGuid(parser.nextText());
                            }
                        } else if (parser.getName().equalsIgnoreCase("title")) {
                            if (inItemTag) {
                                newsObject.setTitle(parser.nextText());
                            }
                        } else if (parser.getName().equalsIgnoreCase("link")) {
                            if (inItemTag) {
                                newsObject.setLink(parser.nextText());
                            }
                        } else if (parser.getName().equalsIgnoreCase("description")) {
                            if (inItemTag) {
                                newsObject.setDescription(parser.nextText());
                            }
                        } else if (parser.getName().equalsIgnoreCase("image")) {
                            if (inItemTag) {
                                newsObject.setImageUrl(parser.nextText());
                            }
                        }
                    } else if (eventType == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("item")) {
                        news.add(newsObject);
                        inItemTag = false;
                    }
                    eventType = parser.next();
                }
            } catch (MalformedURLException e) {
                exception = e;
            } catch ( XmlPullParserException e) {
                exception = e;
            } catch (IOException e) {
                exception = e;
            }
            return exception;
        }

        @Override
        protected void onPostExecute(Exception ex) {
            super.onPostExecute(ex);

            if (ex != null) {
                Toast.makeText(MainActivity.this, "While fetching, an error is occurred. Please try again." + ex.getMessage(), Toast.LENGTH_SHORT).show();
            } else {
                listAdapter.notifyDataSetChanged();
            }

            progressDialog.dismiss();
        }
    }
}