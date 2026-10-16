# Ativação do StarfinBans

## Spigot

Instale o StarfinCore e o JAR `StarfinBans-Spigot` no mesmo servidor. Ative a
instalação do Core pelo código apresentado no console e atribua `StarfinBans`
ao servidor no painel. O Bans lê o serviço `VerifiedAuthorizationService`
do Core e verifica localmente a assinatura ECDSA P-256, produto, instalação,
porta e validade. `canStart` sozinho não autoriza o Bans.

## BungeeCord

Instale somente o JAR `StarfinBans-Bungee`. O proxy gera um ID e uma credencial
próprios no primeiro início, salva-os em `plugins/StarfinBans/config.yml` e
imprime um código temporário no console. Vincule esse código a uma instalação
no painel da StarfinWeb e atribua o produto. A validação usa a mesma API e a
mesma chave pública do Spigot, sem depender de `StarfinLicense` ou Bukkit.

A porta vinculada é a primeira porta de escuta do proxy. Configure-a no painel
para corresponder à instalação. O proxy consulta o site a cada minuto.
Enquanto a autorização estiver pendente ou indisponível, novas entradas e o
chat são bloqueados. A cópia de `config.yml` expõe a credencial da instalação;
trate o arquivo como segredo e revogue o vínculo no painel após comprometimento.

## Chaves e compatibilidade

Os consumidores aceitam grants de protocolo 1, emissor
`starfinplugins.com.br` e chave `starfin-2026-10-p256`. A chave privada fica
somente no servidor Web, apontada por `STARFIN_GRANT_PRIVATE_KEY_PATH`. A chave
pública está embutida nos JARs. Uma rotação requer publicar novos JARs que
confiem na nova chave antes de mudar a chave privada do Web.
