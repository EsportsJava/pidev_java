package tn.esprit.utils;

/**
 * Clés API en dur (dev / démo uniquement).
 * <p>
 * Priorité pour la page Stats (voir {@code StatsDetailController}) :
 * cette constante si non vide, puis {@code -DRIOT_API_KEY=...}, puis la variable d'environnement.
 * <p>
 * Ne commitez pas de vraies clés sur un dépôt public.
 */
public final class ApiKeys {

    private ApiKeys() {
    }

    public static final String RIOT_API_KEY = "RGAPI-931e77a4-7278-45ac-a5c5-747afb68bdfc";

    public static final String STEAM_API_KEY = "662F67CC46A8120DEB0EF079FF4A4776";

    public static final String HENRIK_API_KEY = "HDEV-395e36fe-e9e1-406c-bdef-977b819be8a4";
}
