package com.net2rent.net2rent_backend.repository.spec;

import com.net2rent.net2rent_backend.dto.request.IncidentFilter;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
import com.net2rent.net2rent_backend.model.enums.IncidentPriority;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class IncidentSpecifications {

    private IncidentSpecifications() { }

    public static Specification<Incident> inAccount(Long accountId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("id"), accountId);
    }

    public static Specification<Incident> hasStatus(IncidentStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Incident> hasPriority(IncidentPriority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Incident> hasCategory(IncidentCategory category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Incident> inLodging(Long lodgingId) {
        return (root, query, cb) -> lodgingId == null ? null : cb.equal(root.get("lodging").get("id"), lodgingId);
    }

    public static Specification<Incident> hasAssignee(Long assigneeId) {
        return (root, query, cb) -> assigneeId == null ? null : cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Incident> unassigned() {
        return (root, query, cb) -> cb.and(
                cb.isNull(root.get("assignee")),
                root.get("status").in(IncidentStatus.CLOSED, IncidentStatus.REJECTED).not()
        );
    }

    public static Specification<Incident> openedFrom(LocalDate from) {
        return (root, query, cb) -> from == null ? null
                : cb.greaterThanOrEqualTo(root.<LocalDateTime>get("openedAt"), from.atStartOfDay());
    }

    public static Specification<Incident> openedTo(LocalDate to) {
        return (root, query, cb) -> to == null ? null
                : cb.lessThanOrEqualTo(root.<LocalDateTime>get("openedAt"), to.atTime(LocalTime.MAX));
    }

    public static Specification<Incident> visibleToOperator(Long operatorUserId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("assignee").get("id"), operatorUserId),
                cb.isNull(root.get("assignee"))
        );
    }

    public static Specification<Incident> forListing(Long accountId, IncidentFilter f, Long operatorUserId) {
        List<Specification<Incident>> specs = new ArrayList<>();
        specs.add(inAccount(accountId));
        specs.add(hasStatus(f.status()));
        specs.add(hasPriority(f.priority()));
        specs.add(hasCategory(f.category()));
        specs.add(inLodging(f.lodgingId()));

        if (Boolean.TRUE.equals(f.unassigned())) {
            specs.add(unassigned());
        } else {
            specs.add(hasAssignee(f.assigneeId()));
        }

        specs.add(openedFrom(f.openedFrom()));
        specs.add(openedTo(f.openedTo()));

        if (operatorUserId != null) {
            specs.add(visibleToOperator(operatorUserId));
        }

        return specs.stream().reduce(Specification::and).orElse(null);
    }

    public static Specification<Incident> orderBy(SortField field, Sort.Direction direction) {
        return (root, query, cb) -> {
            Class<?> resultType = query.getResultType();
            boolean isCountQuery = Long.class.equals(resultType) || long.class.equals(resultType);

            if (!isCountQuery) {
                List<Order> orders = new ArrayList<>();

                if (field == SortField.PRIORITY) {
                    Expression<Integer> rank = priorityRank(root, cb);
                    orders.add(direction == Sort.Direction.ASC ? cb.desc(rank) : cb.asc(rank));
                    orders.add(cb.asc(root.get("openedAt")));
                } else { // OPENED_AT
                    Path<Object> openedAt = root.get("openedAt");
                    orders.add(direction == Sort.Direction.ASC ? cb.asc(openedAt) : cb.desc(openedAt));
                }

                orders.add(cb.asc(root.get("id")));
                query.orderBy(orders);
            }

            return null;
        };
    }

    private static Expression<Integer> priorityRank(Root<Incident> root, CriteriaBuilder cb) {
        return cb.<Integer>selectCase()
                .when(cb.equal(root.get("priority"), IncidentPriority.URGENT), 0)
                .when(cb.equal(root.get("priority"), IncidentPriority.HIGH), 1)
                .when(cb.equal(root.get("priority"), IncidentPriority.NORMAL), 2)
                .when(cb.equal(root.get("priority"), IncidentPriority.LOW), 3)
                .otherwise(4);
    }
}