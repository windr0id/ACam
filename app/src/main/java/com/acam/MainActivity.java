package com.acam;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.wang.avi.AVLoadingIndicatorView;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

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

    AVLoadingIndicatorView avi;

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

        ImageButton b_camera = findViewById(R.id.imageButton_camera);
        ImageButton b_select = findViewById(R.id.imageButton_select);
        ImageButton b_tools = findViewById(R.id.imageButton_tools);
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
                showToolsWindow();
                //showLoadingWindow();
            }
        });
        try {
            photoFile = createFile();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private final int MSG_IMAGE = 0;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case MSG_IMAGE:
                    Bitmap bitmap =  msg.getData().getParcelable("image");
                    mImageView.setImageBitmap(bitmap);
                    loadingWindow.dismiss();
                    break;
            }

            return false;
        }
    });

    private PopupWindow toolsWindow;
    private void showToolsWindow(){
        if(!checkImageExist()) return;
        View contentView= LayoutInflater.from(getApplicationContext()).inflate(R.layout.popwindow_tools, null, false);
        final PopupWindow window = new PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        toolsWindow = window;
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

        contentView.findViewById(R.id.button_tools_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                getEdge(bitmap);
                mImageView.setImageBitmap(bitmap);
                hideLoadingWindow();
            }
        });

        contentView.findViewById(R.id.button_style_0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Bitmap bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap retBitmap = stylizeImage(bitmap, 0);
                        Message msg = new Message();
                        msg.what = MSG_IMAGE;
                        Bundle bd = new Bundle();
                        bd.putParcelable("image", retBitmap);
                        msg.setData(bd);
                        mHandler.sendMessage(msg);
                    }
                }).start();
                hideToolsWindow();
                showLoadingWindow();
            }
        });
        contentView.findViewById(R.id.button_style_19).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                bitmap = stylizeImage(bitmap, 19);
                mImageView.setImageBitmap(bitmap);
                window.dismiss();
            }
        });
        contentView.findViewById(R.id.button_style_24).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
                bitmap = stylizeImage(bitmap, 24);
                mImageView.setImageBitmap(bitmap);
                window.dismiss();
            }
        });
    }

    private void hideToolsWindow(){
        if(toolsWindow != null){
            toolsWindow.dismiss();
        }
    }

    private PopupWindow loadingWindow;
    private boolean isLoading = false;

    private void showLoadingWindow(){
        isLoading = true;
        View contentView= LayoutInflater.from(getApplicationContext()).inflate(R.layout.popwindow_loading, null, false);
        contentView.setFocusable(true); // 这个很重要
        contentView.setFocusableInTouchMode(true);
        final PopupWindow window = new PopupWindow(contentView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        loadingWindow = window;
        window.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        window.setTouchable(false);
        window.setFocusable(false);
        window.getBackground().setAlpha(160);
        window.showAtLocation(mImageView, Gravity.CENTER, 0, 0);
        contentView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (isLoading && keyCode == KeyEvent.KEYCODE_BACK) {
                    return true;
                }
                return false;
            }
        });
    }
    private void hideLoadingWindow(){
        if(loadingWindow != null){
            isLoading = false;
            loadingWindow.dismiss();
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

    private final float[] styleVals = new float[NUM_STYLES];
    private int[] intValues;
    private float[] floatValues;
    private TensorFlowInferenceInterface inferenceInterface;
    private static final String MODEL_FILE = "file:///android_asset/stylize_quantized.pb";
    private static final String INPUT_NODE = "input";
    private static final String STYLE_NODE = "style_num";
    private static final String OUTPUT_NODE = "transformer/expand/conv3/conv/Sigmoid";
    private static final int NUM_STYLES = 26;

    private Bitmap stylizeImage(Bitmap bitmap, int style) {
        int desiredSize = 1024;
        intValues = new int[desiredSize * desiredSize];
        floatValues = new float[desiredSize * desiredSize * 3];
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = desiredSize;
        int newHeight = desiredSize;
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        
        croppedBitmap.getPixels(intValues, 0, croppedBitmap.getWidth(), 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
            floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
        }

        for(int i=0; i<NUM_STYLES; i++){
            styleVals[i] = style == i ? 1.0f : 0.0f;
        }
        inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);
        // Copy the input data into TensorFlow.
        inferenceInterface.feed(
                INPUT_NODE, floatValues, 1, croppedBitmap.getWidth(), croppedBitmap.getHeight(), 3);
        inferenceInterface.feed(STYLE_NODE, styleVals, NUM_STYLES);

        final boolean isDebug = false;
        inferenceInterface.run(new String[] {OUTPUT_NODE}, isDebug);
        inferenceInterface.fetch(OUTPUT_NODE, floatValues);

        for (int i = 0; i < intValues.length; ++i) {
            intValues[i] =
                    0xFF000000
                            | (((int) (floatValues[i * 3] * 255)) << 16)
                            | (((int) (floatValues[i * 3 + 1] * 255)) << 8)
                            | ((int) (floatValues[i * 3 + 2] * 255));
        }
        croppedBitmap.setPixels(intValues, 0, croppedBitmap.getWidth(), 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight());
        return croppedBitmap;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    native void getEdge(Object bitmap);

}
