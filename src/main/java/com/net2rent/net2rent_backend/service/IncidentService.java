package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.ClassifyIncidentRequest;
import com.net2rent.net2rent_backend.dto.CorrectIncidentTextRequest;
import com.net2rent.net2rent_backend.dto.IncidentResponse;
import com.net2rent.net2rent_backend.dto.RejectIncidentRequest;
import com.net2rent.net2rent_backend.dto.request.IncidentFilter;
import com.net2rent.net2rent_backend.dto.response.GuestIncidentSummaryResponse;
import com.net2rent.net2rent_backend.dto.request.CreatePhoneIncidentRequest;
import com.net2rent.net2rent_backend.dto.request.CreateGuestIncidentRequest;
import com.net2rent.net2rent_backend.dto.response.GuestIncidentResponse;
import com.net2rent.net2rent_backend.dto.response.IncidentListResponse;
import com.net2rent.net2rent_backend.dto.response.IncidentSummaryResponse;
import com.net2rent.net2rent_backend.dto.response.PagedResponse;
import com.net2rent.net2rent_backend.model.enums.*;
import com.net2rent.net2rent_backend.security.GuestPrincipal;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.Account;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.IncidentCounter;
import com.net2rent.net2rent_backend.model.Lodging;
import com.net2rent.net2rent_backend.repository.IncidentCounterRepository;
import com.net2rent.net2rent_backend.repository.IncidentRepository;
import com.net2rent.net2rent_backend.repository.LodgingRepository;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.repository.spec.IncidentSpecifications;
import com.net2rent.net2rent_backend.repository.spec.SortField;
import com.net2rent.net2rent_backend.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentCounterRepository incidentCounterRepository;
    private final IncidentHistoryService incidentHistoryService;
    private final LodgingRepository lodgingRepository;
    private final UserRepository userRepository;
    private final IncidentImageService incidentImageService;
    private final Clock clock;

    public IncidentService(IncidentRepository incidentRepository,
                           IncidentCounterRepository incidentCounterRepository,
                           IncidentHistoryService incidentHistoryService,
                           LodgingRepository lodgingRepository,
                           UserRepository userRepository, IncidentImageService incidentImageService,
                           Clock clock) {
        this.incidentRepository = incidentRepository;
        this.incidentCounterRepository = incidentCounterRepository;
        this.incidentHistoryService = incidentHistoryService;
        this.lodgingRepository = lodgingRepository;
        this.userRepository = userRepository;
        this.incidentImageService = incidentImageService;
        this.clock = clock;
    }

    // ---------- Lectura ----------

    @Transactional(readOnly = true)
    public IncidentListResponse list(IncidentFilter filter,
                                     SortField sortField,
                                     Sort.Direction direction,
                                     Pageable pageable,
                                     AuthUser user,
                                     OperatorScope scope) {
        Long operatorUserId = UserRole.OPERATOR.name().equals(user.role())
                ? user.userId()
                : null;

        Specification<Incident> filterSpec =
                IncidentSpecifications.forListing(user.accountId(), filter, operatorUserId, scope);

        Page<Incident> page = incidentRepository.findAll(
                filterSpec.and(IncidentSpecifications.orderBy(sortField, direction)),
                pageable);

        List<IncidentSummaryResponse> content = page.getContent().stream()
                .map(IncidentSummaryResponse::from)
                .toList();

        return new IncidentListResponse(
                PagedResponse.of(content, page),
                countByStatus(filterSpec));
    }

    // Header counters (CU-LST-05): the 5 OPEN states, over the same filters as the page.
    private Map<IncidentStatus, Long> countByStatus(Specification<Incident> filterSpec) {
        List<IncidentStatus> headerStatuses = List.of(
                IncidentStatus.NEW,
                IncidentStatus.ASSIGNED,
                IncidentStatus.IN_PROGRESS,
                IncidentStatus.PAUSED,
                IncidentStatus.RESOLVED);

        Map<IncidentStatus, Long> counters = new LinkedHashMap<>();
        for (IncidentStatus status : headerStatuses) {
            counters.put(status,
                    incidentRepository.count(filterSpec.and(IncidentSpecifications.hasStatus(status))));
        }
        return counters;
    }

    @Transactional(readOnly = true)
    public Incident getOwnedByAccountOr404(Long incidentId, AuthUser user) {
        return incidentRepository
                .findByIdAndAccount_Id(incidentId, user.accountId())
                .orElseThrow(() -> new NotFoundException("Incidencia no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<GuestIncidentSummaryResponse> listByLodging(Long lodgingId) {
        return incidentRepository.findByLodging_IdOrderByOpenedAtDesc(lodgingId)
                .stream()
                .map(GuestIncidentSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Incident getOwnedByLodgingOr404(Long incidentId, Long lodgingId) {
        return incidentRepository.findByIdAndLodging_IdWithImages(incidentId, lodgingId)
                .orElseThrow(() -> new NotFoundException("Incidencia no encontrada"));
    }

    // ---------- Alta por teléfono ----------

    @Transactional
    public IncidentResponse registerPhoneIncident(CreatePhoneIncidentRequest req, AuthUser user) {
        LocalDateTime now = LocalDateTime.now(clock);

        Lodging lodging = lodgingRepository
                .findByIdAndAccount_Id(req.lodgingId(), user.accountId())
                .orElseThrow(() -> new NotFoundException("Alojamiento no encontrado"));

        if (!lodging.isActive()) {
            throw new ConflictException("El alojamiento no está activo");
        }

        AppUser assignee = null;
        if (req.assigneeId() != null) {
            assignee = userRepository
                    .findByIdAndAccount_Id(req.assigneeId(), user.accountId())
                    .filter(u -> u.isActive() && u.getRole() == UserRole.OPERATOR)
                    .orElseThrow(() -> new ConflictException("Operario no válido"));
        }

        IncidentStatus status = (assignee == null)
                ? IncidentStatus.NEW
                : IncidentStatus.ASSIGNED;

        Account account = lodging.getAccount();
        int year = req.openedAt().getYear();
        String code = nextIncidentCode(account, year);

        Incident incident = Incident.builder()
                .account(account)
                .code(code)
                .source(IncidentSource.PHONE)
                .status(status)
                .priority(req.priority() != null ? req.priority() : IncidentPriority.NORMAL)
                .category(req.category())
                .lodging(lodging)
                .title(buildTitle(req.description()))
                .description(req.description())
                .guestFirstName(req.firstName())
                .guestLastName(req.lastName())
                .guestContact(normalizeContact(req.contact()))
                .assignee(assignee)
                .openedAt(req.openedAt())
                .createdAt(now)
                .assignedAt(assignee != null ? now : null)
                .build();

        Incident saved = incidentRepository.save(incident);

        AppUser actorEntity = userRepository.getReferenceById(user.userId());
        incidentHistoryService.record(saved, actorEntity, IncidentEventType.CREATED,
                null, status.name(), now);
        if (assignee != null) {
            incidentHistoryService.record(saved, actorEntity, IncidentEventType.ASSIGNED,
                    null, assignee.getId().toString(), now);
        }

        return IncidentResponse.from(saved);
    }

    // ---------- CU-INC-04: clasificar (categoría + prioridad) ----------

    @Transactional
    public IncidentResponse classify(Long incidentId, ClassifyIncidentRequest request, AuthUser user) {
        Incident incident = getOwnedByAccountOr404(incidentId, user);
        ensureNotTerminal(incident);
        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actorEntity = userRepository.getReferenceById(user.userId());

        IncidentCategory oldCategory = incident.getCategory();
        if (oldCategory != request.category()) {
            incident.setCategory(request.category());
            incidentHistoryService.record(incident, actorEntity, IncidentEventType.CATEGORY_CHANGED,
                    nameOrNull(oldCategory), request.category().name(), now);
        }

        IncidentPriority oldPriority = incident.getPriority();
        if (oldPriority != request.priority()) {
            incident.setPriority(request.priority());
            incidentHistoryService.record(incident, actorEntity, IncidentEventType.PRIORITY_CHANGED,
                    nameOrNull(oldPriority), request.priority().name(), now);
        }

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    // ---------- CU-INC-05: marcar como urgente ----------

    @Transactional
    public IncidentResponse markUrgent(Long incidentId, AuthUser user) {
        Incident incident = getOwnedByAccountOr404(incidentId, user);
        ensureNotTerminal(incident);

        IncidentPriority oldPriority = incident.getPriority();
        if (oldPriority != IncidentPriority.URGENT) {
            incident.setPriority(IncidentPriority.URGENT);
            AppUser actorEntity = userRepository.getReferenceById(user.userId());
            incidentHistoryService.record(incident, actorEntity, IncidentEventType.PRIORITY_CHANGED,
                    nameOrNull(oldPriority), IncidentPriority.URGENT.name(), LocalDateTime.now(clock));
            incidentRepository.save(incident);
        }
        return IncidentResponse.from(incident);
    }

    // ---------- Corregir título / descripción ----------

    @Transactional
    public IncidentResponse correctText(Long incidentId, CorrectIncidentTextRequest request, AuthUser user) {
        Incident incident = getOwnedByAccountOr404(incidentId, user);
        ensureNotTerminal(incident);
        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actorEntity = userRepository.getReferenceById(user.userId());

        String oldDescription = incident.getDescription();
        if (!request.description().equals(oldDescription)) {
            incident.setDescription(request.description());
            incidentHistoryService.record(incident, actorEntity, IncidentEventType.DESCRIPTION_CHANGED,
                    oldDescription, request.description(), now);
        }

        String newTitle = resolveTitle(request.title(), request.description());
        String oldTitle = incident.getTitle();
        if (!newTitle.equals(oldTitle)) {
            incident.setTitle(newTitle);
            incidentHistoryService.record(incident, actorEntity, IncidentEventType.TITLE_CHANGED,
                    oldTitle, newTitle, now);
        }

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    // ---------- CU-INC-08: rechazar incidencia ----------

    @Transactional
    public IncidentResponse reject(Long incidentId, RejectIncidentRequest request, AuthUser user) {
        Incident incident = getOwnedByAccountOr404(incidentId, user);

        IncidentStatus current = incident.getStatus();
        ensureNotTerminal(incident);
        if (current == IncidentStatus.RESOLVED) {
            throw new ConflictException("No se puede rechazar una incidencia ya resuelta");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actor = userRepository.getReferenceById(user.userId());

        incident.setStatus(IncidentStatus.REJECTED);
        incident.setRejectionReason(request.reason().strip());

        incidentHistoryService.record(incident, actor, IncidentEventType.STATUS_CHANGED,
                current.name(), IncidentStatus.REJECTED.name(), now);

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    // ---------- autoassign from the pool ----------
    @Transactional
    public IncidentResponse claim(Long incidentId, AuthUser user) {
        Incident incident = incidentRepository
                .findByIdAndAccount_IdForUpdate(incidentId, user.accountId())
                .orElseThrow(() -> new NotFoundException("Incidencia no encontrada"));

        if (incident.getAssignee() != null) {
            throw new ConflictException("Esta incidencia ya ha sido asignada");
        }
        if (incident.getStatus() != IncidentStatus.NEW) {
            throw new ConflictException("La incidencia no está disponible en el pool");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AppUser operator = userRepository.getReferenceById(user.userId());

        incident.setAssignee(operator);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setAssignedAt(now);

        incidentHistoryService.record(incident, operator, IncidentEventType.ASSIGNED,
                null, operator.getId().toString(), now);

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    // ---------- Alta desde el portal del huésped ----------

    @Transactional
    public GuestIncidentResponse registerGuestIncident(CreateGuestIncidentRequest req, GuestPrincipal guest) {
        LocalDateTime now = LocalDateTime.now(clock);

        Lodging lodging = lodgingRepository.findById(guest.lodgingId())
                .filter(Lodging::isActive)
                .orElseThrow(() -> new NotFoundException("Alojamiento no encontrado"));

        Account account = lodging.getAccount();
        String code = nextIncidentCode(account, now.getYear());

        Incident incident = Incident.builder()
                .account(account)
                .code(code)
                .source(IncidentSource.GUEST_PORTAL)
                .status(IncidentStatus.NEW)
                .priority(IncidentPriority.NORMAL)
                .category(req.category())
                .lodging(lodging)
                .title(buildTitle(req.description()))
                .description(req.description())
                .guestFirstName(req.firstName())
                .guestLastName(req.lastName())
                .guestContact(normalizeContact(req.contact()))
                .openedAt(now)
                .createdAt(now)
                .build();

        incident.setImages(incidentImageService.buildImages(req.images(), incident, now));
        Incident saved = incidentRepository.save(incident);

        incidentHistoryService.record(saved, null, IncidentEventType.CREATED,
                null, IncidentStatus.NEW.name(), now);

        return GuestIncidentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getDetail(Long incidentId, AuthUser user) {
        return IncidentResponse.from(getOwnedByAccountOr404(incidentId, user));
    }

    // ---------- Helpers privados ----------

    private String nextIncidentCode(Account account, int year) {
        IncidentCounter counter = incidentCounterRepository
                .findForUpdate(account.getId(), year)
                .orElseGet(() -> incidentCounterRepository.save(
                        IncidentCounter.builder()
                                .account(account)
                                .year(year)
                                .lastNumber(0)
                                .build()));

        int next = counter.getLastNumber() + 1;
        counter.setLastNumber(next);
        return String.format("INC-%d-%06d", year, next);
    }

    private String buildTitle(String description) {
        String trimmed = description.strip();
        return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 80);
    }

    private String resolveTitle(String title, String description) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        return buildTitle(description);
    }

    private String normalizeContact(String contact) {
        if (contact == null)
            return null;
        String t = contact.strip();
        return t.isEmpty() ? null : t;
    }

    private static String nameOrNull(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private void ensureNotTerminal(Incident incident) {
        IncidentStatus status = incident.getStatus();
        if (status == IncidentStatus.CLOSED || status == IncidentStatus.REJECTED) {
            throw new ConflictException("La incidencia está cerrada");
        }
    }
}