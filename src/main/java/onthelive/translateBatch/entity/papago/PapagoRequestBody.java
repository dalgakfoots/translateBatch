package onthelive.translateBatch.entity.papago;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PapagoRequestBody {
    private String source;
    private String target;
    private String text; // 1회 Max 5000자
}
