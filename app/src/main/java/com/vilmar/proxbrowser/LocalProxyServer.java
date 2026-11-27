package com.vilmar.proxbrowser;
import android.util.Base64;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Proxy local (127.0.0.1:18080) que roda dentro do app.
 * O WebView aponta para ele SEM autenticacao, e ele repassa tudo
 * para a proxy externa (IPRoyal etc) injetando o Proxy-Authorization.
 * Isso resolve o ERR_TOO_MANY_RETRIES / loop de autenticacao do WebView,
 * que nao sabe autenticar direto na proxy externa.
 */
public class LocalProxyServer {
    public static final int LOCAL_PORT = 18080;
    private static LocalProxyServer instance;
    private ServerSocket server;
    private volatile boolean running = false;
    private String upHost; private int upPort; private String upUser; private String upPass;
    private Thread acceptThread;

    public static synchronized LocalProxyServer get(){ if(instance==null) instance=new LocalProxyServer(); return instance; }
    public synchronized boolean isRunning(){ return running; }
    public synchronized String upstream(){ return upHost+":"+upPort; }

    public synchronized void start(String host, int port, String user, String pass) throws IOException {
        if(running && host.equals(upHost) && port==upPort && eq(user,upUser) && eq(pass,upPass)) return;
        stop();
        upHost=host; upPort=port; upUser=user==null?"":user; upPass=pass==null?"":pass;
        server=new ServerSocket(LOCAL_PORT, 50, InetAddress.getByName("127.0.0.1"));
        running=true;
        acceptThread=new Thread(()->{
            while(running){
                try{ Socket c=server.accept(); new Thread(()->handle(c)).start(); }
                catch(IOException e){ if(running) e.printStackTrace(); }
            }
        }); acceptThread.start();
    }
    private boolean eq(String a,String b){ return (a==null?"":a).equals(b==null?"":b); }

    public synchronized void stop(){
        running=false;
        try{ if(server!=null) server.close(); }catch(IOException ignored){}
        server=null;
    }

    private String authHeader(){
        if(upUser.isEmpty() && upPass.isEmpty()) return null;
        String raw=upUser+":"+upPass;
        return "Proxy-Authorization: Basic "+Base64.encodeToString(raw.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP)+"\r\n";
    }

