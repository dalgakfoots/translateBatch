package onthelive.translateBatch.service.kakao;

import onthelive.translateBatch.entity.enums.KaKaoLanguageCode;
import onthelive.translateBatch.entity.kakao.KakaoResponseBody;
import onthelive.translateBatch.service.TranslateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class KakaoTranslateService implements TranslateService {

    @Value("${kakao.authorization}")
    private String authorization;

    @Value("${kakao.translateApiUrl}")
    private String translateApiUrl;

    @Override
    public String translateText(String source, String target, String text) {
        StringBuffer result = new StringBuffer();

        String sourceCode = KaKaoLanguageCode.valueOf(source).getCode();
        String targetCode = KaKaoLanguageCode.valueOf(target).getCode();

        HttpEntity entity = getHttpEntity(sourceCode, targetCode, text);

        RestTemplate template = new RestTemplate();
        KakaoResponseBody response = template.postForEntity(translateApiUrl, entity, KakaoResponseBody.class).getBody();

        String[] strings = response.getTranslated_text().get(0);
        for (String string : strings) {
            result.append(string);
        }

        return result.toString();
    }

    /* PRIVATE METHODS */

    private HttpEntity getHttpEntity(String sourceCode, String targetCode, String text) {
        HttpHeaders headers = new HttpHeaders();
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        headers.add("Authorization",authorization);

        body.add("src_lang", sourceCode);
        body.add("target_lang", targetCode);
        body.add("query", text);

        HttpEntity entity = new HttpEntity(body, headers);
        return entity;
    }
}
