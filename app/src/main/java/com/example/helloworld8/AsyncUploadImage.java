package com.example.helloworld8;

import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.example.MashapeHello.R;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.json.JSONException;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * Created by sodabeta on 14-12-24.
 */
public class AsyncUploadImage {
    private String img_url;
    private File img_file;
    private String result_url;
    AsyncUploadImage() {
    }
    public void work(String url) {
        new CallMashapeAsync().execute();
    }
    class CallMashapeAsync extends AsyncTask<String, Integer, HttpResponse<JsonNode>> {

        protected HttpResponse<JsonNode> doInBackground(String... msg) {
            HttpResponse<JsonNode> request = null;
            try {
                request = Unirest.post("http://uploads.im/api?")
                        .header("accept", "application/json")
//                .field("upload", new File(Environment.getExternalStorageDirectory()
//                        + File.separator + "test.jpg"))
                        .field("upload", "http://www.google.com/images/srpr/nav_logo66.png")
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
                result_url = response.getBody().getObject().getString("img_url");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
