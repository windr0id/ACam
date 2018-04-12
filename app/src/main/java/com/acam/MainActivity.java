package com.acam;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    //requestCode
    private static final int REQUEST_IMAGE_CAPTURE_THUMB = 1;
    private static final int REQUEST_IMAGE_CAPTURE_FULL = 2;
    private static final int REQUEST_IMAGE_PICK=3;


    private ImageButton b_camera;
    private ImageButton b_select;
    private ImageButton b_tools;
    private ImageView mImageView;
    private File photoFile;

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    private static final String CAMERA_DIR = "/dcim/";
    private static final String albumName ="ACam";

    private int targetW ;
    private int targetH ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        b_camera = findViewById(R.id.imageButton_camera);
        b_select = findViewById(R.id.imageButton_select);
        b_tools = findViewById(R.id.imageButton_tools);
        mImageView = findViewById(R.id.imageView);



        b_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //CAPTURE_THUMB
                //startActivityForResult(takePictureIntent,REQUEST_IMAGE_CAPTURE_THUMB);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getApplicationContext(),"com.ACam.fileProvider", photoFile));
                startActivityForResult(takePictureIntent,REQUEST_IMAGE_CAPTURE_FULL);
            }
        });
        b_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectPictureIntent = new Intent(Intent.ACTION_PICK);
                selectPictureIntent.setType("image/");
                startActivityForResult(selectPictureIntent, REQUEST_IMAGE_PICK);
            }
        });
        b_tools.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!checkImageExist()) return;
                View contentView= LayoutInflater.from(getApplicationContext()).inflate(R.layout.popwindow_tools, null, false);
                PopupWindow window = new PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                window.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                window.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
                window.setOutsideTouchable(true);
                window.setTouchable(true);
                window.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        WindowManager.LayoutParams lp=getWindow().getAttributes();
                        lp.alpha = 1.0f;
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                        getWindow().setAttributes(lp);
                    }
                });

                float scale=getBaseContext().getResources().getDisplayMetrics().density;
                final int mImage_layout_margin = (int)(12*scale+0.5f);
                window.showAsDropDown(mImageView, -mImage_layout_margin, -window.getContentView().getMeasuredHeight()+mImage_layout_margin);
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.alpha = 0.5f;
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                getWindow().setAttributes(lp);
            }
        });
        try {
            photoFile = createFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_share:
                if(!checkImageExist()) break;
                storeFile();
                Intent share_intent = new Intent();
                share_intent.setAction(Intent.ACTION_SEND);//设置分享行为
                share_intent.setType("image/*");//设置分享内容的类型
                share_intent.putExtra(Intent.EXTRA_SUBJECT, "from ACam");//添加分享内容标题
                share_intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getApplicationContext(),"com.ACam.fileProvider", photoFile));//添加分享内容
                //创建分享的Dialog
                share_intent = Intent.createChooser(share_intent, "分享");
                startActivity(share_intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkImageExist(){
        if(mImageView.getDrawable() == null){
            Toast.makeText(this, "还没有图片呢，快选一张吧", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            return true;
        }
    }

    //获得文件路径
    private File getPhotoDir(){
        File storDirPrivate = null;
        File storDirPublic = null;

        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
            //private,只有本应用可访问
            storDirPrivate = new File (
                    Environment.getExternalStorageDirectory()
                            + CAMERA_DIR
                            + albumName
            );
            //public 所有应用均可访问
            storDirPublic = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    albumName);
            if (! storDirPublic.mkdirs()) {
                if (! storDirPublic.exists()){
                    Log.d("ACam", "failed to create directory");
                    return null;
                }
            }

        }else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storDirPrivate;
    }


    private File createFile() throws IOException {
        photoFile = null;

        String fileName;
        //通过时间戳区别文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
        fileName = JPEG_FILE_PREFIX+timeStamp+"_";

        photoFile = File.createTempFile(fileName,JPEG_FILE_SUFFIX,getPhotoDir());

        return photoFile;
    }

    private void storeFile(){
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(photoFile);
            Bitmap bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_IMAGE_CAPTURE_THUMB:
                if(resultCode == RESULT_OK){
                    Bundle extras = data.getExtras();
                    if(extras == null) break;
                    Bitmap imageBitmap = (Bitmap)extras.get("data") ;
                    mImageView.setImageBitmap(imageBitmap);
                }
                break;
            case REQUEST_IMAGE_CAPTURE_FULL:
                if(resultCode == RESULT_OK){
                    //获得图像的尺寸
                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                    bmOptions.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(photoFile.getAbsolutePath(),bmOptions);

                    int photoW = bmOptions.outWidth;
                    int photoH =bmOptions.outHeight;

                    //计算缩放
                    int scaleFactor = 1;
                    if((targetW>0)||(targetH>0)){
                        scaleFactor = Math.min(photoW/targetW,photoH/targetH);
                    }

                    //将保存的文件解码
                    bmOptions.inJustDecodeBounds = false;
                    bmOptions.inSampleSize = scaleFactor;
                    bmOptions.inPurgeable = true;

                    Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath(), bmOptions);

                    mImageView.setImageBitmap(bitmap);
                }
                break;
            case REQUEST_IMAGE_PICK:
                if(resultCode == RESULT_OK){
                    Uri uri = data.getData();
                    if(uri == null) break;
                    Bitmap bit = null;
                    try {
                        bit = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    mImageView.setImageBitmap(bit);
                }
                break;
        }

    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
