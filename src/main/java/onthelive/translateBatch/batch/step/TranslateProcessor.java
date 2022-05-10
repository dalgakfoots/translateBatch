package onthelive.translateBatch.batch.step;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import onthelive.translateBatch.entity.Segment;
import onthelive.translateBatch.entity.TranslateJob;
import onthelive.translateBatch.entity.enums.TranslatorType;
import onthelive.translateBatch.service.gcp.GcpTranslateService;
import onthelive.translateBatch.service.kakao.KakaoTranslateService;
import onthelive.translateBatch.service.papago.PapagoTranslateService;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranslateProcessor implements ItemProcessor<Segment, TranslateJob> {

    private final JdbcTemplate jdbcTemplate;

    private final GcpTranslateService gcpTranslateService;
    private final KakaoTranslateService kakaoTranslateService;
    private final PapagoTranslateService papagoTranslateService;

    @Override
    public TranslateJob process(Segment param) throws Exception {

        /*job_master , job_subs , job_sub_histories 전처리*/
        Long historyId = updateTablePreprocess(param);
        /*job_master , job_subs , job_sub_histories 전처리 끝*/

        TranslateJob item = new Gson().fromJson(param.getValue(), TranslateJob.class);
        item.setProcessCode("machine_translation");

        TranslatorType translatorType = item.getMtSource();

        if(translatorType == TranslatorType.GOOGLE) {
            item.setTranslateService(gcpTranslateService);
        } else if(translatorType == TranslatorType.NAVER) {
            item.setTranslateService(papagoTranslateService);
        } else if(translatorType == TranslatorType.KAKAO) {
            item.setTranslateService(kakaoTranslateService);
        }

        try {
            item.translate();
            return setResultParams(param, item, "COMPLETE", historyId);
        } catch (Exception e) {
            e.printStackTrace();
            failProcess(param.getJobMasterId(), param.getJobSubId(), historyId , param.getUserId());
            return setResultParams(param, item, "FAIL", historyId);
        }
    }




    /* PRIVATE METHODS */

    private Long getHistoryId(Long masterId, Long subId) {
        Long historyId = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_sub_histories WHERE job_master_id = ? AND job_sub_id = ?", Long.class,
                masterId, subId
        );
        return historyId;
    }

    private Long updateTablePreprocess(Segment param) {

        Long jobMasterId = param.getJobMasterId();
        Long jobSubId = param.getJobSubId();
        Long userId = param.getUserId();

        Long historyId = getHistoryId(jobMasterId, jobSubId) + 1;

        jdbcTemplate.update("UPDATE job_masters SET current_state = 'PROGRESS', updated_datetime = now() WHERE id = ?", jobMasterId);
        jdbcTemplate.update("UPDATE job_subs SET state = 'PROGRESS', updated_datetime = now() WHERE job_master_id = ? and id = ? ", jobMasterId, jobSubId);

        jdbcTemplate.update("INSERT INTO job_sub_histories (id, job_master_id, job_sub_id, user_id, process_code, state, reject_state) " +
                        "VALUES (?, ? , ? , ? , 'machine_translation', 'PROGRESS' , '0')",
                historyId, jobMasterId, jobSubId, userId
        );

        return historyId;
    }

    private void failProcess(Long masterId, Long subId, Long historyId, Long userId) {
        log.info("failProcess.....");

        jdbcTemplate.update("UPDATE job_masters SET current_state = 'FAIL', updated_datetime = now() WHERE id = ?", masterId);
        jdbcTemplate.update("UPDATE job_subs SET state = 'FAIL', updated_datetime = now() WHERE job_master_id = ? and id = ? ", masterId, subId);

        jdbcTemplate.update("INSERT INTO job_sub_histories (id, job_master_id, job_sub_id, user_id, process_code, state, reject_state) " +
                        "VALUES (?, ? , ? , ? , 'machine_translation', 'FAIL' , '0') on duplicate key update state = 'FAIL'",
                historyId + 1, masterId, subId, userId
        );
    }

    private TranslateJob setResultParams(Segment param, TranslateJob item, String state, Long historyCnt) {
        item.setSegmentId(param.getId());
        item.setProjectId(param.getProjectId());
        item.setDocumentId(param.getDocumentId());
        item.setSectionId(param.getSectionId());

        item.setJobMasterId(param.getJobMasterId());
        item.setJobSubId(param.getJobSubId());
        item.setUserId(param.getUserId());

        item.setState(state);
        item.setHistoryCnt(historyCnt);

        return item;
    }

}
