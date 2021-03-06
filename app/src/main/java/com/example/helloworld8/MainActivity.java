package com.example.helloworld8;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.provider.DocumentsContract;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onClickTakePhoto(View v) {
        new ImageOptionDialog().show(getFragmentManager(),"image_option");
    }
    public void onClickCamera() {
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
    public void onClickSelectImage() {
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);//ACTION_OPEN_DOCUMENT
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/jpeg");
        if(android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.KITKAT){
            startActivityForResult(intent, SELECT_PIC_KITKAT);
        }else{
            startActivityForResult(intent, SELECT_PIC);
        }
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class ImageOptionDialog extends DialogFragment {
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.pick_search_item)
                    .setItems(R.array.image_option_item, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // The 'which' argument contains the index position
                            // of the selected item
                            if(which == 0)
                                ((MainActivity)getActivity()).onClickCamera();
                            else if(which == 1)
                                ((MainActivity)getActivity()).onClickSelectImage();
                        }
                    });
            return builder.create();
        }
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
            case SELECT_PIC_KITKAT: {
                Log.d("select", "1");
                if(data != null && data.getData() != null) {
                    Uri _uri = data.getData();
                    //User had pick an image.
//                    Cursor cursor = getContentResolver().query(_uri, new String[] { android.provider.MediaStore.Images.ImageColumns.DATA }, null, null, null);
//                    cursor.moveToFirst();
                    //Link to the image
                    mCurrentPhotoPath = getPath(this,_uri);
                    if(mCurrentPhotoPath != null) {
                        Log.d("mCurrentPhotoPath", mCurrentPhotoPath);
                    }else
                        Log.d("mCurrentPhotoPath", "null");
//                    cursor.close();
                    handlePhoto();
                }
                break;
            }
            case SELECT_PIC: {
                Log.d("select", "2");
                break;
            }
        }
    }
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
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
            mCurrentPhotoUri = Uri.fromFile(imgFileOrig);
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
            startWebviewForSearchAmazon();
        }else {
            TextView txtView = (TextView) findViewById(R.id.textView1);
            txtView.setText("I'm uploading");
            gif.setPaused(false);
            new IdentifyImageAndSearchAmazon().execute();
        }
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SearchOptionDialog extends DialogFragment {
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.pick_search_item)
                    .setItems(R.array.search_option_item, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // The 'which' argument contains the index position
                            // of the selected item
                            if(which == 0)
                                ((MainActivity)getActivity()).onClickGoogleSearch();
                            else if(which == 1)
                                ((MainActivity)getActivity()).onClickAmazonSearch();
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
    private static final int SELECT_PIC_KITKAT = 100;
    private static final int SELECT_PIC = 101;
}

