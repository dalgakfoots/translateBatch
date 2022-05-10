package onthelive.translateBatch.service.papago;

import onthelive.translateBatch.entity.papago.PapagoRequestBody;
import onthelive.translateBatch.entity.papago.PapagoResponseBody;
import onthelive.translateBatch.service.TranslateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PapagoTranslateService implements TranslateService {

    @Value("${papago.clientId}")
    private String clientId;
    @Value("${papago.clientSecret}")
    private String clientSecret;
    @Value("${papago.translateApiUrl}")
    private String translateApiUrl;

    @Override
    public String translateText(String source, String target, String text) {
        PapagoRequestBody body = new PapagoRequestBody(source, target, text);

        WebClient client = WebClient.builder()
                .baseUrl(translateApiUrl)
                .build();

        PapagoResponseBody response = client.post()
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, e -> e.bodyToMono(String.class).map(Exception::new))
                .bodyToMono(PapagoResponseBody.class)
                .block();
        String result = response.getMessage().getResult().getTranslatedText();
        return result;
    }
}
