package com.vilmar.proxbrowser;
import java.io.*; import java.net.*; import java.util.*;
public class FreeProxyApi {
    public interface Callback { void onResult(List<String[]> list); }
    public static void fetch(String country, Callback cb){
        new Thread(()->{
            List<String[]> out=new ArrayList<>();
            try{
                URL u=new URL("https://api.proxyscrape.com/v2/?request=displayproxies&protocol=http&timeout=10000&country="+country.toUpperCase()+"&ssl=all&anonymity=all");
                BufferedReader r=new BufferedReader(new InputStreamReader(u.openStream()));
                String line; while((line=r.readLine())!=null && out.size()<30){ String[] p=line.trim().split(":"); if(p.length==2) out.add(p); }
                r.close();
            }catch(Exception ignored){}
            cb.onResult(out);
        }).start();
    }
}
