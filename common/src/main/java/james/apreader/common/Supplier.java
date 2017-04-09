package james.apreader.common;

import android.app.Application;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import james.apreader.common.data.AuthorData;
import james.apreader.common.data.WallData;
import james.apreader.common.utils.ElementUtils;
import james.apreader.common.utils.FontUtils;

public class Supplier extends Application {

    private String url;
    private int pages;

    private AuthorData author;
    private ArrayList<WallData> wallpapers;

    private ArrayList<String> favWallpapers;

    private Typeface typeface;

    private SharedPreferences prefs;
    private Gson gson;

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        gson = new Gson();

        url = getString(R.string.feed_url);

        favWallpapers = new ArrayList<>();

        int size = prefs.getInt("favorites-size", 0);
        for (int i = 0; i < size; i++) {
            String json = prefs.getString("favorites-" + i, null);
            favWallpapers.add(json);
        }
    }

    public boolean getNetworkResources() {
        //download any resources needed for the voids below while the splash screen is showing
        //yes, this is thread-safe
        //no, it is not needed for the current setup since all the resources are in res/values/strings.xml

        wallpapers = new ArrayList<>();

        try {
            Document document = ElementUtils.getDocument(new URL(url));
            if (document == null) return false;

            author = new AuthorData(document.title(), ElementUtils.getDescription(document), 0, url.substring(0, url.length() - 5), url);

            Elements elements = document.select("item");
            for (Element element : elements) {
                WallData data = new WallData(ElementUtils.getName(element), ElementUtils.getDescription(element), ElementUtils.getDate(element), ElementUtils.getLink(element), ElementUtils.getComments(element), ElementUtils.getImages(element), ElementUtils.getCategories(element), author.name, author.id);
                wallpapers.add(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public Typeface getTypeface() {
        if (typeface == null)
            typeface = FontUtils.getTypeface(this);

        return typeface;
    }

    //get a list of the different sections
    public AuthorData getAuthor() {
        return author;
    }

    //get a list of the different wallpapers
    public ArrayList<WallData> getWallpapers() {
        return new ArrayList<>(wallpapers);
    }

    public void getWallpapers(final AsyncListener<ArrayList<WallData>> listener) {
        new Thread() {
            @Override
            public void run() {
                final ArrayList<WallData> walls = new ArrayList<>();

                Document document;
                try {
                    document = ElementUtils.getDocument(new URL(url + "?paged=" + String.valueOf(pages + 2)));
                } catch (IOException e) {
                    e.printStackTrace();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onFailure();
                        }
                    });
                    return;
                }

                if (document == null) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onFailure();
                        }
                    });
                    return;
                }

                Elements elements = document.select("item");
                for (Element element : elements) {
                    WallData data = new WallData(ElementUtils.getName(element), ElementUtils.getDescription(element), ElementUtils.getDate(element), ElementUtils.getLink(element), ElementUtils.getComments(element), ElementUtils.getImages(element), ElementUtils.getCategories(element), author.name, 0);
                    walls.add(data);
                }

                wallpapers.addAll(walls);
                pages++;

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onTaskComplete(walls);
                    }
                });
            }
        }.start();
    }

    public void getFullContent(final WallData data, final AsyncListener<String> listener) {
        new Thread() {
            @Override
            public void run() {
                String content = null;
                try {
                    URLConnection connection = new URL(data.url).openConnection();
                    connection.connect();

                    InputStream stream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                    StringBuilder builder = new StringBuilder();
                    for (String line; (line = reader.readLine()) != null; ) {
                        builder.append(line);
                    }

                    stream.close();
                    content = builder.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (content != null) {
                    Document document = Jsoup.parse(content);
                    Elements elements = document.getElementsByClass("post-content");
                    if (elements.size() > 0) {
                        Element element = elements.first();
                        element.select("img").remove();
                        element.select("script").remove();
                        content = element.html();
                    } else content = null;
                }

                final String article = content;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (article != null)
                            listener.onTaskComplete(article);
                        else listener.onFailure();
                    }
                });
            }
        }.start();
    }

    public ArrayList<WallData> getFavoriteWallpapers() {
        ArrayList<WallData> walls = new ArrayList<>();
        for (String string : favWallpapers) {
            walls.add(gson.fromJson(string, WallData.class));
        }

        return walls;
    }

    public boolean setFavoriteWallpapers() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("favorites-size", favWallpapers.size());

        for (int i = 0; i < favWallpapers.size(); i++) {
            editor.putString("favorites-" + i, favWallpapers.get(i));
        }

        return editor.commit();
    }

    public boolean isFavorite(WallData data) {
        return favWallpapers.contains(gson.toJson(data));
    }

    public boolean favoriteWallpaper(WallData data) {
        if (isFavorite(data)) return false;

        favWallpapers.add(gson.toJson(data));
        return setFavoriteWallpapers();
    }

    public boolean unfavoriteWallpaper(WallData data) {
        if (!isFavorite(data)) return false;

        favWallpapers.remove(favWallpapers.indexOf(gson.toJson(data)));
        return setFavoriteWallpapers();
    }

    public void downloadWallpaper(Context context, String name, String url) {
        //start a download
        DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
        r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name + ".png");
        r.allowScanningByMediaScanner();
        r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        dm.enqueue(r);
    }

    //share a wallpaper
    public void shareWallpaper(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_STREAM, String.valueOf(Uri.parse(url)));
        context.startActivity(intent);
    }

    public interface AsyncListener<E> {
        void onTaskComplete(E value);

        void onFailure();
    }
}
