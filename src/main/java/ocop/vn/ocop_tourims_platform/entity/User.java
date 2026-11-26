package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;

@Getter
@Entity
@Table(
    name = "users",
    uniqueConstraints =
        @jakarta.persistence.UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@BatchSize(size = 100)
public class User extends BaseEntity {
  @Generated
  @ColumnDefault("'USR-' || nextval('users_user_id_seq')::text")
  @Column(name = "user_id", nullable = false, unique = true, insertable = false, updatable = false)
  private String userId;

  @Setter
  @Column(nullable = false)
  private String email;

  @Setter
  @Column(name = "password_hash")
  private String passwordHash;

  @Setter
  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Setter
  @Column(name = "avatar_url")
  private String avatarUrl;

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.ACTIVE;

  @Setter
  @Column(name = "email_verified_at")
  private Instant emailVerifiedAt;

  @Setter
  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  private Set<UserRole> userRoles = new HashSet<>();

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  private Set<UserIdentity> identities = new HashSet<>();

  public Set<UserRole> getUserRoles() {
    return Collections.unmodifiableSet(userRoles);
  }

  public Set<UserIdentity> getIdentities() {
    return Collections.unmodifiableSet(identities);
  }

  public UserRole addRole(Role role, User assignedBy) {
    Objects.requireNonNull(role, "role");
    boolean alreadyAssigned =
        userRoles.stream()
            .map(UserRole::getRole)
            .anyMatch(
                current ->
                    current == role
                        || (role.getId() != null && role.getId().equals(current.getId())));
    if (alreadyAssigned) {
      throw new IllegalArgumentException("Vai trò đã được gán cho người dùng");
    }

    UserRole link = new UserRole(this, role, assignedBy);
    userRoles.add(link);
    role.addUserRole(link);
    return link;
  }

  public void removeRole(UserRole link) {
    if (userRoles.remove(link)) {
      link.getRole().removeUserRole(link);
    }
  }

  public UserIdentity linkIdentity(UserIdentity.Provider provider, String subject) {
    Objects.requireNonNull(provider, "provider");
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("Mã định danh tài khoản liên kết không được trống");
    }
    if (identities.stream().anyMatch(identity -> identity.getProvider() == provider)) {
      throw new IllegalArgumentException("Phương thức đăng nhập đã được liên kết");
    }

    UserIdentity identity = new UserIdentity(this, provider, subject);
    identities.add(identity);
    return identity;
  }

  public void unlinkIdentity(UserIdentity identity) {
    identities.remove(identity);
  }

  public enum Status {
    ACTIVE,
    UNACTIVE
  }

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  private Set<UserFavoritePoi> favoritePois = new HashSet<>();

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  private Set<Notification> notifications = new HashSet<>();
}
