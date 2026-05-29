package com.donggree.global.support;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

import com.donggree.global.auth.LoginMemberIdArgumentResolver;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

@ExtendWith(RestDocumentationExtension.class)
public abstract class RestDocsSupport {

    protected MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.standaloneSetup(initController())
                .setControllerAdvice(controllerAdvices())
                .setCustomArgumentResolvers(argumentResolvers().toArray(new HandlerMethodArgumentResolver[0]))
                .addFilters(new CharacterEncodingFilter(StandardCharsets.UTF_8.name(), true))
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .build();
    }

    protected abstract Object initController();

    /**
     * 테스트에서 사용할 커스텀 ArgumentResolver 목록을 반환한다.
     * 기본으로 LoginMemberIdArgumentResolver를 포함하며, 하위 클래스에서 오버라이드 가능하다.
     */
    protected List<HandlerMethodArgumentResolver> argumentResolvers() {
        return List.of(new LoginMemberIdArgumentResolver());
    }

    /** 테스트에서 사용할 @ControllerAdvice 목록. 예외 핸들링이 필요한 테스트에서 오버라이드한다. */
    protected Object[] controllerAdvices() {
        return new Object[0];
    }
}
