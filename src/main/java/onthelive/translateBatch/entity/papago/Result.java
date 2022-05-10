package onthelive.translateBatch.entity.papago;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Result {
    private String srcLangType;
    private String tarLangType;
    private String translatedText;
}
