package com.vilmar.proxbrowser;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;
import java.util.concurrent.Executor;
public class ProxyManager {
    public interface Done { void onDone(boolean ok, String msg); }
    static final Executor DIRECT = Runnable::run;
    public static SharedPreferences prefs(Context c){ return c.getSharedPreferences("prox_prefs", Context.MODE_PRIVATE); }
    public static void saveCreds(Context c, String host, int port, String user, String pass){
        prefs(c).edit().putString("host",host).putInt("port",port).putString("user",user).putString("pass",pass).putBoolean("active",true).apply();
    }
    public static void apply(Context ctx, Done d){
        SharedPreferences p=prefs(ctx);
        String host=p.getString("host",""); int port=p.getInt("port",0);
        String user=p.getString("user",""), pass=p.getString("pass","");
        apply(ctx, host, port, user, pass, d);
    }
    public static void apply(Context ctx, String host, int port, String user, String pass, Done d){
        if(host==null||host.isEmpty()||port<=0){ d.onDone(false,"Host/porta invalidos"); return; }
        if(!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)){ d.onDone(false,"Android/WebView nao suporta Proxy Override (precisa Android 10+ e WebView atualizado)"); return; }
        new Thread(()->{
            try{
                // 1) sobe o proxy local que injeta a autenticacao na proxy externa
                LocalProxyServer.get().start(host, port, user, pass);
                // 2) WebView aponta so para o proxy local (sem auth)
                ProxyConfig config=new ProxyConfig.Builder().addProxyRule("127.0.0.1:"+LocalProxyServer.LOCAL_PORT).addDirect().build();
                ProxyController.getInstance().setProxyOverride(config, DIRECT, ()->{
                    saveCreds(ctx,host,port,user,pass);
                    d.onDone(true,"Proxy ativa neste app: "+host+":"+port+" (via motor local)");
                });
            }catch(Exception e){
                d.onDone(false,"Falha ao iniciar motor local: "+e.getMessage());
            }
        }).start();
    }
    public static void clear(Context ctx, Done d){
        LocalProxyServer.get().stop();
        prefs(ctx).edit().putBoolean("active",false).apply();
        if(!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)){ d.onDone(true,"Proxy desligada"); return; }
        ProxyController.getInstance().clearProxyOverride(DIRECT, ()-> d.onDone(true,"Proxy desligada - IP normal"));
    }
}
