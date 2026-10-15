# StarfinBans — integração controlada com StarfinCore

O módulo Spigot/Bukkit agora aceita StarfinCore quando disponível e habilitado. A checagem inicial do Core é remota e assíncrona, então o Bans espera até 90s pela autorização antes de carregar suas funções. Enquanto não houver autorização, comandos e listeners de punição não são registrados. Não há bypass nem chave embutida no JAR.

Se StarfinCore não estiver instalado, o Bans conserva a compatibilidade com o StarfinLicense legado (temporária para instalações antigas). Quando o Core está presente, ele tem prioridade e não ocorre fallback para um plugin legado diante de uma negativa do Core. Verificações periódicas continuam fail-closed.

O identificador de produto é `starfin_bans`. O Bans não se comunica diretamente com a API de licenças. O armazenamento e o histórico de punições não foram alterados.

**BungeeCord:** permanece com StarfinLicense pois o StarfinCore disponível é plugin Bukkit/Spigot, não um plugin Bungee. Não copiar o JAR do Core para proxy; uma implementação compatível será necessária. Além disso, ainda faltam testes em servidores reais, integração Web, casos de revogação e documentação legal/comercial.
