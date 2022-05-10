package onthelive.translateBatch.service;

public interface TranslateService {
    public String translateText(String source , String target , String text) throws Exception;
}
