package com.dcz.fileportal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dcz.fileportal.exceptions.DirectoryProvidedException;
import com.dcz.fileportal.exceptions.EmptyFileException;
import com.dcz.fileportal.exceptions.InvalidTimeException;
import com.dcz.fileportal.exceptions.NoTokenException;
import com.dcz.fileportal.exceptions.UploadFailedException;
import com.dcz.fileportal.network.CountingFileRequestBody;
import com.dcz.fileportal.network.ProgressListener;
import com.dcz.fileportal.network.TrustAllManager;
import com.dcz.fileportal.network.bean.Response;
import com.dcz.fileportal.network.callbacks.FileUploadResultCallback;
import com.dcz.fileportal.network.callbacks.TokenResultCallback;
import com.dcz.fileportal.options.UploadOptions;
import com.dcz.fileportal.utils.ContentType;
import com.dcz.fileportal.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import static com.dcz.fileportal.Constants.NET_KEY_ACCESS;
import static com.dcz.fileportal.Constants.NET_KEY_FILE;
import static com.dcz.fileportal.Constants.NET_KEY_MD5;
import static com.dcz.fileportal.Constants.NET_KEY_MD_UPPER;
import static com.dcz.fileportal.Constants.NET_KEY_MODE;
import static com.dcz.fileportal.Constants.NET_KEY_PKG;
import static com.dcz.fileportal.Constants.NET_KEY_SIGNATURE;
import static com.dcz.fileportal.Constants.NET_KEY_TOKEN;
import static com.dcz.fileportal.Constants.NET_KEY_UID;
import static com.dcz.fileportal.Constants.RET_SUCCESS;
import static com.dcz.fileportal.options.AccessOption.ACCESS_VERIFY;
import static com.dcz.fileportal.options.ModeOption.MODE_STAY;

/**
 * 文件传送门。
 * todo 注意：目前后台存在文件size限制，最大值为20MB。
 */
public class FilePortal {

    private static volatile FilePortal mInstance;
    private OkHttpClient mClient;
    private final Gson mGson;
    private SharedPreferences mSP;
    private final ExecutorService mExecutor;
    private final Handler mHandler;

