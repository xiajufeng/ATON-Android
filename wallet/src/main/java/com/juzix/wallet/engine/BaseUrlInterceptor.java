package com.juzix.wallet.engine;

import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class BaseUrlInterceptor implements Interceptor {

    private final static String TAG = BaseUrlInterceptor.class.getSimpleName();

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String header = request.header("name");
        if (ServerUtils.HEADER_UPDATE_VERSION.equals(header)) {
            return chain.proceed(request);
        } else {
            String pathSegments = TextUtils.join("/", request.url().pathSegments());
            HttpUrl newHttpUrl = HttpUrl.parse(NodeManager.getInstance().getCurNode().getNodeAddress()).newBuilder().addPathSegments(pathSegments).build();
            return chain.proceed(request.newBuilder()
                    .url(newHttpUrl)
                    .build());
        }
    }
}
