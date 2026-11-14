package com.donggree.user.internal.domain;

import com.donggree.user.ReportTargetType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberSuspensionRepository extends JpaRepository<MemberSuspension, Long> {

    List<MemberSuspension> findByMemberIdOrderByIdDesc(Long memberId);

    Optional<MemberSuspension> findTopByMemberIdOrderByIdDesc(Long memberId);

    @Query("select s.memberId, count(s) from MemberSuspension s"
            + " where s.memberId in :memberIds and s.reportType = :reportType group by s.memberId")
    List<Object[]> countByMemberIdsAndReportType(
            @Param("memberIds") Collection<Long> memberIds, @Param("reportType") ReportTargetType reportType);
}
