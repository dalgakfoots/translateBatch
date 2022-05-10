package onthelive.translateBatch.entity;

import lombok.Data;

@Data
public class Segment {

    private Long projectId;
    private Long documentId;
    private Long sectionId;
    private Long id;
    private String value;

    private Long jobMasterId;
    private Long jobSubId;
    private Long userId;
}
