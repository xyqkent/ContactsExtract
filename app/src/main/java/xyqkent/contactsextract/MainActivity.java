package xyqkent.contactsextract;

import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.annotation.BoolRes;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    String AppPath;
    Context mContext;
    public static Handler mHandler;
    Thread mThread;
    ProgressDialog myDialog = null;
    String FileName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        AppPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "ContactsExtract";
        File AppDir = new File(AppPath);
        if (!AppDir.exists()) AppDir.mkdirs();//确保所有主要文件夹都存在

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton input = (FloatingActionButton) findViewById(R.id.input);
        input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mThread = new Thread(ExtractRunnable);
                mThread.start();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        //调用刷新LV
                        int sum = (int) msg.obj;
                        processThread(sum);
                        break;
                    case 2:
                        //回调process进度的值
                        myDialog.incrementProgressBy(1);
                        break;
                    case 3:
                        if (myDialog != null) {
                            myDialog.dismiss();
                        }
                        break;
                    case 4://特殊情况终止
                        if (myDialog != null) {
                            myDialog.dismiss();
                            deleteFile(AppPath + File.separator + FileName);
                        }
                        break;
                }
            }
        };
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private Runnable ExtractRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
                String t = format.format(new Date());
                FileName = "Contacts" + t + ".vcf";
                String SavePath = AppPath + File.separator + FileName;

                ContentResolver cr = getContentResolver();
                Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

                Message.obtain(mHandler, 1, cur.getCount()).sendToTarget();

                int index = cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                FileOutputStream fout = null;
                fout = new FileOutputStream(SavePath);
                byte[] data = new byte[1024 * 1];
                while (cur.moveToNext()) {
                    String lookupKey = cur.getString(index);
                    Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
                    AssetFileDescriptor fd = mContext.getContentResolver().openAssetFileDescriptor(uri, "r");
                    FileInputStream fin = fd.createInputStream();
                    int len = -1;
                    while ((len = fin.read(data)) != -1) {
                        fout.write(data, 0, len);
                    }
                    fin.close();
                    Message.obtain(mHandler, 2).sendToTarget();
                }
                fout.close();

                Log.v("check", "save complete");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Message.obtain(mHandler, 4).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
                Message.obtain(mHandler, 4).sendToTarget();
            }
            Message.obtain(mHandler, 3).sendToTarget();
        }
    };

    public static String qpDecoding(String str) {
        if (str == null) {
            return "";
        }
        try {
            str = str.replaceAll("=\n", "");
            byte[] bytes = str.getBytes("US-ASCII");
            for (int i = 0; i < bytes.length; i++) {
                byte b = bytes[i];
                if (b != 95) {
                    bytes[i] = b;
                } else {
                    bytes[i] = 32;
                }
            }
            if (bytes == null) {
                return "";
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            for (int i = 0; i < bytes.length; i++) {
                int b = bytes[i];
                if (b == '=') {
                    try {
                        int u = Character.digit((char) bytes[++i], 16);
                        int l = Character.digit((char) bytes[++i], 16);
                        if (u == -1 || l == -1) {
                            continue;
                        }
                        buffer.write((char) ((u << 4) + l));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                } else {
                    buffer.write(b);
                }
            }
            return new String(buffer.toByteArray(), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public void restore(Context context, String filename) {
        Intent intent = new Intent();
        intent.setPackage("com.android.contacts");
        Uri uri = Uri.fromFile(new File(filename));
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/x-vcard");
        context.startActivity(intent);
    }

    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    // 构建一个下载进度条
    @SuppressWarnings("deprecation")
    private void processThread(int sum) {
        // 创建ProgressDialog对象
        myDialog = new ProgressDialog(mContext);
        // 设置进度条风格，风格为长形
        myDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        // 设置ProgressDialog 标题
        myDialog.setTitle("保存中");
        // 设置ProgressDialog 进度条最大值（获取图片值）
        myDialog.setMax(sum);
        // 设置ProgressDialog 的进度条是否不明确
        myDialog.setIndeterminate(false);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE,"取消", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                myDialog.dismiss();
                deleteFile(AppPath + File.separator + FileName);
                Log.i("check","用户取消");
            }
        });
        // 设置ProgressDialog 是否可以按退回按键取消
        myDialog.setCancelable(false);
        //设置点击进度对话框外的区域对话框不消失
        myDialog.setCanceledOnTouchOutside(false);
        // 让ProgressDialog显示
        myDialog.show();
    }
}
