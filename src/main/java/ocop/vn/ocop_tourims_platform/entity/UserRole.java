package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@BatchSize(size = 100)
@Table(name = "user_roles", uniqueConstraints =
        @UniqueConstraint(columnNames = {"user_ref_id", "role_ref_id"}))
public class UserRole extends BaseEntity {

    @Generated
    @ColumnDefault("'URL-' || nextval('user_roles_user_role_id_seq')::text")
    @Column(name = "user_role_id", nullable = false, unique = true,
            insertable = false, updatable = false)
    private String userRoleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_ref_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_ref_id", nullable = false, updatable = false)
    private Role role;

    @Column(name = "assigned_by_user_id")
    private UUID assignedByUserId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt = Instant.now();

    protected UserRole() {
    }

    UserRole(User user, Role role, User assignedBy) {
        this.user = user;
        this.role = role;
        this.assignedByUserId = assignedBy == null ? null : assignedBy.getId();
    }
}
