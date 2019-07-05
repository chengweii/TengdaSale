package com.example.xch.scanzbar;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.alibaba.fastjson.JSON;
import com.example.xch.scanzbar.zbar.CaptureActivity;
import com.example.xch.scanzbar.zbar.MakeQRCodeUtil;
import com.xuexiang.xqrcode.XQRCode;
import com.xuexiang.xui.XUI;
import com.xuexiang.xui.widget.popupwindow.bar.CookieBar;
import com.xuexiang.xui.widget.toast.XToast;
import okhttp3.*;

import java.io.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_scan;
    private TextView partsCode;
    private EditText partsName;
    private NumberPicker partsNum;
    private EditText partsPrice;
    private Button btn_scan2;
    private Button btn_scan3;
    private Button btn_scan4;
    private static final int REQUEST_CODE_SCAN = 0x0000;// 扫描二维码

    private String[] numbers = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "10+"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();
        setContentView(R.layout.activity_main);
        btn_scan = (Button) findViewById(R.id.btn_scan);
        partsCode = (TextView) findViewById(R.id.partsCode);
        btn_scan.setOnClickListener(this);

        btn_scan2 = (Button) findViewById(R.id.btn_scan2);
        btn_scan2.setOnClickListener(this);

        btn_scan3 = (Button) findViewById(R.id.btn_scan3);
        btn_scan3.setOnClickListener(this);

        btn_scan4 = (Button) findViewById(R.id.btn_scan4);
        btn_scan4.setOnClickListener(this);

        partsName = (EditText) findViewById(R.id.partsName);

        partsPrice = (EditText) findViewById(R.id.partsPrice);

        partsNum = (NumberPicker) findViewById(R.id.partsNum);
        //设置需要显示的内容数组
        //num_input.setDisplayedValues(numbers);
        //设置最大最小值
        partsNum.setMinValue(1);
        partsNum.setMaxValue(100);
        //设置默认的位置
        partsNum.setValue(1);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
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
                request(RequestType.IN);
                break;
            case R.id.btn_scan3:
                request(RequestType.OUT);
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
            String partsNameValue = partsName.getText().toString();
            String partsPriceValue = partsPrice.getText().toString();
            if (partsNameValue == null || "".equals(partsNameValue.trim())) {
                showWarnAlert("请先扫描配件信息", "请输入配件名称");
                return;
            }
            if (partsPriceValue == null || "".equals(partsPriceValue.trim())) {
                showWarnAlert("请先扫描配件信息", "请输入配件价格");
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String sd = sdf.format(new Date());
            partsCode.setText(sd);

            takePhoto();
        } catch (Throwable t) {
            printException(t);
        }
    }

    private Uri imageUri;
    public static final int TAKE_PHOTO = 0x0003;

    /**
     * 拍照获取图片
     **/
    public void takePhoto() {
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
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
        } else {
            Toast.makeText(MainActivity.this, "没有储存卡", Toast.LENGTH_LONG).show();
        }
    }

    public static class AddRecordParams {
        private byte type;
        private String partsCode;
        private String partsName;
        private int partsNum;
        private BigDecimal partsPrice;

        public byte getType() {
            return type;
        }

        public void setType(byte type) {
            this.type = type;
        }

        public String getPartsCode() {
            return partsCode;
        }

        public void setPartsCode(String partsCode) {
            this.partsCode = partsCode;
        }

        public String getPartsName() {
            return partsName;
        }

        public void setPartsName(String partsName) {
            this.partsName = partsName;
        }

        public int getPartsNum() {
            return partsNum;
        }

        public void setPartsNum(int partsNum) {
            this.partsNum = partsNum;
        }

        public BigDecimal getPartsPrice() {
            return partsPrice;
        }

        public void setPartsPrice(BigDecimal partsPrice) {
            this.partsPrice = partsPrice;
        }
    }

    private void showSuccessAlert(String title,String msg){
        CookieBar.builder(MainActivity.this)
                .setTitle(title)
                .setMessage(msg)
                .setDuration(3000)
                .setBackgroundColor(R.color.xui_config_color_blue)
                .setLayoutGravity(Gravity.TOP)
                .show();
    }

    private void showErrorAlert(String title,String msg){
        CookieBar.builder(MainActivity.this)
                .setTitle(title)
                .setMessage(msg)
                .setDuration(10000)
                .setBackgroundColor(R.color.xui_config_color_red)
                .setLayoutGravity(Gravity.TOP)
                .show();
    }

    private void showWarnAlert(String title,String msg){
        CookieBar.builder(MainActivity.this)
                .setTitle(title)
                .setMessage(msg)
                .setDuration(3000)
                .setBackgroundColor(R.color.xui_config_color_red)
                .setLayoutGravity(Gravity.TOP)
                .show();
    }

    private void request(final RequestType type) {
        String partsCodeValue = partsCode.getText().toString();
        String partsNameValue = partsName.getText().toString();
        String partsPriceValue = partsPrice.getText().toString();
        if (partsCodeValue == null || "".equals(partsCodeValue.trim())) {
            showWarnAlert("请先扫描配件信息", "请输入配件条码");
            return;
        }
        if (partsNameValue == null || "".equals(partsNameValue.trim())) {
            showWarnAlert("请先扫描配件信息", "请输入配件名称");
            return;
        }
        if (partsPriceValue == null || "".equals(partsPriceValue.trim())) {
            showWarnAlert("请先扫描配件信息", "请输入配件价格");
            return;
        }

        String opreateType = RequestType.IN == type ? "进货" : "出货";
        byte opType = RequestType.IN == type ? (byte) 1 : (byte) 2;

        BigDecimal amount = BigDecimal.valueOf(partsNum.getValue()).multiply(BigDecimal.valueOf(Double.parseDouble(partsPriceValue)));
        String msg = String.format("【%s】【%s】【%s件】完成，总金额【%s元】。", opreateType, partsNameValue, partsNum.getValue(), amount);
        String confirmMsg = String.format("确认【%s】【%s】【%s件】，总金额【%s元】？", opreateType, partsNameValue, partsNum.getValue(), amount);
        showConfirmMsg(confirmMsg, "", (data) -> {
            try {
                AddRecordParams addRecordParams = new AddRecordParams();
                addRecordParams.setPartsCode(partsCodeValue);
                addRecordParams.setPartsName(partsNameValue);
                addRecordParams.setPartsNum(partsNum.getValue());
                addRecordParams.setPartsPrice(BigDecimal.valueOf(Double.parseDouble(partsPriceValue)));
                addRecordParams.setType(opType);

                new AnsyTry(HOST + "sale_record/add", JSON.toJSONString(addRecordParams), (response) -> {
                    if (response != null) {
                        try {
                            if (response != null) {
                                JsonResult detail = JSON.parseObject(response, JsonResult.class);
                                if (detail != null && detail.code == 200) {
                                    showSuccessAlert("保存成功",msg);
                                } else {
                                    showErrorAlert("保存失败",detail.msg);
                                }
                            }
                        } catch (Throwable t) {
                            showErrorAlert("操作失败，请先手动记账，然后稍后再试。", "操作失败，请先手动记账，然后稍后再试。操作类型：" + opreateType);
                        }
                    }
                }).execute();
            } catch (Throwable t) {
                showErrorAlert("操作失败，请先手动记账，然后稍后再试。", "操作失败，请先手动记账，然后稍后再试。操作类型：" + opreateType);
            }
        });
    }

    public enum RequestType {
        /**
         *
         */
        IN, OUT;
    }

    private void printException(Throwable e) {
        if (e == null) {
            return;
        }
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String msg = stringWriter.toString();
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

    public interface Callback {
        void execute(String data);
    }

    private class AnsyTry extends AsyncTask<String, Integer, String> {
        private String url;
        private String paramJson;
        private Callback httpCallback;

        public AnsyTry(String url, String paramJson, Callback httpCallback) {
            this.httpCallback = httpCallback;
            this.url = url;
            this.paramJson = paramJson;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                MediaType json = MediaType.parse("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(json, paramJson);
                Request request = new Request.Builder().url(url).post(body).build();
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

    private AlertDialog.Builder alertBuilder;

    private void showMsg(String title, String msg) {
        alertBuilder = new AlertDialog.Builder(this).setTitle(title).setMessage(msg).setCancelable(true);
        alertBuilder.create().show();
    }

    private AlertDialog.Builder confirmBuilder;

    private void showConfirmMsg(String title, String msg, Callback clickCallback) {
        confirmBuilder = new AlertDialog.Builder(this).setTitle(title).setMessage(msg).setCancelable(true).setPositiveButton("不对", (dialog, which) -> {
        }).setNegativeButton("好的", (dialog, which) -> {
            clickCallback.execute(null);
        });
        confirmBuilder.create().show();
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

    private static final String HOST = "http://10.0.6.96:8080/";

    public static class JsonResult {
        private int code;
        private String msg;
        private Object result;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
        }
    }

    public static class AddParams {
        private String partsCode;
        private String partsName;
        private BigDecimal currentPrice;

        public String getPartsCode() {
            return partsCode;
        }

        public void setPartsCode(String partsCode) {
            this.partsCode = partsCode;
        }

        public String getPartsName() {
            return partsName;
        }

        public void setPartsName(String partsName) {
            this.partsName = partsName;
        }

        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
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
                        try {
                            if (!result.matches("^.+##.+##.+$")) {
                                throw new RuntimeException("配件二维码内容错误");
                            }
                            String[] contents = result.split("##");
                            partsCode.setText(contents[0]);
                            partsName.setText(contents[1]);
                            partsPrice.setText(contents[2]);
                        } catch (Throwable t) {
                            showErrorAlert("扫描二维码异常","配件二维码内容错误：" + result);
                        }
                    }
                }
                break;
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        String partsCodeValue = partsCode.getText().toString();
                        String partsNameValue = partsName.getText().toString();
                        String partsPriceValue = partsPrice.getText().toString();

                        String codeValue = partsCodeValue + "##" + partsNameValue + "##" + partsPriceValue;

                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        Bitmap qrcode = XQRCode.createQRCodeWithLogo(codeValue, bitmap);
                        Bitmap finalCode = MakeQRCodeUtil.composeWatermark(bitmap, qrcode);


                        String confirmMsg = String.format("确认新增配件【%s】，售价【%s元】？", partsNameValue, partsPriceValue);
                        showConfirmMsg(confirmMsg, "", (data1) -> {
                            saveImageToGallery(finalCode);
                            showSuccessAlert("配件二维码已保存到相册", "请到相册打印纸质二维码后续使用");

                            try {
                                AddParams addParams = new AddParams();
                                addParams.setPartsCode(partsCodeValue);
                                addParams.setPartsName(partsNameValue);
                                addParams.setCurrentPrice(BigDecimal.valueOf(Double.parseDouble(partsPriceValue)));
                                new AnsyTry(HOST + "sale_parts/add", JSON.toJSONString(addParams), (response) -> {
                                    if (response != null) {
                                        try {
                                            JsonResult detail = JSON.parseObject(response, JsonResult.class);
                                            if (detail != null && detail.code == 200) {
                                                showSuccessAlert("保存成功",partsNameValue);
                                            } else {
                                                showErrorAlert("保存失败",detail.msg);
                                            }
                                        } catch (Throwable t) {
                                            showErrorAlert("操作失败，请到PC端新增或稍后再试。", "操作失败，请到PC端新增或稍后再试。");
                                        }
                                    }
                                }).execute();
                            } catch (Throwable t) {
                                showErrorAlert("操作失败，请到PC端新增或稍后再试。", "操作失败，请到PC端新增或稍后再试。");
                            }
                        });
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
