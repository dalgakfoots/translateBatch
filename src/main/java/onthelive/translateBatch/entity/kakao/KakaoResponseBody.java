package onthelive.translateBatch.entity.kakao;

import lombok.Data;

import java.util.List;

@Data
public class KakaoResponseBody {
    private List<String[]> translated_text;
}