    private void handle(Socket client){
        try{
            client.setSoTimeout(60000);
            InputStream cin=client.getInputStream(); OutputStream cout=client.getOutputStream();
            ByteArrayOutputStream headBuf=new ByteArrayOutputStream();
            int matched=0; int b;
            // le cabecalho ate \r\n\r\n
            while((b=cin.read())!=-1){
                headBuf.write(b);
                if((matched==0||matched==2) && b=='\r') matched++;
                else if((matched==1||matched==3) && b=='\n') matched++;
                else matched=0;
                if(matched==4) break;
                if(headBuf.size()>65536) break;
            }
            if(headBuf.size()==0){ client.close(); return; }
            String head=new String(headBuf.toByteArray(), StandardCharsets.ISO_8859_1);
            String firstLine=head.split("\r\n")[0];
            String[] parts=firstLine.split(" ");
            if(parts.length<2){ client.close(); return; }
            String method=parts[0], target=parts[1];

            Socket up=new Socket(); up.connect(new InetSocketAddress(upHost, upPort), 15000); up.setSoTimeout(60000);
            InputStream uin=up.getInputStream(); OutputStream uout=up.getOutputStream();
            String auth=authHeader();

            if("CONNECT".equalsIgnoreCase(method)){
                StringBuilder sb=new StringBuilder();
                sb.append("CONNECT ").append(target).append(" HTTP/1.1\r\n");
                sb.append("Host: ").append(target).append("\r\n");
                if(auth!=null) sb.append(auth);
                sb.append("Proxy-Connection: keep-alive\r\n\r\n");
                uout.write(sb.toString().getBytes(StandardCharsets.ISO_8859_1)); uout.flush();
                // le resposta da proxy externa
                ByteArrayOutputStream rb=new ByteArrayOutputStream(); int m=0; int ch;
                while((ch=uin.read())!=-1){
                    rb.write(ch);
                    if((m==0||m==2)&&ch=='\r') m++; else if((m==1||m==3)&&ch=='\n') m++; else m=0;
                    if(m==4) break; if(rb.size()>65536) break;
                }
                String resp=new String(rb.toByteArray(), StandardCharsets.ISO_8859_1);
                if(resp.contains(" 200")){
                    cout.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1)); cout.flush();
                    relay(client, cin, up, uin);
                } else {
                    // devolve o erro real da proxy (ex: 407) para o WebView
                    cout.write(rb.toByteArray()); cout.flush();
                    up.close(); client.close();
                }
            } else {
                // HTTP puro: repassa com URI absoluta + auth injetada
                StringBuilder sb=new StringBuilder();
                sb.append(firstLine).append("\r\n");
                String[] lines=head.split("\r\n");
                for(int i=1;i<lines.length;i++){
                    String line=lines[i];
                    String low=line.toLowerCase();
                    if(low.startsWith("proxy-authorization:")||low.startsWith("proxy-connection:")||low.startsWith("connection:")) continue;
                    if(!line.isEmpty()) sb.append(line).append("\r\n");
                }
                if(auth!=null) sb.append(auth);
                sb.append("Proxy-Connection: close\r\nConnection: close\r\n\r\n");
                uout.write(sb.toString().getBytes(StandardCharsets.ISO_8859_1)); uout.flush();
                relay(client, cin, up, uin);
            }
        }catch(Exception e){
            try{ client.close(); }catch(Exception ignored){}
        }
    }

    private void relay(Socket client, InputStream cin, Socket up, InputStream uin){
        Thread t=new Thread(()->pipe(cin, upOutput(up)));
        t.start();
        pipe(uin, clientOutput(client));
        try{ up.close(); }catch(Exception ignored){}
        try{ client.close(); }catch(Exception ignored){}
    }
    private OutputStream upOutput(Socket up){ try{ return up.getOutputStream(); }catch(Exception e){ return null; } }
    private OutputStream clientOutput(Socket c){ try{ return c.getOutputStream(); }catch(Exception e){ return null; } }
    private void pipe(InputStream in, OutputStream out){
        if(in==null||out==null) return;
        byte[] buf=new byte[16384]; int n;
        try{ while((n=in.read(buf))!=-1){ out.write(buf,0,n); out.flush(); } }catch(Exception ignored){}
        try{ out.flush(); }catch(Exception ignored){}
    }

    /** Teste direto da proxy externa: abre socket e faz CONNECT em ipinfo.io:443. Retorna a linha de status (ex: HTTP/1.1 200 ...). */
    public static String testUpstream(String host, int port, String user, String pass){
        try{
            Socket s=new Socket(); s.connect(new InetSocketAddress(host,port),12000); s.setSoTimeout(12000);
            String auth="";
            if(!(user==null||user.isEmpty()) || !(pass==null||pass.isEmpty())){
                String raw=(user==null?"":user)+":"+(pass==null?"":pass);
                auth="Proxy-Authorization: Basic "+Base64.encodeToString(raw.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP)+"\r\n";
            }
            String req="CONNECT ipinfo.io:443 HTTP/1.1\r\nHost: ipinfo.io:443\r\n"+auth+"Proxy-Connection: close\r\n\r\n";
            s.getOutputStream().write(req.getBytes(StandardCharsets.ISO_8859_1)); s.getOutputStream().flush();
            BufferedReader r=new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.ISO_8859_1));
            String line=r.readLine(); s.close();
            return line==null?"sem resposta da proxy":line;
        }catch(Exception e){ return "erro: "+e.getClass().getSimpleName()+" - "+e.getMessage(); }
    }
}
