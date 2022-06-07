package onthelive.translateBatch.batch.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import onthelive.translateBatch.batch.listener.NoWorkFoundStepExecutionListener;
import onthelive.translateBatch.batch.step.AfterTranslateUpdateSectionTaskletBean;
import onthelive.translateBatch.batch.step.TranslateProcessor;
import onthelive.translateBatch.entity.Segment;
import onthelive.translateBatch.entity.TranslateJob;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TranslateBatchConfigure {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    private final TranslateProcessor translateProcessor;
    private final Step afterTranslateUpdateSectionStep;

    private static final int CHUNK_SIZE = 1;

    @Value("${project-type-code}")
    private String projectTypeCode;

    // --------------- MultiThread --------------- //
    private static final int DEFAULT_POOL_SIZE = 10;

    public TaskExecutor executor(int poolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setThreadNamePrefix("multi-thread-");
        executor.setWaitForTasksToCompleteOnShutdown(Boolean.TRUE);
        executor.initialize();
        return executor;
    }
    // --------------- MultiThread --------------- //


    // --------------- Job --------------- //
    @Bean
    public Job translateBatchJob() throws Exception {
        return jobBuilderFactory.get("translateBatchJob")
                .start(translateStep())
                .on("*").to(afterTranslateUpdateSectionStep)
                .end()
                .incrementer(new RunIdIncrementer()).build();
    }
    // --------------- Job --------------- //

    // --------------- Translate Step --------------- //

    @Bean
    public Step translateStep() throws Exception {
        log.info("Translation Start");
        return stepBuilderFactory.get("translateStep")
                .<Segment, TranslateJob>chunk(CHUNK_SIZE)
                .reader(translateReader())
                .processor(translateProcessor)
                .writer(compositeItemWriter(
                        translateWriter(),
                        updateJobMastersSetStateCompleteTranslate(),
                        insertIntoJobHistoriesTranslate(),
                        updateJobSubsSetStateCompleteTranslate(),
                        updateSegmentSetStateCompleteTranslate()
                ))
                .listener(new NoWorkFoundStepExecutionListener())
                .taskExecutor(executor(DEFAULT_POOL_SIZE))
                .throttleLimit(DEFAULT_POOL_SIZE)
                .build();
    }

    @Bean
    public JdbcPagingItemReader<Segment> translateReader() throws Exception {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("process_code", "machine_translation");
        parameterValues.put("state", "WAIT");
        parameterValues.put("state1", "FAIL");
        parameterValues.put("project_type_code", projectTypeCode); //TODO 프로젝트 타입 코드는 16-1 또는 16-3 이다. 프로퍼티로 별도로 관리 고려할 것.

        return new JdbcPagingItemReaderBuilder<Segment>()
                .pageSize(CHUNK_SIZE)
                .fetchSize(CHUNK_SIZE)
                .dataSource(dataSource)
                .rowMapper(new BeanPropertyRowMapper<>(Segment.class))
                .name("translateReader")
                .queryProvider(translateQueryProvider())
                .parameterValues(parameterValues)
                .saveState(false)
                .build();
    }

    @Bean
    public PagingQueryProvider translateQueryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();

        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("select a.project_id as projectId, " +
                "a.document_id as documentId, " +
                "a.section_id as sectionId, " +
                "a.id , a.value, " +
                "b.job_master_id as jobMasterId, " +
                "c.job_sub_id as jobSubId, " +
                "c.user_id as userId");
        queryProvider.setFromClause("from segments a " +
                "inner join ( select id as job_master_id, project_id as pid, document_id as did, section_id as sid, segment_id as sgid from job_masters ) b " +
                "on a.id = b.sgid and a.section_id = b.sid and a.document_id = b.did and a.project_id = b.pid " +
                "inner join ( select job_master_id, id as job_sub_id , user_id from job_subs) c " +
                "on b.job_master_id = c.job_master_id " +
                "inner join ( select id as pid , project_type_code from projects ) d on a.project_id = d.pid ");
        queryProvider.setWhereClause("where a.current_process = :process_code and (current_state = :state or current_state = :state1) " +
                "and d.project_type_code = :project_type_code");

        Map<String, Order> sortKeys = new HashMap<>(1);
        sortKeys.put("id", Order.ASCENDING);
        queryProvider.setSortKeys(sortKeys);

        return queryProvider.getObject();
    }

    // --------------- Translate Step --------------- //

    //

    @Bean
    public JdbcBatchItemWriter<TranslateJob> translateWriter() {
        return new JdbcBatchItemWriterBuilder<TranslateJob>()
                .dataSource(dataSource)
                .sql("insert into job_sub_results (job_master_id , job_sub_id , value) " +
                        "values (:jobMasterId , :jobSubId , :targetText) " +
                        "on duplicate key update value = :targetText, updated_datetime = now()")
                .beanMapped()
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<TranslateJob> insertIntoJobHistoriesTranslate() {
        return new JdbcBatchItemWriterBuilder<TranslateJob>()
                .dataSource(dataSource)
                .sql("INSERT INTO job_sub_histories (id, job_master_id, job_sub_id, user_id, process_code, state, reject_state) " +
                        "values (:historyCnt + 1 , :jobMasterId , :jobSubId , :userId , :processCode , :state , '0') " +
                        "on duplicate key update state = :state")
                .beanMapped()
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<TranslateJob> updateJobMastersSetStateCompleteTranslate() {
        return new JdbcBatchItemWriterBuilder<TranslateJob>()
                .dataSource(dataSource)
                .sql("update job_masters set current_state = :state , updated_datetime = now() where id = :jobMasterId")
                .beanMapped()
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<TranslateJob> updateJobSubsSetStateCompleteTranslate() {
        return new JdbcBatchItemWriterBuilder<TranslateJob>()
                .dataSource(dataSource)
                .sql("update job_subs set state = :state, updated_datetime = now() where job_master_id = :jobMasterId and id = :jobSubId")
                .beanMapped()
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<TranslateJob> updateSegmentSetStateCompleteTranslate() {
        return new JdbcBatchItemWriterBuilder<TranslateJob>()
                .dataSource(dataSource)
                .sql("update segments set current_state = :state, updated_datetime = now() " +
                        "where id = :segmentId and project_id = :projectId and document_id = :documentId " +
                        "and section_id = :sectionId")
                .beanMapped()
                .build();
    }

    @Bean
    public CompositeItemWriter<TranslateJob> compositeItemWriter(
            @Qualifier("translateWriter") JdbcBatchItemWriter<TranslateJob> translateWriter,
            @Qualifier("updateJobMastersSetStateCompleteTranslate") JdbcBatchItemWriter<TranslateJob> updateJobsSetStateCompleteSTT,
            @Qualifier("insertIntoJobHistoriesTranslate") JdbcBatchItemWriter<TranslateJob> insertIntoJobHistoriesSTT,
            @Qualifier("updateJobSubsSetStateCompleteTranslate") JdbcBatchItemWriter<TranslateJob> updateJobSubsSetStateCompleteSTT,
            @Qualifier("updateSegmentSetStateCompleteTranslate") JdbcBatchItemWriter<TranslateJob> updateSegmentSetStateCompleteTranslate
    ) {
        CompositeItemWriter<TranslateJob> writer = new CompositeItemWriter<>();
        writer.setDelegates(
                Arrays.asList(
                        translateWriter,
                        updateJobsSetStateCompleteSTT,
                        insertIntoJobHistoriesSTT,
                        updateJobSubsSetStateCompleteSTT,
                        updateSegmentSetStateCompleteTranslate
                )
        );

        return writer;
    }

}
