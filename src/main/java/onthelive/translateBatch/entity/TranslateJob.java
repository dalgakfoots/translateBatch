package onthelive.translateBatch.entity;

import com.google.gson.Gson;
import lombok.Data;
import onthelive.translateBatch.entity.enums.TranslatorType;
import onthelive.translateBatch.service.TranslateService;

import java.util.HashMap;

@Data
public class TranslateJob {

    private String sourceText;
    private String sourceLang;
    private String targetLang;
    private String targetText;
    private TranslatorType mtSource;

    private Long segmentId;
    private Long projectId;
    private Long documentId;
    private Long sectionId;

    private Long jobMasterId;
    private Long jobSubId;
    private Long userId;
    private String processCode;
    private String state;
    private Long historyCnt;



    private TranslateService translateService;

    /* PUBLIC METHOD */

    public void translate() throws Exception {
        HashMap<String, String> temp = new HashMap<>();
        String translateValue = translateService.translateText(sourceLang, targetLang, sourceText);
        temp.put("translatedText",translateValue);

        String translate = new Gson().toJson(temp);
        setTargetText(translate);
    }

}
