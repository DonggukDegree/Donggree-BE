package com.donggree.user.internal.application;

import com.donggree.global.apiPayload.exception.GeneralException;
import com.donggree.user.MemberIdentityService;
import com.donggree.user.internal.application.exception.UserErrorCode;
import com.donggree.user.internal.domain.Member;
import com.donggree.user.internal.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberIdentityServiceImpl implements MemberIdentityService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public void validatePdfOwner(Long memberId, String pdfStudentId, String pdfName) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.MEMBER_NOT_FOUND));

        if (member.getStudentId() == null) {
            return;
        }
        if (!pdfStudentId.equals(member.getStudentId()) || !pdfName.equals(member.getName())) {
            throw new GeneralException(UserErrorCode.PDF_OWNER_MISMATCH);
        }
    }

    @Override
    @Transactional
    public void verifyIdentityIfMatch(Long memberId, String pdfStudentId, String pdfName) {
        if (pdfStudentId == null || pdfName == null) {
            return;
        }
        memberRepository.findById(memberId).ifPresent(member -> {
            if (member.getStudentId() != null
                    && pdfStudentId.equals(member.getStudentId())
                    && pdfName.equals(member.getName())) {
                member.verifyIdentity();
            }
        });
    }
}
