# StarfinBans e StarfinCore

Em Spigot e BungeeCord, instale as distribuições correspondentes dos dois plugins no mesmo servidor. O Core consulta a StarfinWeb para autorizar `starfin_bans`. O Bans consulta o estado atualizado do Core e valida de forma independente a assinatura do grant, o produto, a instalação, a porta e a validade.

No BungeeCord, `StarfinCore` é uma dependência obrigatória declarada no `bungee.yml`. Sem o Core ou sem autorização válida, o Bans não registra comandos de punição e não aplica banimentos ou mutes. Cada comando e evento confere novamente a autorização antes de agir. A perda da autorização remove os comandos; o retorno da autorização permite a inicialização novamente.

O Core Bungee importa uma identidade antiga do `plugins/StarfinBans/config.yml` no primeiro início para preservar o vínculo existente. Depois dessa migração, a identidade e a credencial ficam em `plugins/StarfinCore/internal/`; a configuração antiga do Bans não deve ser apagada antes de confirmar a importação.
