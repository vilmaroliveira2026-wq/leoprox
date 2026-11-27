package com.vilmar.proxbrowser;
import android.app.Activity; import android.content.SharedPreferences; import android.os.Bundle; import android.widget.*; import java.util.*;
public class ProxyActivity extends Activity {
    EditText host, port, user, pass; TextView msg;
    @Override protected void onCreate(Bundle s){ super.onCreate(s); setContentView(R.layout.activity_proxy);
        host=findViewById(R.id.hostBox); port=findViewById(R.id.portBox); user=findViewById(R.id.userBox); pass=findViewById(R.id.passBox);
        msg=findViewById(R.id.proxyMsg); ListView list=findViewById(R.id.proxyList); Spinner country=findViewById(R.id.countrySpinner);
        String[] countries={"US","BR","DE","FR","GB","NL","CA","ES","IT","PL"};
        country.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,countries));
        SharedPreferences p=ProxyManager.prefs(this);
        host.setText(p.getString("host","")); if(p.getInt("port",0)>0) port.setText(String.valueOf(p.getInt("port",0)));
        user.setText(p.getString("user","")); pass.setText(p.getString("pass",""));
        ((Button)findViewById(R.id.testBtn)).setOnClickListener(v->{
            String hh=host.getText().toString().trim(); int pp; try{ pp=Integer.parseInt(port.getText().toString()); }catch(Exception e){ msg.setText("Porta invalida"); return; }
            msg.setText("Testando conexao direta com a proxy...");
            new Thread(()->{ String r=LocalProxyServer.testUpstream(hh,pp,user.getText().toString(),pass.getText().toString());
                runOnUiThread(()-> msg.setText("Resultado do teste: "+r+(r.contains(" 200")?"  => PROXY OK, pode aplicar":r.contains("407")?"  => A proxy exige autenticacao correta (confira usuario/senha no painel da IPRoyal)":"  => Proxy nao aceitou a conexao"))); }).start();
        });
        ((Button)findViewById(R.id.applyBtn)).setOnClickListener(v->{
            String hh=host.getText().toString().trim(); int pp; try{ pp=Integer.parseInt(port.getText().toString()); }catch(Exception e){ msg.setText("Preencha host e porta validos"); return; }
            if(hh.isEmpty()){ msg.setText("Preencha host e porta validos"); return; }
            ProxyManager.apply(this,hh,pp,user.getText().toString(),pass.getText().toString(),(ok,m)-> runOnUiThread(()->{ msg.setText(m); if(ok) finish(); }));
        });
        ((Button)findViewById(R.id.offBtn)).setOnClickListener(v-> ProxyManager.clear(this,(ok,m)-> runOnUiThread(()-> msg.setText(m))));
        ((Button)findViewById(R.id.fetchBtn)).setOnClickListener(v->{
            msg.setText("Buscando proxies gratis...");
            FreeProxyApi.fetch(country.getSelectedItem().toString(), proxies-> runOnUiThread(()->{
                if(proxies.isEmpty()){ msg.setText("Nenhuma proxy retornou agora. Tente outro pais ou cole uma manual."); return; }
                msg.setText(proxies.size()+" proxies encontradas (sem autenticacao). Toque numa pra aplicar:");
                List<String> labels=new ArrayList<>(); for(String[] pr:proxies) labels.add(pr[0]+":"+pr[1]);
                list.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels));
                list.setOnItemClickListener((parent,view,i,pos)->{ String[] pr=proxies.get(i); ProxyManager.apply(this,pr[0],Integer.parseInt(pr[1]),"","",(ok,m)-> runOnUiThread(()->{ msg.setText(m); if(ok) finish(); })); });
            }));
        });
    }
}