    private FilePortal() {
        //todo 超时、重试策略。
        initOkHttpClient();
        mGson = new Gson();
        mExecutor = Executors.newCachedThreadPool();
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Initialize the OkHttp Client which is used globally.
     */
    private void initOkHttpClient() {
        TrustAllManager trustAllManager = new TrustAllManager();
        mClient = new OkHttpClient.Builder()
                //直接忽略证书校验并不是解决证书不被系统认可的最佳实践。 @see TrustAllManager.java。
                .sslSocketFactory(Objects.requireNonNull(TrustAllManager.createTrustAllSSLFactory(trustAllManager)), trustAllManager)
                .hostnameVerifier(TrustAllManager.createTrustAllHostnameVerifier())
                .build();
    }

    /**
     * Get the singleton instance.
     *
     * @return Singleton instance of FilePortal.
     */
    public static FilePortal getInstance() {
        if (mInstance == null) {
            synchronized (FilePortal.class) {
                if (mInstance == null) {
                    mInstance = new FilePortal();
                }
            }
        }
        return mInstance;
    }

    /**
     * Exposed api for upload file.
     *
     * @param uploadOptions Options of the upload task.
     */
    public void upload(@NonNull UploadOptions uploadOptions) {
        final Context context = uploadOptions.getContext();
        final String uid = uploadOptions.getUid();
        final String apiKey = uploadOptions.getApiKey();
        final File file = uploadOptions.getFile();
        final String accessVerify = uploadOptions.getAccessVerify();
        final String mode = uploadOptions.getMode();
        final FileUploadResultCallback callback = uploadOptions.getFileUploadResultCallback();
        final ProgressListener progressListener = uploadOptions.getProgressListener();
        upload(context, uid, apiKey, file, accessVerify, mode, callback, progressListener);
    }

    /**
     * Get token and start uploading the file.
     *
     * @param context          current context.
     * @param uid              Unique uid of the user who is to access the file.
     * @param apiKey           Registered api key for Marvel File System.
     * @param file             The file to upload.
     * @param accessVerify     Weather token is needed to access the file.
     * @param mode             The strategy of keeping files.
     * @param callback         File upload result callback.
     * @param progressListener The listener of uploading progress.
     */
    private void upload(@NonNull final Context context, @NonNull final String uid, @NonNull final String apiKey,
                        final @NonNull File file, @NonNull final String accessVerify, @NonNull final String mode, @Nullable final FileUploadResultCallback callback,
                        @Nullable ProgressListener progressListener) {
        if (file.isDirectory()) {
            onFailureCallback(callback, new DirectoryProvidedException());
            return;
        }
        if (file.length() == 0) {
            onFailureCallback(callback, new EmptyFileException());
            return;
        }
        Runnable runnable = () -> {
            final String token = getToken(context, uid, apiKey);
            if (token == null) {
                onFailureCallback(callback, new NoTokenException());
            } else {
                String fileMD5 = Utils.md5(file);//todo 是否会遇到OOM
                final boolean serverCached = preUpload(token, fileMD5, callback);
                //No cache in the backend server.Then do upload.
                if (!serverCached) {
                    doUpload(file, fileMD5, token, accessVerify, mode, callback, progressListener);
                } else if (progressListener != null) {
                    progressListener.transferred(100);
                }
            }
        };
        mExecutor.execute(runnable);
    }

    /**
     * Check before do upload task.
     * If the same file has been uploaded,we'll get a url.Otherwise ret_code != 200,start upload.
     *
     * @param token    token.
     * @param callback FileUploadResultCallback.
     * @param fileMD5  The MD5 hash which will be used to check if the file has exists on backend server of the file.
     * @return Weather the same file has benn uploaded.
     */
    private boolean preUpload(final String token, final String fileMD5, @Nullable final FileUploadResultCallback callback) {
        RequestBody requestBody = new FormBody.Builder()
                .add(NET_KEY_MD5, fileMD5)
                .add(NET_KEY_TOKEN, token)
                .add(NET_KEY_MODE, MODE_STAY)
                .add(NET_KEY_ACCESS, ACCESS_VERIFY)
                .build();
        Request request = new Request.Builder()
                .url(Constants.API_PRE_UPLOAD)
                .post(requestBody)
                .build();
        try (okhttp3.Response response = mClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                final Response<Response.UploadBean> preloadInfo = mGson.fromJson(response.body().string(), new TypeToken<Response<Response.UploadBean>>() {
                }.getType());
                if (preloadInfo.getRet() == RET_SUCCESS) {
                    onSuccessCallback(callback, preloadInfo.getData().getUrl(), true);
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Upload the file to the backend server.
     *
     * @param file             The file to be uploaded.
     * @param fileMD5          The MD5 hash of the file.
     * @param token            verification token.
     * @param callback         The callback of file uploading result.
     * @param accessVerify     Weather token is needed to access the file.
     * @param mode             The strategy of keeping files.
     * @param progressListener The listener of uploading progress.
     */
    private void doUpload(@NonNull final File file, final String fileMD5, final String token,
                          String accessVerify, String mode, @Nullable final FileUploadResultCallback callback
            , @Nullable ProgressListener progressListener) {
        final MediaType mediaType = MediaType.parse(ContentType.getContentTypeFromExtension(file.getName(), ContentType.IMAGE_PREFIX));//todo 如何选择一个合适的默认值。
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(NET_KEY_MD_UPPER, fileMD5)
                .addFormDataPart(NET_KEY_TOKEN, token)
                .addFormDataPart(NET_KEY_ACCESS, accessVerify)
                .addFormDataPart(NET_KEY_MODE, mode)
                .addFormDataPart(NET_KEY_FILE, file.getName(), RequestBody.create(mediaType, file));
        if (progressListener != null) {
            builder.addPart(new CountingFileRequestBody(file, percent -> onProgressCallback(progressListener, percent)));
        }
        RequestBody requestBody = builder.build();
        Request request = new Request.Builder()
                .url(Constants.API_UPLOAD)
                .post(requestBody)
                .build();
        try (okhttp3.Response response = mClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                Response<Response.UploadBean> uploadInfo = mGson.fromJson(response.body().string(), new TypeToken<Response<Response.UploadBean>>() {
                }.getType());
                //Succeed to upload the file.
                if (uploadInfo.getRet() == RET_SUCCESS) {
                    onSuccessCallback(callback, uploadInfo.getData().getUrl(), false);
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            onFailureCallback(callback, e);
            return;
        }
        onFailureCallback(callback, new UploadFailedException());
    }

    /**
     * If there is a valid token for the user,use it directly.Otherwise get a new token from backend server.
     *
     * @param uid    The unique uid of current user.
     * @param apiKey Api key registered by developer for Marvel File System.
     * @return A token gotten from local or backend server.
     */
    private String getToken(final Context context, final String uid, final String apiKey) {
        String validToken = getTokenFromLocal(context, uid);
        if (validToken != null) return validToken;
        //No valid token found,then fetch a token from backend server.
        String packageName = context.getPackageName();
        String sourceParams = uid + apiKey + packageName;
        String signature = Utils.md5(sourceParams);
        RequestBody requestBody = new FormBody.Builder()
                .add(NET_KEY_UID, uid)
                .add(NET_KEY_SIGNATURE, signature)
                .add(NET_KEY_PKG, packageName)
                .build();
        Request request = new Request.Builder()
                .url(Constants.API_TOKEN)
                .post(requestBody)
                .build();
        try (okhttp3.Response response = mClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                Response<Response.TokenBean> tokenResponse = mGson.fromJson(response.body().string(), new TypeToken<Response<Response.TokenBean>>() {//todo 为什么判空无效。
                }.getType());
                if (tokenResponse.getRet() == RET_SUCCESS) {
                    //Cache the token info
                    tokenResponse.getData().setUid(uid);
                    getSP(context).edit().putString(Constants.PREFS_TOKEN_JSON, mGson.toJson(tokenResponse.getData())).apply();
                    return tokenResponse.getData().getToken();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get token Asynchronously.
     *
     * @param uid    The unique uid of current user.
     * @param apiKey Api key registered by developer for Marvel File System.
     */
    public void getToken(@NonNull final Context context, @NonNull final String uid, @NonNull final String apiKey, @NonNull final TokenResultCallback callback) {
        Runnable runnable = () -> {
            String token = getToken(context, uid, apiKey);
            mHandler.post(() -> callback.onTokenResult(token));
        };
        mExecutor.execute(runnable);
    }

    /**
     * Get cached token for shared preference.
     *
     * @param context current context。
     * @return Provide cached token if not null and not outdated.
     */
    private String getTokenFromLocal(@NonNull final Context context, String uid) {
        //Get the cached token info.
        String tokenJson = getSP(context).getString(Constants.PREFS_TOKEN_JSON, null);
        if (tokenJson == null) return null;//No cached token info found.
        Response.TokenBean tokenInfo = mGson.fromJson(tokenJson, Response.TokenBean.class);
        try {
            long expireTimestamp = Utils.dateStr2Timestamp(tokenInfo.getExpireTime());
            //Cached token is out of date.
            if (System.currentTimeMillis() > expireTimestamp) return null;
        } catch (InvalidTimeException ignored) {
            //Failed to get expire time,abandon the token anyway.
            return null;
        }
        //The cached token belongs to another user.
        if (!TextUtils.equals(tokenInfo.getUid(), uid)) {
            return null;
        }
        return tokenInfo.getToken();
    }

    /**
     * Post success info to main thread when the file is uploaded successfully.
     *
     * @param callback The callback of file uploading result.
     * @param url      The download url of the uploaded file.
     * @param isCached if the file cached by the backend server.
     */
    private void onSuccessCallback(@Nullable FileUploadResultCallback callback, String url, boolean isCached) {
        if (callback != null) {
            mHandler.post(() -> callback.onSuccess(url, isCached));
        }
    }

    /**
     * Post failure info to main thread when the file failed to upload.
     *
     * @param callback The callback of file uploading result.
     * @param e        The exception for which file uploaded failed.
     */
    private void onFailureCallback(@Nullable FileUploadResultCallback callback, Exception e) {
        if (callback != null) {
            mHandler.post(() -> callback.onFailure(e));
        }
    }

    /**
     * Post the progress to main thread when uploading progress changed.
     *
     * @param progressListener The listener of file uploading progress.
     * @param progress         Current progress of the uploading task.
     */
    private void onProgressCallback(@Nullable ProgressListener progressListener, int progress) {
        if (progressListener != null) {
            mHandler.post(() -> progressListener.transferred(progress));
        }
    }

    /**
     * Shut down the executor.
     */
    public void shutDownAllTasks() {
        if (!mExecutor.isShutdown())
            mExecutor.shutdown();
    }

    /**
     * Shut down the executor.
     */
    public void shutDownAllTasksNow() {
        if (!mExecutor.isShutdown())
            mExecutor.shutdownNow();
    }

    /**
     * Get shared preference object.
     *
     * @param context current context.
     * @return Shared preference object.
     */
    private SharedPreferences getSP(@NonNull final Context context) {
        if (mSP == null)
            mSP = context.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return mSP;
    }
}
