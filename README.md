# LeoProx

Navegador Android **isolado com proxy**: só ele usa a proxy configurada — o resto do celular continua com o IP normal. Não é VPN.

![version](https://img.shields.io/badge/version-3.0-blue) ![minSdk](https://img.shields.io/badge/minSdk-29-green) ![license](https://img.shields.io/badge/license-MIT-lightgrey)

## Por que ele existe
Extensões de proxy no desktop são fáceis. No celular, ou você instala uma VPN (muda o aparelho todo) ou fica sem nada. O LeoProx resolve isso: um navegador próprio onde você cola qualquer proxy HTTP/HTTPS (IPRoyal, Bright Data, Oxylabs, ou proxies públicas) e navega por outro país só dentro dele.

## Arquitetura (o diferencial da v3)
O WebView do Android **não autentica direto em proxy externa** — o resultado é o erro `ERR_TOO_MANY_RETRIES`. A v3 resolve com um **motor de proxy local**:

```
WebView ──► Proxy local 127.0.0.1:18080 (dentro do app, sem auth)
                │  injeta Proxy-Authorization (Basic)
                ▼
        Proxy externa (ex: sua proxy, ex: 203.0.113.10:8080)
                ▼
             Internet
```

- `LocalProxyServer.java` — servidor proxy local: túnel CONNECT para HTTPS, forward de HTTP puro, injeção de autenticação, relay bidirecional.
- `ProxyManager.java` — sobe o motor local e aponta o WebView para ele via `ProxyController` (androidx.webkit). Só este app é afetado.
- `FreeProxyApi.java` — lista de proxies públicas gratuitas por país (API ProxyScrape, sem chave).
- Botão **Testar Proxy**: faz um CONNECT real na proxy externa e mostra a resposta crua (200 = ok, 407 = autenticação errada), sem chute.

## Recursos
- Página inicial própria (busca Google, atalhos TikTok/YouTube/Instagram, Ver meu IP)
- Perfis de proxy manuais + perfil pronto com 1 toque
- Página de erro própria com diagnóstico e atalhos (testar / desligar proxy / início)
- Sem DuckDuckGo, sem rastreadores, só 2 permissões (Internet e estado de rede)

## Como compilar
**Android Studio:** abra a pasta, sincronize o Gradle e `Build > Build APK(s)`.
**Linha de comando:** `gradle assembleDebug`
**GitHub Actions:** cada push na `main` gera o APK automaticamente (aba Actions > artefato `LeoProx-apk`).

Requisitos: Android Studio Hedgehog+, JDK 17, minSdk 29 (Android 10).

## Roadmap
- [ ] Suporte SOCKS5 via túnel local
- [ ] Teste automático de velocidade e pular proxies mortas na lista gratuita
- [ ] Abas, favoritos e histórico
- [ ] Múltiplos perfis nomeados (ex: "TikTok US", "Shopee BR")

## Licença
MIT — veja `LICENSE`.
