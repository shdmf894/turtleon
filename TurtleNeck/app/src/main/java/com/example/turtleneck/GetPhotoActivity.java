package com.example.turtleneck;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import java.lang.String;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class GetPhotoActivity extends MainActivity {
    final String TAG = getClass().getSimpleName();
    ImageView imageView;
    Button GoCam;
    Button GoServer;
    final static int TAKE_PHOTO = 1;

    String mCurrentPhotoPath;

    static final int REQUEST_TAKE_PHOTO = 1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_getphoto);

        imageView = findViewById(R.id.DiaPht);//이미지뷰
        GoCam = findViewById(R.id.GoCam);//카메라
        GoServer=findViewById(R.id.GoServer);//서버로 전송


        //서버로
        GoServer.setOnClickListener(this);
       // 카메라로
       GoCam.setOnClickListener(this);
       //
        // 6.0 마쉬멜로우 이상일 경우에는 권한 체크 후 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ) {
                Log.d(TAG, "권한 설정 완료");
            } else {
                Log.d(TAG, "권한 설정 요청");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

    }

    // 권한 요청
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult");
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED ) {
            Log.d(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
        }
    }

    // 버튼 클릭 이벤트리스너 처리
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.GoCam: {
                // 카메라 앱을 여는 소스
                dispatchTakePictureIntent();
                break;
            }
            case R.id.GoServer:{
                //서버로 보내는 함수호출!!!!!!
                uploadImage();
                break;
            }
        }
    }



    // 카메라 실행
    private void dispatchTakePictureIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 인텐트 확인
        if (intent.resolveActivity(getPackageManager()) != null) {
            // 파일 만들고 초기화
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // 에러난 경우
            }
            // 파일이 정상적으로 만들어지면 계속
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.turtleneck.fileprovider",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    // 카메라로 촬영한 이미지를 파일로 저장
    private File createImageFile() throws IOException {
        // 파일 이름 만들기

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // 파일 저장
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }




    // 카메라로 촬영한 영상을 가져오는 부분
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        try {
            switch(requestCode) {
                case REQUEST_TAKE_PHOTO : {
                    if(resultCode == RESULT_OK) {
                        File file = new File(mCurrentPhotoPath);
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(file));

                        if(bitmap != null) {
                            ExifInterface ei = new ExifInterface(mCurrentPhotoPath);
                            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

                            // 회전된 사진
                            Bitmap rotatedBitmap = null;
                            switch (orientation) {
                                case ExifInterface.ORIENTATION_ROTATE_90 : {
                                    rotatedBitmap = rotateImage(bitmap, 90);
                                    break;
                                }
                                case ExifInterface.ORIENTATION_ROTATE_180 : {
                                    rotatedBitmap = rotateImage(bitmap, 180);
                                    break;
                                }
                                case ExifInterface.ORIENTATION_ROTATE_270 : {
                                    rotatedBitmap = rotateImage(bitmap, 270);
                                    break;
                                }
                                case ExifInterface.ORIENTATION_NORMAL :
                                    default:
                                        rotatedBitmap = bitmap;
                            }

                            // 이미지뷰에 사진 띄우기
                            imageView.setImageBitmap(rotatedBitmap);
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 사진 돌려주는 함수
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    //이미지 서버로 전송하는 함수
    public void uploadImage() {
        //절대 경로 가져오기..왜안대 시벌!

        //String image_path = getRealPathFromURI(uri);

        File imageFile = new File(mCurrentPhotoPath);


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(DjangoApi.DJANGO_SITE)
                .addConverterFactory(GsonConverterFactory.create())
                .build();


        DjangoApi postApi = retrofit.create(DjangoApi.class);


        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/data"), imageFile);
        MultipartBody.Part multiPartBody = MultipartBody.Part
                .createFormData("model_pic", imageFile.getName(), requestBody);


        Call<RequestBody> call = postApi.uploadFile(multiPartBody);

        call.enqueue(new Callback<RequestBody>() {
            @Override
            public void onResponse(Call<RequestBody> call, Response<RequestBody> response) {
                Log.d("good", "good");

            }

            @Override
            public void onFailure(Call<RequestBody> call, Throwable t) {
                Log.d("fail", "fail");
            }
        });

    }





}