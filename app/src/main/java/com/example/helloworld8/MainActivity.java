package com.example.helloworld8;

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
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.MashapeHello.R;
import com.mashape.unirest.http.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONException;
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

public class MainActivity extends Activity implements TextToSpeech.OnInitListener{

    private TextToSpeech tts;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        tts = new TextToSpeech(this,this);
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
    public void onInit (int status){
        tts.setLanguage(Locale.ENGLISH);
        tts.setSpeechRate(1);
    }
    private String create_url() {
        //google search
        String base = "https://www.google.com.hk/#safe=strict&q=";
        String ks = keyWords;
        String[] as = ks.split(" ");
        StringBuilder s = new StringBuilder(base);
        s.append(as[0]);
        for(int i=1;i<as.length;++i) {
            s.append('+'+as[i]);
        }
        return s.toString();
    }
	protected void startWebviewForSearch() {
        Intent intent = new Intent(this, WebForSearch.class);
        String s_url = create_url();
        intent.putExtra(SEARCH_URL_MESSAGE,s_url);
        startActivity(intent);
    }
    public void search(View view) {
        TextView txtView = (TextView) findViewById(R.id.textView1);
    	txtView.setText("Please wait!");
        new CallMashapeAsync1().execute(token);
//        keyWords = "baidu logo";
//        startWebviewForSearch();
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
                    if (! storageDir.exists()){
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
    private void setPic() {
        mImageView.setImageURI(mCurrentPhotoUri);
        mImageView.setVisibility(View.VISIBLE);
        return;
    }
    private void handlePhoto() {
        if (mCurrentPhotoPath != null) {
            // we'll start with the original picture already open to a file
            File imgFileOrig = new File(mCurrentPhotoPath); //change "getPic()" for whatever you need to open the image file.
            Bitmap b = BitmapFactory.decodeFile(imgFileOrig.getAbsolutePath());
// original measurements
            int origWidth = b.getWidth();
            int origHeight = b.getHeight();

            final int destWidth = 200;//or the width you need

            if(origWidth > destWidth){
                // picture is wider than we want it, we calculate its target height
                int destHeight = origHeight/( origWidth / destWidth ) ;
                // we create an scaled bitmap so it reduces the image, not just trim it
                Bitmap b2 = Bitmap.createScaledBitmap(b, destWidth, destHeight, false);
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                // compress to the format you want, JPEG, PNG...
                // 70 is the 0-100 quality percentage
                b2.compress(Bitmap.CompressFormat.JPEG,70 , outStream);
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
                // remember close de FileOutput
                try {
                    fo.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            setPic();
            //processPhoto();
            //mCurrentPhotoPath = null;
        }
    }
    private void handleProgressGif() {
        gif.setPaused(false);
        TextView txtView = (TextView) findViewById(R.id.textView1);
        txtView.setText("Uploading");
    }
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_IMAGE_REQUEST_CODE: {
                if (resultCode == RESULT_OK) {
                    handlePhoto();
                    handleProgressGif();
                    new CallMashapeAsync().execute("where");
                }
                break;
            }

        }
    }
    private class CallMashapeAsync extends AsyncTask<String, Integer, HttpResponse<JsonNode>> {

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
//				// TODO Auto-generated catch block
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
            gif.setPaused(true);
            TextView txtView = (TextView) findViewById(R.id.textView1);
            txtView.setText("Upload Completed");
        }
    }
    private class CallMashapeAsync1 extends AsyncTask<String, Integer, HttpResponse<JsonNode>> {
        private String tt;

        protected HttpResponse<JsonNode> doInBackground(String... msg) {
            tt = msg[0];
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
//				// TODO Auto-generated catch block
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
//            TextView txtView = (TextView) findViewById(R.id.textView1);
//            txtView.setText(answer);
//            tts.speak(answer, TextToSpeech.QUEUE_FLUSH, myHashAlarm);
            startWebviewForSearch();
        }
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
    protected final static String SEARCH_URL_MESSAGE = "com.example.helloworld8.search_url_message";
}

