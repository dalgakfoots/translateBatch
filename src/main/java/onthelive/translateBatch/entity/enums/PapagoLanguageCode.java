package onthelive.translateBatch.entity.enums;

public enum PapagoLanguageCode {
    en("en"),
    ja("ja"),
    zh("zn-CN"),
    es("es"),
    ru("ru"),
    de("de"),
    fr("fr"),
    ko("ko");

    private String code;

    PapagoLanguageCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
