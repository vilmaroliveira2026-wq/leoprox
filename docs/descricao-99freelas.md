# Texto pronto para postar na 99Freelas (copiar e colar)

**Título do projeto:**
App Android - Navegador com proxy isolado (WebView + proxy autenticada) - continuidade e melhorias

**Categoria:** Programação & TI > Desenvolvimento de Aplicativos

**Descrição:**
Tenho um app Android próprio já funcionando na v3 (código-fonte completo em Java, sem Kotlin, minSdk 29) chamado LeoProx: um navegador isolado onde só o app usa a proxy configurada (não é VPN). Ele já tem:
- WebView com ProxyController (androidx.webkit)
- Motor de proxy local em Java puro (127.0.0.1) que injeta autenticação Basic na proxy externa (resolve ERR_TOO_MANY_RETRIES do WebView)
- Integração com proxy paga (IPRoyal) e lista de proxies gratuitas por país (API ProxyScrape)
- Teste de conexão real da proxy dentro do app
- Página inicial e de erro próprias

Preciso de um freelancer para a próxima etapa:
1. Suporte a proxy SOCKS5 através do motor local
2. Teste automático de velocidade/mortas na lista de proxies gratuitas
3. Sistema de abas, favoritos e histórico
4. Publicação na Play Store (build release assinado)

O repositório está organizado, com README, licença MIT e GitHub Actions gerando o APK a cada push. Entrego o código e o APK atual para avaliação antes do início.

**Habilidades desejadas:** Android, Java, WebView, redes/proxy (HTTP CONNECT, SOCKS5), Git/GitHub

**O que enviar na proposta:** experiência com apps de rede/VPN/proxy, prazo estimado por etapa e valor por etapa (não quero pacote fechado sem avaliação do código).
