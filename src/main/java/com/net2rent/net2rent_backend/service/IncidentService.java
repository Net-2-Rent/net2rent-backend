package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.ClassifyIncidentRequest;
import com.net2rent.net2rent_backend.dto.CorrectIncidentTextRequest;
import com.net2rent.net2rent_backend.dto.IncidentResponse;
import com.net2rent.net2rent_backend.dto.request.CreatePhoneIncidentRequest;
import com.net2rent.net2rent_backend.dto.request.CreateGuestIncidentRequest;
import com.net2rent.net2rent_backend.dto.response.GuestIncidentResponse;
import com.net2rent.net2rent_backend.security.GuestPrincipal;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.Account;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.IncidentCounter;
import com.net2rent.net2rent_backend.model.IncidentHistory;
import com.net2rent.net2rent_backend.model.Lodging;
import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
import com.net2rent.net2rent_backend.model.enums.IncidentPriority;
import com.net2rent.net2rent_backend.model.enums.IncidentSource;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;
import com.net2rent.net2rent_backend.model.enums.UserRole;
import com.net2rent.net2rent_backend.repository.IncidentCounterRepository;
import com.net2rent.net2rent_backend.repository.IncidentHistoryRepository;
import com.net2rent.net2rent_backend.repository.IncidentRepository;
import com.net2rent.net2rent_backend.repository.LodgingRepository;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidentService {

    // --- Tipos de evento del historial (triage NET-66) ---
    private static final String CATEGORY_CHANGED = "CATEGORY_CHANGED";
    private static final String PRIORITY_CHANGED = "PRIORITY_CHANGED";
    private static final String TITLE_CHANGED = "TITLE_CHANGED";
    private static final String DESCRIPTION_CHANGED = "DESCRIPTION_CHANGED";

    private final IncidentRepository incidentRepository;
    private final IncidentCounterRepository incidentCounterRepository;
    private final IncidentHistoryRepository incidentHistoryRepository;
    private final LodgingRepository lodgingRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public IncidentService(IncidentRepository incidentRepository,
                           IncidentCounterRepository incidentCounterRepository,
                           IncidentHistoryRepository incidentHistoryRepository,
                           LodgingRepository lodgingRepository,
                           UserRepository userRepository,
                           Clock clock) {
        this.incidentRepository = incidentRepository;
        this.incidentCounterRepository = incidentCounterRepository;
        this.incidentHistoryRepository = incidentHistoryRepository;
        this.lodgingRepository = lodgingRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    // ---------- Lectura ----------

    @Transactional(readOnly = true)
    public List<IncidentResponse> list(AuthUser user) {
        List<Incident> incidents;

        if (UserRole.OPERATOR.name().equals(user.role())) {
            incidents = incidentRepository.findVisibleToOperator(
                    user.accountId(), user.userId());
        } else {
            incidents = incidentRepository.findByAccount_Id(user.accountId());
        }

        return incidents.stream().map(IncidentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Incident getOwnedByAccountOr404(Long incidentId, AuthUser user) {
        return incidentRepository
                .findByIdAndAccount_Id(incidentId, user.accountId())
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
        recordEvent(saved, actorEntity, "created", null, status.name(), now);
        if (assignee != null) {
            recordEvent(saved, actorEntity, "assigned", null, assignee.getId().toString(), now);
        }

        return IncidentResponse.from(saved);
    }

    // ---------- CU-INC-04: clasificar (categoría + prioridad) ----------

    @Transactional
    public IncidentResponse classify(Long incidentId, ClassifyIncidentRequest request, AuthUser user) {
        Incident incident = getOwnedByAccountOr404(incidentId, user);
        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actorEntity = userRepository.getReferenceById(user.userId());

        IncidentCategory oldCategory = incident.getCategory();
        if (oldCategory != request.category()) {
            incident.setCategory(request.category());
            recordEvent(incident, actorEntity, CATEGORY_CHANGED, nameOrNull(oldCategory),
                    request.category().name(), now);
        }

        IncidentPriority oldPriority = incident.getPriority();
        if (oldPriority != request.priority()) {
            incident.setPriority(request.priority());
            recordEvent(incident, actorEntity, PRIORITY_CHANGED, nameOrNull(oldPriority),
                    request.priority().name(), now);
        }

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    // ---------- CU-INC-05: marcar como urgente ----------

    @Transactional
    public IncidentResponse markUrgent(Long incidentId, AuthUser user) {
        Incident incident = getOwnedByAccountOr404(incidentId, user);

        IncidentPriority oldPriority = incident.getPriority();
        if (oldPriority != IncidentPriority.URGENT) {
            incident.setPriority(IncidentPriority.URGENT);
            AppUser actorEntity = userRepository.getReferenceById(user.userId());
            recordEvent(incident, actorEntity, PRIORITY_CHANGED, nameOrNull(oldPriority),
                    IncidentPriority.URGENT.name(), LocalDateTime.now(clock));
            incidentRepository.save(incident);
        }
        return IncidentResponse.from(incident);
    }

    // ---------- Corregir título / descripción ----------

    @Transactional
    public IncidentResponse correctText(Long incidentId, CorrectIncidentTextRequest request, AuthUser user) {
        Incident incident = getOwnedByAccountOr404(incidentId, user);
        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actorEntity = userRepository.getReferenceById(user.userId());

        String oldDescription = incident.getDescription();
        if (!request.description().equals(oldDescription)) {
            incident.setDescription(request.description());
            recordEvent(incident, actorEntity, DESCRIPTION_CHANGED, oldDescription,
                    request.description(), now);
        }

        String newTitle = resolveTitle(request.title(), request.description());
        String oldTitle = incident.getTitle();
        if (!newTitle.equals(oldTitle)) {
            incident.setTitle(newTitle);
            recordEvent(incident, actorEntity, TITLE_CHANGED, oldTitle, newTitle, now);
        }

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

        Incident saved = incidentRepository.save(incident);

        recordEvent(saved, null, "created", null, IncidentStatus.NEW.name(), now);

        return GuestIncidentResponse.from(saved);
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

    private void recordEvent(Incident incident, AppUser actor, String eventType,
                             String previousValue, String newValue, LocalDateTime now) {
        IncidentHistory event = IncidentHistory.builder()
                .incident(incident)
                .actor(actor)
                .eventType(eventType)
                .previousValue(previousValue)
                .newValue(newValue)
                .createdAt(now)
                .build();
        incidentHistoryRepository.save(event);
    }

    private static String nameOrNull(Enum<?> value) {
        return value == null ? null : value.name();
    }
}