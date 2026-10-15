# Licenciamento do StarfinBans

## Bukkit / PaperSpigot 1.8.8

O artefato Bukkit exige `StarfinCore` como dependência em `plugin.yml`. O produto consultado é sempre `starfin_bans`; uma autorização de outro produto não libera o Bans. Na inicialização, os comandos são registrados com guardas locais, enquanto armazenamento, listeners e lógica de punições aguardam autorização. A consulta lê apenas a memória do Core; a rede é responsabilidade do Core em tarefa assíncrona.

Se a autorização for revogada ou a tolerância expirar, comandos, listeners, placeholders e chamadas públicas de `PunishmentAPI` bloqueiam novas operações. O histórico e o banco permanecem intactos. Após uma nova autorização confirmada, as operações são liberadas sem reiniciar o servidor. Não há fallback para StarfinLicense no Bukkit.

`messages.yml` continua exclusivo do Bans. O campo `prefix` usa a identidade StarfinPlugins; as chaves `license.*` cobrem ausência, pendência, concessão, suspensão e recuperação. As mensagens de punição existentes continuam editáveis no mesmo arquivo.

## BungeeCord

`bungee.yml` e a implementação Bungee continuam dependentes do StarfinLicense até existir um Core nativo homologado para proxy. Um JAR Bukkit não pode ser instalado no BungeeCord.

## Homologação

O build Maven compilou o artefato conjunto. O teste em PaperSpigot 1.8.8 descartável comprovou o estado sem chave e a ausência de inicialização das funcionalidades de punição. Ainda faltam credencial de homologação válida, revogação real, recuperação e teste Bungee. Não utilizar esta migração como evidência de autorização comercial em produção.
