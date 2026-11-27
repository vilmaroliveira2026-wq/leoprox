package com.vilmar.proxbrowser;
import android.app.Activity; import android.app.AlertDialog; import android.content.Intent; import android.content.SharedPreferences; import android.graphics.Bitmap; import android.net.Uri; import android.os.Bundle; import android.view.KeyEvent; import android.view.View; import android.view.ViewGroup; import android.view.inputmethod.EditorInfo; import android.webkit.CookieManager; import android.webkit.WebChromeClient; import android.webkit.WebResourceError; import android.webkit.WebResourceRequest; import android.webkit.WebSettings; import android.webkit.WebView; import android.webkit.WebViewClient; import android.widget.Button; import android.widget.EditText; import android.widget.FrameLayout; import android.widget.ProgressBar; import android.widget.TextView; import android.widget.Toast;
import java.net.URLEncoder; import java.util.ArrayList; import java.util.List;
import org.json.JSONArray; import org.json.JSONObject;
public class MainActivity extends Activity {
    static final String UA_MOBILE = "Mozilla/5.0 (Linux; Android 14; SM-A556E Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.6478.122 Mobile Safari/537.36";
    static final String UA_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    static final String HOME_BASE = "https://proxbrowser.home/";
    static final String HOME_HTML = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>body{background:linear-gradient(160deg,#0B0E14,#131A2A);color:#fff;font-family:sans-serif;text-align:center;margin:0;padding:28px 12px}h1{font-size:28px;margin:6px 0 2px}h1 span{color:#FF4A3D}.sub{color:#9AA4B2;font-size:13px;margin-bottom:20px}.grid{max-width:360px;margin:auto}.tile{display:inline-block;width:76px;margin:7px 4px;text-decoration:none;color:#fff;font-size:12px}.ic{width:56px;height:56px;border-radius:17px;margin:0 auto 5px;font-size:25px;line-height:56px;font-weight:bold}.strip{margin:22px auto 0;max-width:340px;background:#171C28;border:1px solid #26304a;border-radius:14px;padding:10px;color:#9AA4B2;font-size:12px}.strip b{color:#4DA3FF}</style></head><body>"
      + "<h1>Prox<span>Browser</span></h1><div class='sub'>Seu navegador com proxy - estilo Brave + Opera</div><div class='grid'>"
      + "<a class='tile' href='https://www.tiktok.com'><div class='ic' style='background:#000;border:1px solid #444'>T</div>TikTok</a>"
      + "<a class='tile' href='https://www.youtube.com'><div class='ic' style='background:#FF0000'>Y</div>YouTube</a>"
      + "<a class='tile' href='https://www.instagram.com'><div class='ic' style='background:linear-gradient(45deg,#FEDA75,#D62976,#962FBF)'>I</div>Instagram</a>"
      + "<a class='tile' href='https://muse.ai'><div class='ic' style='background:#D97757'>M</div>Muse</a>"
      + "<a class='tile' href='https://www.google.com'><div class='ic' style='background:#fff;color:#333'>G</div>Google</a>"
      + "<a class='tile' href='https://www.facebook.com'><div class='ic' style='background:#1877F2'>f</div>Facebook</a>"
      + "<a class='tile' href='https://x.com'><div class='ic' style='background:#000;border:1px solid #444'>X</div>X</a>"
      + "<a class='tile' href='https://web.whatsapp.com'><div class='ic' style='background:#25D366'>W</div>WhatsApp</a>"
      + "</div><div class='strip'>Navegacao pela <b>sua proxy</b> - HTTPS automatico ligado - abas, favoritos e historico no menu</div></body></html>";
    FrameLayout container; EditText urlBox; TextView status; ProgressBar progress; Button tabsBtn, starBtn;
    List<WebView> tabs = new ArrayList<>(); int current = -1; boolean loadingHome = false; SharedPreferences store;
    WebView web(){ return current>=0 ? tabs.get(current) : null; }
    @Override protected void onCreate(Bundle s){ super.onCreate(s); setContentView(R.layout.activity_main);
        store = getSharedPreferences("browser_data", MODE_PRIVATE);
        container=findViewById(R.id.webContainer); urlBox=findViewById(R.id.urlBox); status=findViewById(R.id.statusText); progress=findViewById(R.id.progressBar); tabsBtn=findViewById(R.id.tabsBtn); starBtn=findViewById(R.id.starBtn);

        SharedPreferences pp = ProxyManager.prefs(this);
        if(pp.getBoolean("active",false)) ProxyManager.apply(this, (ok,msg)->{});
        urlBox.setOnEditorActionListener((v,a,e)->{ if(a==EditorInfo.IME_ACTION_GO||(e!=null&&e.getKeyCode()==KeyEvent.KEYCODE_ENTER)){ load(urlBox.getText().toString()); return true; } return false; });
        findViewById(R.id.goBtn).setOnClickListener(v->load(urlBox.getText().toString()));
        findViewById(R.id.backBtn).setOnClickListener(v->{ if(web()!=null&&web().canGoBack()) web().goBack(); });
        findViewById(R.id.fwdBtn).setOnClickListener(v->{ if(web()!=null&&web().canGoForward()) web().goForward(); });
        findViewById(R.id.reloadBtn).setOnClickListener(v->{ if(web()!=null) web().reload(); });
        starBtn.setOnClickListener(v->toggleBookmark());
        tabsBtn.setOnClickListener(v->showTabs());
        findViewById(R.id.menuBtn).setOnClickListener(v->showMenu());
        findViewById(R.id.homeBtn).setOnClickListener(v->showHome());
        findViewById(R.id.favBtn).setOnClickListener(v->showBookmarks());
        findViewById(R.id.histBtn).setOnClickListener(v->showHistory());
        findViewById(R.id.ipBtn).setOnClickListener(v->{ if(web()!=null) web().loadUrl("https://ipinfo.io"); });
        findViewById(R.id.proxyBtn).setOnClickListener(v->startActivity(new Intent(this, ProxyActivity.class)));
        newTab(null);
        String st = pp.getBoolean("active",false) ? ("Proxy ativa neste app: "+pp.getString("host","")+":"+pp.getInt("port",0)) : "Proxy desligada - toque em Proxy";
        status.setText(st);
    }
    WebView newTab(String url){
        final WebView w = new WebView(this);
        w.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        WebSettings ws = w.getSettings();
        ws.setJavaScriptEnabled(true); ws.setDomStorageEnabled(true);
        ws.setUserAgentString(store.getBoolean("desktop_mode",false) ? UA_DESKTOP : UA_MOBILE);
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
        ws.setSupportMultipleWindows(true); ws.setJavaScriptCanOpenWindowsAutomatically(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        ws.setMediaPlaybackRequiresUserGesture(false); ws.setLoadWithOverviewMode(true); ws.setUseWideViewPort(true);
        w.setWebChromeClient(new WebChromeClient(){ @Override public void onProgressChanged(WebView v,int p){ if(v==web()){ progress.setProgress(p); progress.setVisibility(p>=100?View.GONE:View.VISIBLE); } } });
        w.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req){ return handleNav(v, req.getUrl().toString()); }
            @Override public boolean shouldOverrideUrlLoading(WebView v, String u){ return handleNav(v, u); }
            @Override public void onPageStarted(WebView v,String u,Bitmap f){ if(v==web()&&!loadingHome&&u!=null&&u.startsWith("http")) urlBox.setText(u); }
            @Override public void onPageFinished(WebView v,String u){ if(u!=null&&u.startsWith("http")&&!u.startsWith(HOME_BASE)) addHistory(v.getTitle(), u); if(v==web()) updateStar(); }
            @Override public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err){ if(req.isForMainFrame()){ loadingHome=false; String msg="<html><body style='background:#0B0E14;color:#fff;font-family:sans-serif;text-align:center;padding:40px'><h2>Nao foi possivel abrir a pagina</h2><p style='color:#9AA4B2'>"+err.getDescription()+"</p><p><a style='color:#4DA3FF' href='app://home'>Voltar ao inicio</a> &nbsp; <a style='color:#4DA3FF' href='app://ip'>Ver meu IP</a></p></body></html>"; v.loadDataWithBaseURL(HOME_BASE, msg, "text/html", "UTF-8", null); } }
        });
        w.setDownloadListener((u,ua,cd,mime,len)->{ try{ startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); }catch(Exception e){} });
        container.addView(w); tabs.add(w); switchTab(tabs.size()-1);
        if(url==null||url.isEmpty()) showHome(); else w.loadUrl(url);
        return w;
    }
    boolean handleNav(WebView v, String u){
        if(u.startsWith("app://")){ if(u.contains("ip")) v.loadUrl("https://ipinfo.io"); else showHome(); return true; }
        if(u.startsWith("http://")){ v.loadUrl("https://"+u.substring(7)); return true; }
        return false;
    }
    void switchTab(int i){ if(i<0||i>=tabs.size()) return; current=i; for(int k=0;k<tabs.size();k++) tabs.get(k).setVisibility(k==i?View.VISIBLE:View.GONE); WebView w=web(); if(w!=null){ String u=w.getUrl(); urlBox.setText(u==null?"":u); } tabsBtn.setText("▢ "+tabs.size()); updateStar(); }
    void closeTab(int i){ if(tabs.size()<=1){ showHome(); return; } WebView w=tabs.remove(i); container.removeView(w); w.destroy(); if(current>=tabs.size()) current=tabs.size()-1; switchTab(Math.max(0,current)); }
    void showHome(){ if(web()==null) return; loadingHome=true; web().loadDataWithBaseURL(HOME_BASE, HOME_HTML, "text/html", "UTF-8", null); urlBox.setText(""); status.setText("Pagina inicial - Speed Dial"); }
    void load(String raw){ if(web()==null) return; String u=raw.trim(); if(u.isEmpty()) return; if(u.startsWith("http://")) u="https://"+u.substring(7); if(!u.startsWith("http")){ if(u.contains(".")&&!u.contains(" ")) u="https://"+u; else { try{ u="https://www.google.com/search?q="+URLEncoder.encode(u,"UTF-8"); }catch(Exception e){} } } web().loadUrl(u); }
    // Favoritos e historico (JSON em SharedPreferences)
    JSONArray getList(String key){ try{ return new JSONArray(store.getString(key,"[]")); }catch(Exception e){ return new JSONArray(); } }
    void saveList(String key, JSONArray a){ store.edit().putString(key, a.toString()).apply(); }
    void addHistory(String title, String url){ try{ JSONArray a=getList("history"); if(a.length()>0 && url.equals(a.getJSONObject(0).optString("u"))) return; JSONArray b=new JSONArray(); JSONObject o=new JSONObject(); o.put("t", title==null||title.isEmpty()?url:title); o.put("u", url); b.put(o); for(int i=0;i<a.length()&&b.length()<100;i++) b.put(a.getJSONObject(i)); saveList("history", b); }catch(Exception e){} }
    boolean isBookmarked(String url){ JSONArray a=getList("bookmarks"); for(int i=0;i<a.length();i++) if(url.equals(a.optJSONObject(i)==null?"":a.optJSONObject(i).optString("u"))) return true; return false; }
    void updateStar(){ WebView w=web(); starBtn.setText(w!=null&&w.getUrl()!=null&&isBookmarked(w.getUrl()) ? "★" : "☆"); }
    void toggleBookmark(){ WebView w=web(); if(w==null||w.getUrl()==null||!w.getUrl().startsWith("http")) return; String url=w.getUrl(); JSONArray a=getList("bookmarks"); JSONArray b=new JSONArray(); boolean removed=false; for(int i=0;i<a.length();i++){ JSONObject o=a.optJSONObject(i); if(o!=null&&url.equals(o.optString("u"))){ removed=true; continue; } if(o!=null) b.put(o); } if(!removed){ try{ JSONObject o=new JSONObject(); o.put("t", w.getTitle()==null?url:w.getTitle()); o.put("u", url); b.put(o); }catch(Exception e){} } saveList("bookmarks", b); updateStar(); Toast.makeText(this, removed?"Favorito removido":"Adicionado aos favoritos", Toast.LENGTH_SHORT).show(); }
    void showBookmarks(){ JSONArray a=getList("bookmarks"); if(a.length()==0){ Toast.makeText(this,"Nenhum favorito ainda - toque na estrela", Toast.LENGTH_LONG).show(); return; } String[] items=new String[a.length()]; for(int i=0;i<a.length();i++){ JSONObject o=a.optJSONObject(i); items[i]=o==null?"":o.optString("t"); } new AlertDialog.Builder(this).setTitle("Favoritos").setItems(items,(d,w)->{ JSONObject o=a.optJSONObject(w); if(o!=null) load(o.optString("u")); }).setNegativeButton("Fechar",null).show(); }
    void showHistory(){ JSONArray a=getList("history"); if(a.length()==0){ Toast.makeText(this,"Historico vazio", Toast.LENGTH_SHORT).show(); return; } String[] items=new String[a.length()]; for(int i=0;i<a.length();i++){ JSONObject o=a.optJSONObject(i); items[i]=o==null?"":o.optString("t"); } new AlertDialog.Builder(this).setTitle("Historico").setItems(items,(d,w)->{ JSONObject o=a.optJSONObject(w); if(o!=null) load(o.optString("u")); }).setNegativeButton("Fechar",null).show(); }
    void showTabs(){ String[] items=new String[tabs.size()]; for(int i=0;i<tabs.size();i++){ String t=tabs.get(i).getTitle(); items[i]=(i==current?"● ":"○ ")+(t==null||t.isEmpty()?"Nova aba":t); } new AlertDialog.Builder(this).setTitle("Abas abertas ("+tabs.size()+")").setItems(items,(d,w)->switchTab(w)).setPositiveButton("Nova aba",(d,w)->newTab(null)).setNeutralButton("Fechar atual",(d,w)->closeTab(current)).setNegativeButton("Cancelar",null).show(); }
    void showMenu(){ boolean desk=store.getBoolean("desktop_mode",false); String[] items={"Nova aba","Fechar aba atual","Modo desktop: "+(desk?"LIGADO":"desligado"),"Buscar na pagina","Compartilhar pagina","Limpar historico","Limpar favoritos"}; new AlertDialog.Builder(this).setTitle("Menu").setItems(items,(d,w)->{ WebView web=web(); switch(w){ case 0: newTab(null); break; case 1: closeTab(current); break; case 2: boolean nd=!store.getBoolean("desktop_mode",false); store.edit().putBoolean("desktop_mode",nd).apply(); if(web!=null){ web.getSettings().setUserAgentString(nd?UA_DESKTOP:UA_MOBILE); web.reload(); } Toast.makeText(this,"Modo desktop "+(nd?"ligado":"desligado"),Toast.LENGTH_SHORT).show(); break; case 3: findInPage(); break; case 4: if(web!=null&&web.getUrl()!=null){ Intent it=new Intent(Intent.ACTION_SEND); it.setType("text/plain"); it.putExtra(Intent.EXTRA_TEXT, web.getUrl()); startActivity(Intent.createChooser(it,"Compartilhar")); } break; case 5: saveList("history", new JSONArray()); Toast.makeText(this,"Historico limpo",Toast.LENGTH_SHORT).show(); break; case 6: saveList("bookmarks", new JSONArray()); updateStar(); Toast.makeText(this,"Favoritos limpos",Toast.LENGTH_SHORT).show(); break; } }).show(); }
    void findInPage(){ final EditText in=new EditText(this); in.setHint("Palavra na pagina"); new AlertDialog.Builder(this).setTitle("Buscar na pagina").setView(in).setPositiveButton("Buscar",(d,w)->{ if(web()!=null) web().findAllAsync(in.getText().toString()); }).setNegativeButton("Cancelar",null).show(); }
    @Override public void onBackPressed(){ if(web()!=null&&web().canGoBack()) web().goBack(); else super.onBackPressed(); }
    @Override protected void onResume(){ super.onResume(); SharedPreferences pp=ProxyManager.prefs(this); if(pp.getBoolean("active",false)) ProxyManager.apply(this, (ok,msg)->{}); else ProxyManager.clear(this, (ok,msg)->{}); }
}
