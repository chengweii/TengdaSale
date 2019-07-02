package com.example.xch.scanzbar;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import com.alibaba.fastjson.JSON;
import com.example.xch.scanzbar.zbar.CaptureActivity;
import com.example.xch.scanzbar.zbar.MakeQRCodeUtil;
import com.xuexiang.xqrcode.XQRCode;
import com.xuexiang.xui.XUI;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_scan;
    private TextView tv_scanResult;
    private NumberPicker num_input;
    private Button btn_scan2;
    private Button btn_scan3;
    private Button btn_scan4;
    private static final int REQUEST_CODE_SCAN = 0x0000;// 扫描二维码

    //声明一个AlertDialog构造器
    private AlertDialog.Builder builder;

    private String[] numbers = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "10+"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();
        setContentView(R.layout.activity_main);
        btn_scan = (Button) findViewById(R.id.btn_scan);
        tv_scanResult = (TextView) findViewById(R.id.tv_scanResult);
        btn_scan.setOnClickListener(this);

        btn_scan2 = (Button) findViewById(R.id.btn_scan2);
        btn_scan2.setOnClickListener(this);

        btn_scan3 = (Button) findViewById(R.id.btn_scan3);
        btn_scan3.setOnClickListener(this);

        btn_scan4 = (Button) findViewById(R.id.btn_scan4);
        btn_scan4.setOnClickListener(this);

        num_input = (NumberPicker) findViewById(R.id.num_input);
        //设置需要显示的内容数组
        //num_input.setDisplayedValues(numbers);
        //设置最大最小值
        num_input.setMinValue(1);
        num_input.setMaxValue(100);
        //设置默认的位置
        num_input.setValue(1);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA,Manifest.permission.INTERNET,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},
                1);
    }

    /**
     * 初始化XUI 框架
     */
    private void initUI() {
        XUI.init(this.getApplication()); //初始化UI框架
        XUI.debug(true);  //开启UI框架调试日志
        XUI.initTheme(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                //动态权限申请
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
                } else {
                    goScan();
                }
                break;
            case R.id.btn_scan2:
                request("入");
                break;
            case R.id.btn_scan3:
                request("出");
                generateQrcode();
                break;
            case R.id.btn_scan4:
                generateQrcode();
                break;
            default:
                break;
        }
    }

    private void generateQrcode() {
        try {
            takePhoto();
        } catch (Throwable t) {
            printException(t);
        }
    }

    private Uri imageUri;
    public static final int TAKE_PHOTO = 0x0003;
    /**
     *拍照获取图片
     **/
    public void takePhoto() {
        String status= Environment.getExternalStorageState();
        if(status.equals(Environment.MEDIA_MOUNTED)) {
            //创建File对象，用于存储拍照后的图片
            File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
            try {
                if (outputImage.exists()) {
                    outputImage.delete();
                }
                outputImage.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (Build.VERSION.SDK_INT >= 24) {
                imageUri = FileProvider.getUriForFile(this, "com.example.xch.scanzbar.fileProvider", outputImage);
            } else {
                imageUri = Uri.fromFile(outputImage);
            }
            //启动相机程序
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, TAKE_PHOTO);
        }else
        {
            Toast.makeText(MainActivity.this, "没有储存卡",Toast.LENGTH_LONG).show();
        }
    }

    private void request(final String type) {
        try {
            new AnsyTry("https://used-api.jd.com/auction/detail?auctionId=114830306", (response) -> {
                if (response != null) {
                    try {
                        Detail detail = JSON.parseObject(response, Detail.class);
                        String result = "";
                        if (detail != null && detail.data != null) {
                            result = detail.data.freightAreaText;
                        }
                        showMsg(String.format("%s %s,%s个,响应1：%s", type, tv_scanResult.getText(), num_input.getValue(), result));
                    } catch (Throwable t) {
                        showMsg("不好疑似出问题了，" + t.getMessage());
                    }
                }
            }).execute(tv_scanResult.getText().toString(), String.valueOf(num_input.getValue()));
        } catch (Throwable t) {
            showMsg("不好疑似出问题了，手动弄吧：" + t.getMessage());
        }
    }

    private void printException(Throwable e){
        if (e == null){
            return;
        }
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String msg =  stringWriter.toString();
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    public String saveImageToGallery(Bitmap bmp) {
        //生成路径
        File appDir = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera/");

        //文件名为时间
        long timeStamp = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("配件-yyyyMMddHHmmss");
        String sd = sdf.format(new Date(timeStamp));
        String fileName = sd + ".jpg";

        //获取文件
        File file = new File(appDir, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = Uri.fromFile(file);
            intent.setData(uri);
            this.getApplication().sendBroadcast(intent);

            return fileName;
        } catch (FileNotFoundException e) {
            printException(e);
        } catch (IOException e) {
            printException(e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                printException(e);
            }
        }
        return null;
    }

    public static class Detail {
        private Data data;

        public Data getData() {
            return data;
        }

        public void setData(Data data) {
            this.data = data;
        }

        public static class Data {
            private String freightAreaText;

            public String getFreightAreaText() {
                return freightAreaText;
            }

            public void setFreightAreaText(String freightAreaText) {
                this.freightAreaText = freightAreaText;
            }
        }
    }

    private final OkHttpClient client = new OkHttpClient();

    public interface HttpCallback {
        void execute(String response);
    }

    private class AnsyTry extends AsyncTask<String, Integer, String> {
        private String url;
        private HttpCallback httpCallback;

        public AnsyTry(String url, HttpCallback httpCallback) {
            this.httpCallback = httpCallback;
            this.url = url;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    return response.body().string();
                }
            } catch (IOException e) {
                return e.getMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (httpCallback != null) {
                httpCallback.execute(result);
            }
            //finish();
        }
    }

    private void showMsg(String msg) {
        builder = new AlertDialog.Builder(this);
        builder.setTitle("标题");
        builder.setMessage(msg);
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 跳转到扫码界面扫码
     */
    private void goScan() {
        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SCAN);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    goScan();
                } else {
                    Toast.makeText(this, "你拒绝了权限申请，可能无法打开相机扫码哟！", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SCAN:// 二维码
                // 扫描二维码回传
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        //获取扫描结果
                        Bundle bundle = data.getExtras();
                        String result = bundle.getString(CaptureActivity.EXTRA_STRING);
                        tv_scanResult.setText(result);
                    }
                }
                break;
            case TAKE_PHOTO :
                if (resultCode == RESULT_OK) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        Bitmap qrcode = XQRCode.createQRCodeWithLogo("远大阀门208##305",bitmap);
                        Bitmap finalCode =MakeQRCodeUtil.composeWatermark(bitmap,qrcode);
                        saveImageToGallery(finalCode);
                        Toast.makeText(this, "图片二维码已保存到相册", Toast.LENGTH_LONG).show();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }
}
