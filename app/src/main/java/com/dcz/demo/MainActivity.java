package com.dcz.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.dcz.demo.databinding.ActivityMainBinding;
import com.dcz.fileportal.FilePortal;
import com.dcz.fileportal.network.ProgressListener;
import com.dcz.fileportal.network.callbacks.FileUploadResultCallback;
import com.dcz.fileportal.options.AccessOption;
import com.dcz.fileportal.options.ModeOption;
import com.dcz.fileportal.options.UploadOptions;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String FILE_SYSTEM_API_KEY = "78b8d9185810072a";

    private static final String TAG = "dcz";

    private static final String UID = "2000";

    private static final int REQUEST_CODE_CHOOSE_PIC_FROM_GALLERY = 100;

    private boolean needVerify;

    private String token;

    private String url;

    private ActivityMainBinding binding;
    private Uri currentUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnSelectPic.setOnClickListener(v -> {
            if (requestStoragePermission()) {
                choosePictureFromGallery();
            }
        });
        binding.btnBrowser.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });
        binding.btnUpload.setOnClickListener(v -> upload());
        FilePortal.getInstance().getToken(this, UID, FILE_SYSTEM_API_KEY, token -> {
            MainActivity.this.token = token;
            Log.e(TAG, "token:" + token);
        });
    }

    /**
     * 请求读取外部存储权限。
     */
    private boolean requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return false;
        }
        return true;
    }

    /**
     * 从相册选取图片。
     */
    private void choosePictureFromGallery() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_CHOOSE_PIC_FROM_GALLERY);
    }

    /**
     * 上传文件。
     */
    private void upload() {
        if (currentUri == null) {
            toast("No pictures selected");
            return;
        }
        File file = new File(getRealPathFromURI(currentUri));
        FileUploadResultCallback callback = new FileUploadResultCallback() {
            @Override
            public void onSuccess(String url, boolean isCached) {
                binding.info.setText(isCached ? "Cached" : "Newly uploaded");
                if (needVerify) {
                    url = url + "&token=" + token;
                }
                MainActivity.this.url = url;
                RequestOptions options = new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true);
                Glide.with(binding.ivUploaded)
                        .load(url)
                        .apply(options)
                        .into(binding.ivUploaded);
                Log.e(TAG, "success:" + url + " isCached:" + isCached);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, e.getMessage());
                Glide.with(binding.ivUploaded)
                        .load(R.drawable.ic_launcher_background)
                        .into(binding.ivUploaded);
            }
        };
        ProgressListener progressListener = percent -> {
            Log.e(TAG, "progress:" + percent);
            binding.progressbar.setProgress(percent);
        };
        UploadOptions options = new UploadOptions.Builder(this, "1000", FILE_SYSTEM_API_KEY, file)
                .setFileUploadResultCallback(callback)
                .setProgressListener(progressListener)
                .setAccessVerify(binding.cbVerify.isChecked() ? AccessOption.ACCESS_VERIFY : AccessOption.ACCESS_NO_VERIFY)
                .setMode(binding.cbKeep.isChecked() ? ModeOption.MODE_STAY : ModeOption.MODE_DELETE)
                .build();
        FilePortal.getInstance().upload(options);
        needVerify = binding.cbVerify.isChecked();
    }

    /**
     * 获取Uri对应的文件路径。
     *
     * @param contentURI Source uri。
     */
    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE_PIC_FROM_GALLERY && resultCode == RESULT_OK && data != null) {
            currentUri = data.getData();
            Glide.with(binding.ivToUpload)
                    .load(currentUri)
                    .into(binding.ivToUpload);
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}