package com.foobnix.pdf.info.dictionary;

import com.foobnix.pdf.info.BuildConfig;

import java.io.IOException;

import okhttp3.*;

public class InAppDictionary {

    private final OkHttpClient client;

    public InAppDictionary() {
        client = new OkHttpClient();
    }

    public String makePostRequest(String selectedText) {
        String requestBody = "{\"q\":\"" + selectedText + "\",\"source\":\"it\",\"target" +
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
                return response.body().string();
            } else {
                throw new IOException(response.message());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
