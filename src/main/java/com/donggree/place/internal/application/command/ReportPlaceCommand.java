package com.donggree.place.internal.application.command;

import com.donggree.place.internal.domain.enums.PlaceReportReason;

/** 장소 신고 명령. 기타 사유는 설명이 필요하다. */
public record ReportPlaceCommand(PlaceReportReason reason, String description) {}
