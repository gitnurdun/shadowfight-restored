package local.shadowfight.restore;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.*;
import java.security.MessageDigest;
import android.content.SharedPreferences;
import org.json.*;

/** Installs bundled offline content before starting the preserved Unity activity. */
public final class RestoreBootstrap extends Activity {
    TextView status; ProgressBar progress;
    public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setFlags(1024,1024);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);layout.setGravity(Gravity.CENTER);layout.setPadding(70,40,70,40);layout.setBackgroundColor(Color.rgb(36,26,17));
        status=new TextView(this);status.setTextColor(Color.rgb(234,211,165));status.setTextSize(27);status.setGravity(Gravity.CENTER);status.setText("Preparing your game…");layout.addView(status);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);layout.addView(progress,new LinearLayout.LayoutParams(-1,20));setContentView(layout);
        new Thread(new Runnable(){public void run(){try{prepare();runOnUiThread(new Runnable(){public void run(){Intent game=new Intent();game.setClassName(getPackageName(),"com.nekki.utils.activity.FCMNekkiUnityPlayerActivity");startActivity(game);finish();}});}
        catch(Exception e){android.util.Log.e("ShadowRestore","Preparation failed",e);runOnUiThread(new Runnable(){public void run(){status.setText("Preparation could not finish. Free some storage and reopen the game.");}});}}},"offline-content").start();
    }
    byte[] read(InputStream in)throws Exception{try(InputStream source=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[65536];int n;while((n=source.read(buf))!=-1)out.write(buf,0,n);return out.toByteArray();}}
    void prepare()throws Exception{
        File root=getExternalFilesDir(null);if(root==null)throw new IOException("Storage unavailable");
        JSONArray files=new JSONArray(new String(read(getAssets().open("revival/index.json")),"UTF-8"));
        SharedPreferences receipts=getSharedPreferences("offline_content",MODE_PRIVATE);
        for(int i=0;i<files.length();i++){
            JSONObject item=files.getJSONObject(i);String relative=item.getString("path");File target=new File(root,relative);
            if(!target.getCanonicalPath().startsWith(root.getCanonicalPath()+File.separator))throw new IOException("Invalid content path");
            if(item.optBoolean("seed",false)&&target.exists())continue;
            String expected=item.optString("sha256","");
            if(!target.exists()||target.length()!=item.getLong("bytes")||!expected.equals(receipts.getString(relative,""))){
                File parent=target.getParentFile();if(!parent.exists()&&!parent.mkdirs())throw new IOException("Cannot create content directory");
                File temporary=new File(parent,target.getName()+".installing");
                MessageDigest digest=MessageDigest.getInstance("SHA-256");
                try(InputStream in=getAssets().open("revival/"+relative);OutputStream out=new FileOutputStream(temporary)){byte[] buf=new byte[262144];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);digest.update(buf,0,n);}}
                if(temporary.length()!=item.getLong("bytes"))throw new IOException("Incomplete content copy");
                StringBuilder hex=new StringBuilder();for(byte b:digest.digest()){hex.append(Character.forDigit((b>>4)&15,16));hex.append(Character.forDigit(b&15,16));}
                if(!expected.isEmpty()&&!expected.equals(hex.toString()))throw new IOException("Content hash mismatch");
                if(target.exists()&&!target.delete())throw new IOException("Cannot replace content file");
                if(!temporary.renameTo(target))throw new IOException("Cannot finish content copy");
                receipts.edit().putString(relative,expected).apply();
            }
            final int percent=(i+1)*100/files.length();runOnUiThread(new Runnable(){public void run(){progress.setProgress(percent);}});
        }
    }
}
