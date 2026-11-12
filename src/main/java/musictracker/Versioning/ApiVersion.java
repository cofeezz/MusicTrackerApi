package musictracker.Versioning;

import musictracker.Exception.UnsupportedApiVersionException;

public enum ApiVersion {
    V1("1"),
    V2("2");

    public static final String HEADER = "X-API-Version";

    private final String value;

    ApiVersion(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ApiVersion parse(String requested) {
        if (requested == null || requested.isBlank()) {
            return V1;
        }
        String trimmed = requested.trim();
        for (ApiVersion version : values()) {
            if (version.value.equals(trimmed)) {
                return version;
            }
        }
        throw new UnsupportedApiVersionException(
                "Versão da API não suportada: '" + trimmed + "'. Use o cabeçalho " + HEADER + " com 1 ou 2.");
    }
}
