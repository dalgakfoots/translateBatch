package onthelive.translateBatch.service.gcp;

import com.google.cloud.translate.v3.*;
import onthelive.translateBatch.entity.enums.GcpTargetLanguageCode;
import onthelive.translateBatch.service.TranslateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GcpTranslateService implements TranslateService {

    @Value("${gcp.projectId}")
    private String projectId;

    @Override
    public String translateText(String source, String target, String text) throws Exception {
        StringBuffer result = new StringBuffer();
        try(TranslationServiceClient client = TranslationServiceClient.create()){
            LocationName parent = LocationName.of(projectId, "global");

            String targetLanguageCode = GcpTargetLanguageCode.valueOf(target).getCode();

            TranslateTextRequest request =
                    TranslateTextRequest.newBuilder()
                            .setParent(parent.toString())
                            .setMimeType("text/plain")
                            .setTargetLanguageCode(targetLanguageCode)
                            .addContents(text)
                            .build();

            TranslateTextResponse response = client.translateText(request);
            for(Translation translation : response.getTranslationsList()) {
                result.append(translation.getTranslatedText());
            }

            return result.toString();
        }
    }
}
