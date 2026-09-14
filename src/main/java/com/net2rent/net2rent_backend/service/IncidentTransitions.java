package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.model.enums.IncidentStatus;

import java.util.EnumSet;
import java.util.Set;

public final class IncidentTransitions {

    public static final Set<IncidentStatus> STARTABLE_FROM = EnumSet.of(IncidentStatus.ASSIGNED);
    public static final Set<IncidentStatus> PAUSABLE_FROM = EnumSet.of(IncidentStatus.IN_PROGRESS);
    public static final Set<IncidentStatus> RESUMABLE_FROM = EnumSet.of(IncidentStatus.PAUSED);
    public static final Set<IncidentStatus> RESOLVABLE_FROM =
            EnumSet.of(IncidentStatus.IN_PROGRESS, IncidentStatus.PAUSED);
    public static final Set<IncidentStatus> CLOSABLE_FROM = EnumSet.of(IncidentStatus.RESOLVED);
    public static final Set<IncidentStatus> CLAIMABLE_FROM = EnumSet.of(IncidentStatus.NEW);

    private IncidentTransitions() {
    }
}