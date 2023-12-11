package com.foobnix.pdf.info.dictionary;

import android.os.AsyncTask;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.BuildConfig;

import java.io.IOException;

import okhttp3.*;

public class InAppDictionary extends AsyncTask<String, Void, String> {

    private final OkHttpClient client;

    public InAppDictionary() {
        client = new OkHttpClient();
    }

    @Override
    protected String doInBackground(String... selectedText) {
        String requestBody = "{\"q\":\"" + selectedText[0] + "\",\"source\":\"it\",\"target" +
                "\":\"en\",\"format\":\"text\"}";
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), requestBody);

        String apiKey = BuildConfig.TRANSLATE_API_KEY;
        Request request = new Request.Builder()
                .url("https://translation.googleapis.com/language/translate/" +
                        "v2?key=" + apiKey)
                .post(body)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                assert response.body() != null;
                String responseText = response.body().string();
                LOG.d("Translation is: " + responseText);
                return responseText;
            } else {
                throw new IOException(response.message());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
