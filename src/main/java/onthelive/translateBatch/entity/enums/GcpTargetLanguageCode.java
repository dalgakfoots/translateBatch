package onthelive.translateBatch.entity.enums;

public enum GcpTargetLanguageCode {

    en("en"),
    ja("ja"),
    zh("zh-CN"),
    es("es"),
    ru("ru"),
    de("de"),
    fr("fr"),
    ko("ko");

    private String code;

    GcpTargetLanguageCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
