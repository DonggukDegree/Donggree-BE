package com.donggree.place.internal.application;

import static com.donggree.place.internal.domain.PlaceFixtures.NEAR_CAMPUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.donggree.place.internal.domain.Place;
import com.donggree.place.internal.domain.PlaceFixtures;
import com.donggree.user.MemberProfile;
import com.donggree.user.UserLookupService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PlaceDetailReaderTest {

    private final UserLookupService userLookupService = mock(UserLookupService.class);
    private final PlaceDetailReader reader = new PlaceDetailReader(userLookupService);

    @Test
    void 등록자_닉네임을_조회해_담는다() {
        given(userLookupService.findProfiles(List.of(1L))).willReturn(Map.of(1L, new MemberProfile(1L, "김아코", null)));

        assertThat(reader.read(PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS)).registrantNickname())
                .isEqualTo("김아코");
    }

    @Test
    void 탈퇴로_등록자가_해제됐거나_조회되지_않으면_닉네임은_null이다() {
        Place anonymized = PlaceFixtures.place(10L, "동국반점", NEAR_CAMPUS);
        ReflectionTestUtils.setField(anonymized, "createdBy", null);

        assertThat(reader.read(anonymized).registrantNickname()).isNull();
        verify(userLookupService, never()).findProfiles(any());

        given(userLookupService.findProfiles(List.of(1L))).willReturn(Map.of());
        assertThat(reader.read(PlaceFixtures.place(11L, "필동커피", NEAR_CAMPUS)).registrantNickname())
                .isNull();
    }
}
