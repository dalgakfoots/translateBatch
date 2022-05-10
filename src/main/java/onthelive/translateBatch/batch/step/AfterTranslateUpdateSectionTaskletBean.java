package onthelive.translateBatch.batch.step;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AfterTranslateUpdateSectionTaskletBean {

    private final StepBuilderFactory stepBuilderFactory;
    private final AfterTranslateUpdateSectionTasklet afterTranslateUpdateSectionTasklet;

    @Bean
    public Step afterTranslateUpdateSectionStep() throws Exception {
        return stepBuilderFactory.get("afterTranslateUpdateSectionStep")
                .tasklet(afterTranslateUpdateSectionTasklet)
                .build();
    }
}
