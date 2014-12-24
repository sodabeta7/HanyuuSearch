package com.example.helloworld8;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.MashapeHello.R;
import com.mashape.unirest.http.*;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MainActivity extends FragmentActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        tts = new TextToSpeech(this, this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.imageView1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
        } else {
            mAlbumStorageDirFactory = new BaseAlbumDirFactory();
        }
        gif = (GifView)findViewById(R.id.progress_gif);
        gif.setMovieResource(R.raw.progress_gif);
        gif.setPaused(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onInit (int status) {
        tts.setLanguage(Locale.ENGLISH);
        tts.setSpeechRate(1);
    }



    public void onClickIdentify(View view) {
        TextView txtView = (TextView) findViewById(R.id.textView1);
        txtView.setText("I'm uploading");
        gif.setPaused(false);
        new IdentifyImage().execute();
    }
    private class IdentifyImage extends AsyncTask<String, Integer, HttpResponse<JsonNode>> {

        protected HttpResponse<JsonNode> doInBackground(String... msg) {

            HttpResponse<JsonNode> request = null;
            try {
                request = Unirest.post("https://camfind.p.mashape.com/image_requests")
                        .header("X-Mashape-Key", "55vDTIMyfdmshoCvD6k39tT2BgVCp1LbMYHjsn2ubCVgH3QDBi")
                        .field("image_request[image]", new File(Environment.getExternalStorageDirectory()
                                + File.separator + "test.jpg"))
                        .field("image_request[language]", "en")
                        .field("image_request[locale]", "en_US")
                        .asJson();
            } catch (UnirestException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return request;
        }

        protected void onProgressUpdate(Integer...integers) {
        }

        protected void onPostExecute(HttpResponse<JsonNode> response) {
            try {
                token = response.getBody().getObject().getString("token");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d("token", token);
            get_token = true;
            TextView txtView = (TextView) findViewById(R.id.textView1);
            txtView.setText("I'm identifying");
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    gif.setPaused(true);
                    TextView txtView = (TextView) findViewById(R.id.textView1);
                    txtView.setText("Completed!");
                    new IdentifyImageDisplay().execute();
                }
            }, 20 * 1000);
        }
    }
    private class IdentifyImageDisplay extends AsyncTask<String, Integer, HttpResponse<JsonNode>> {
        private String tt;

        protected HttpResponse<JsonNode> doInBackground(String... msg) {
            HttpResponse<JsonNode> request = null;
            try {
                Log.d("tokentoken", "https://camfind.p.mashape.com/image_responses/" + token);
                String url = "https://camfind.p.mashape.com/image_responses/" + token;
                try {
                    url = new String(url.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                final String api_key = "55vDTIMyfdmshoCvD6k39tT2BgVCp1LbMYHjsn2ubCVgH3QDBi";
                request = Unirest.get(url)
                        .header("X-Mashape-Key", api_key)
                        .asJson();
            } catch (UnirestException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return request;
        }

        protected void onProgressUpdate(Integer... integers) {
        }

        protected void onPostExecute(HttpResponse<JsonNode> response) {
            HashMap<String, String> myHashAlarm = new HashMap();
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                    String.valueOf(AudioManager.STREAM_ALARM));
            String answer = null;
            try {
                answer = response.getBody().getObject().getString("name");
                keyWords = answer;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            TextView txtView = (TextView) findViewById(R.id.textView1);
            txtView.setText("I guess it is " + keyWords);
            get_keywords = true;
        }
    }

    public void onClickTakePhoto(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File f = null;
        try {
            f = setUpPhotoFile();
            mCurrentPhotoPath = f.getAbsolutePath();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        } catch (IOException e) {
            e.printStackTrace();
            f = null;
            mCurrentPhotoPath = null;
        }
        startActivityForResult(takePictureIntent, TAKE_IMAGE_REQUEST_CODE);
    }
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_IMAGE_REQUEST_CODE: {
                get_keywords = false;
                get_token = false;
                if (resultCode == RESULT_OK) {
                    handlePhoto();
                }
                break;
            }
        }
    }
    private String getAlbumName() {
        return getString(R.string.album_name);
    }
    private File getAlbumDir() {
        File storageDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());
            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()) {
                        return null;
                    }
                }
            }
        } else {
        }
        return storageDir;
    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG" + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }
    private File setUpPhotoFile() throws IOException {
        File f = createImageFile();
        mCurrentPhotoPath = f.getAbsolutePath();
        mCurrentPhotoUri = Uri.fromFile(f);
        return f;
    }
    private void setPic() {
        mImageView.setImageURI(mCurrentPhotoUri);
        mImageView.setVisibility(View.VISIBLE);
        return;
    }
    private void handlePhoto() {
        if (mCurrentPhotoPath != null) {                    // we'll start with the original picture already open to a file
            File imgFileOrig = new File(mCurrentPhotoPath); //change "getPic()" for whatever you need to open the image file.
            Bitmap b = BitmapFactory.decodeFile(imgFileOrig.getAbsolutePath());
            // original measurements
            int origWidth = b.getWidth();
            int origHeight = b.getHeight();
            final int destWidth = 200;//or the width you need
            if (origWidth > destWidth) {
                // picture is wider than we want it, we calculate its target height
                int destHeight = origHeight / ( origWidth / destWidth ) ;
                // we create an scaled bitmap so it reduces the image, not just trim it
                Bitmap b2 = Bitmap.createScaledBitmap(b, destWidth, destHeight, false);
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                // compress to the format you want, JPEG, PNG...
                // 70 is the 0-100 quality percentage
                b2.compress(Bitmap.CompressFormat.JPEG, 70 , outStream);
                // we save the file, at least until we have made use of it
                File f = new File(Environment.getExternalStorageDirectory()
                        + File.separator + "test.jpg");
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //write the bytes in file
                FileOutputStream fo = null;
                try {
                    fo = new FileOutputStream(f);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    fo.write(outStream.toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    fo.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            setPic();
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onClickSearch(View view) {
        new SearchOptionDialog().show(getFragmentManager(),"search_option");
    }
    protected void onClickGoogleSearch() {
        TextView txtView = (TextView) findViewById(R.id.textView1);
        txtView.setText("I'm working");
        searchUsingGoogle();
    }
    protected void onClickAmazonSearch() {
        if(get_keywords) {

        }else {
            TextView txtView = (TextView) findViewById(R.id.textView1);
            txtView.setText("I'm uploading");
            gif.setPaused(false);
            new IdentifyImageAndSearchAmazon().execute();
        }
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public class SearchOptionDialog extends DialogFragment {
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.pick_search_item)
                    .setItems(R.array.search_option_item, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // The 'which' argument contains the index position
                            // of the selected item
                            if(which == 0)
                                onClickGoogleSearch();
                            else if(which == 1)
                                onClickAmazonSearch();
                        }
                    });
            return builder.create();
        }
    }
    private void searchUsingGoogle() {
        new GoogleSearchByImage().execute();
    }
    private class GoogleSearchByImage extends AsyncTask<String, Integer, HttpResponse<JsonNode>> {
        protected HttpResponse<JsonNode> doInBackground(String... msg) {
            HttpResponse<JsonNode> request = null;
            try {
                request = Unirest.post("http://uploads.im/api?")
                        .field("upload", new File(Environment.getExternalStorageDirectory()
                                + File.separator + "test.jpg"))
                        .asJson();
            } catch (UnirestException e) {
                e.printStackTrace();
            }
            return request;
        }
        protected void onPostExecute(HttpResponse<JsonNode> response) {
            Log.d("response", response.getBody().toString());
            try {
                JSONObject o = response.getBody().getObject().getJSONObject("data");
                String url = o.getString("img_url");
                startWebviewForSearchUsingGoogle(url);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private String create_url_google(String img_url) {
        final String base = "https://www.google.com.hk/searchbyimage?&image_url=";
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < img_url.length(); ++i) {
            if (img_url.charAt(i) != '\\')
                s.append(img_url.charAt(i));
        }
        return base + s.toString();
    }
    private class IdentifyImageAndSearchAmazon extends AsyncTask<String, Integer, HttpResponse<JsonNode>> {

        protected HttpResponse<JsonNode> doInBackground(String... msg) {

            HttpResponse<JsonNode> request = null;
            try {
                request = Unirest.post("https://camfind.p.mashape.com/image_requests")
                        .header("X-Mashape-Key", "55vDTIMyfdmshoCvD6k39tT2BgVCp1LbMYHjsn2ubCVgH3QDBi")
                        .field("image_request[image]", new File(Environment.getExternalStorageDirectory()
                                + File.separator + "test.jpg"))
                        .field("image_request[language]", "en")
                        .field("image_request[locale]", "en_US")
                        .asJson();
            } catch (UnirestException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return request;
        }

        protected void onProgressUpdate(Integer...integers) {
        }

        protected void onPostExecute(HttpResponse<JsonNode> response) {
            try {
                token = response.getBody().getObject().getString("token");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d("token", token);
            get_token = true;
            TextView txtView = (TextView) findViewById(R.id.textView1);
            txtView.setText("I'm identifying");
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    gif.setPaused(true);
                    TextView txtView = (TextView) findViewById(R.id.textView1);
                    txtView.setText("Completed!");
                    new IdentifyImageAndSearchAmazonGet().execute();
                }
            }, 20 * 1000);
        }
    }
    private class IdentifyImageAndSearchAmazonGet extends AsyncTask<String, Integer, HttpResponse<JsonNode>> {
        private String tt;

        protected HttpResponse<JsonNode> doInBackground(String... msg) {
            HttpResponse<JsonNode> request = null;
            try {
                Log.d("tokentoken", "https://camfind.p.mashape.com/image_responses/" + token);
                String url = "https://camfind.p.mashape.com/image_responses/" + token;
                try {
                    url = new String(url.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                final String api_key = "55vDTIMyfdmshoCvD6k39tT2BgVCp1LbMYHjsn2ubCVgH3QDBi";
                request = Unirest.get(url)
                        .header("X-Mashape-Key", api_key)
                        .asJson();
            } catch (UnirestException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return request;
        }

        protected void onProgressUpdate(Integer... integers) {
        }

        protected void onPostExecute(HttpResponse<JsonNode> response) {
            HashMap<String, String> myHashAlarm = new HashMap();
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                    String.valueOf(AudioManager.STREAM_ALARM));
            String answer = null;
            try {
                answer = response.getBody().getObject().getString("name");
                keyWords = answer;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            TextView txtView = (TextView) findViewById(R.id.textView1);
            txtView.setText("I guess it is " + keyWords);
            get_keywords = true;
            startWebviewForSearchAmazon();
        }
    }
    private void startWebviewForSearchAmazon() {
        String url_base = "http://www.amazon.com/s/ref=nb_sb_noss?url=search-alias%3Daps&field-keywords=";
        String ks = keyWords;
        String[] as = ks.split(" ");
        StringBuilder s = new StringBuilder(url_base);
        s.append(as[0]);
        for (int i = 1; i < as.length; ++i) {
            s.append('+' + as[i]);
        }
        String url = s.toString();
        Intent intent = new Intent(this, WebForSearch.class);
        intent.putExtra(SEARCH_URL_MESSAGE, url);
        Log.d("search_amazon_url", url);
        startActivity(intent);
    }
    private void startWebviewForSearchUsingGoogle(String url) {
        Intent intent = new Intent(this, WebForSearch.class);
        String s_url = create_url_google(url);
        intent.putExtra(SEARCH_URL_MESSAGE, s_url);
        Log.d("search_url", s_url);
        TextView txtView = (TextView) findViewById(R.id.textView1);
        txtView.setText("Completed!");
        startActivity(intent);
    }


    private String create_url_camfind() {
        //google search
        String base = "https://www.google.com.hk/#safe=strict&q=";
        String ks = keyWords;
        String[] as = ks.split(" ");
        StringBuilder s = new StringBuilder(base);
        s.append(as[0]);
        for (int i = 1; i < as.length; ++i) {
            s.append('+' + as[i]);
        }
        return s.toString();
    }
    protected void startWebviewForSearch() {
        Intent intent = new Intent(this, WebForSearch.class);
        String s_url = create_url_camfind();
        intent.putExtra(SEARCH_URL_MESSAGE, s_url);
        startActivity(intent);
    }

    private ImageView mImageView;
    private String mCurrentPhotoPath;
    private Uri mCurrentPhotoUri;
    private String token;
    private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private final int TAKE_IMAGE_REQUEST_CODE = 1;
    private String keyWords;
    private GifView gif;
    private boolean get_keywords = false;
    private boolean get_token = false;
    protected final static String SEARCH_URL_MESSAGE = "com.example.helloworld8.search_url_message";
}

