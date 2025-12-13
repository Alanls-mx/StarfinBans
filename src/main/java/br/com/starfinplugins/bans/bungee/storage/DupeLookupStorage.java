package br.com.starfinplugins.bans.bungee.storage;

import java.util.List;

public interface DupeLookupStorage {

    /**
     * Retorna o último IP conhecido do jogador (pelo target_name).
     * Usa a mesma tabela "punishments".
     */
    String getLastIpByName(String targetName);

    /**
     * Retorna lista de nicks distintos vistos no mesmo IP.
     */
    List<String> getAlts(String ip);
}
