package onthelive.translateBatch.entity.enums;

public enum KaKaoLanguageCode {
    en("en"),
    ja("jp"),
    zh("cn"),
    es("es"),
    ru("ru"),
    de("de"),
    fr("fr"),
    ko("kr");

    private String code;

    KaKaoLanguageCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
