package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Entity
@BatchSize(size = 100)
@Table(name = "user_identities", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "provider_subject"}),
        @UniqueConstraint(columnNames = {"user_ref_id", "provider"})
})
@Check(constraints = "provider = 'GOOGLE' AND btrim(provider_subject) <> ''")
public class UserIdentity extends BaseEntity {

    @Generated
    @ColumnDefault("'UID-' || nextval('user_identities_user_identity_id_seq')::text")
    @Column(name = "user_identity_id", nullable = false, unique = true,
            insertable = false, updatable = false)
    private String userIdentityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_ref_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Provider provider;

    @Column(name = "provider_subject", nullable = false, updatable = false,
            columnDefinition = "text")
    private String providerSubject;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt = Instant.now();

    @Setter
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected UserIdentity() {
    }

    UserIdentity(User user, Provider provider, String providerSubject) {
        this.user = user;
        this.provider = provider;
        this.providerSubject = providerSubject;
    }

    public enum Provider {
        GOOGLE
    }
}
