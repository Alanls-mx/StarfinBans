# Ativação do StarfinBans

## Spigot

Instale o StarfinCore e o JAR `StarfinBans-Spigot` no mesmo servidor. Ative a
instalação do Core pelo código apresentado no console e atribua `StarfinBans`
ao servidor no painel. O Bans lê o serviço `VerifiedAuthorizationService`
do Core e verifica localmente a assinatura ECDSA P-256, produto, instalação,
porta e validade. `canStart` sozinho não autoriza o Bans.

## BungeeCord

Instale os JARs `StarfinCore-Bungee` e `StarfinBans-Bungee` no proxy. O Core
mantém a identidade da instalação e consulta a StarfinWeb. Use
`/starfincore ativar` para obter um código de vínculo quando necessário e
atribua `StarfinBans` no painel. O Bans depende do Core e valida localmente o
grant assinado para `starfin_bans` antes de habilitar comandos e punições.

A porta vinculada é a primeira porta de escuta do proxy. Configure-a no painel
para corresponder à instalação. O Core consulta o site a cada minuto. Quando
a autorização estiver pendente ou indisponível, o Bans permanece inativo.
Proteja `plugins/StarfinCore/internal/`, que contém a credencial da instalação.

## Chaves e compatibilidade

Os consumidores aceitam grants de protocolo 1, emissor
`starfinplugins.com.br` e chave `starfin-2026-10-p256`. A chave privada fica
somente no servidor Web, apontada por `STARFIN_GRANT_PRIVATE_KEY_PATH`. A chave
pública está embutida nos JARs. Uma rotação requer publicar novos JARs que
confiem na nova chave antes de mudar a chave privada do Web.
