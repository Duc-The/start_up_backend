package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Getter
@Entity
@BatchSize(size = 100)
@Table(name = "roles")
@Check(constraints = "code ~ '^[A-Z][A-Z0-9_]*$' AND btrim(name) <> ''")
public class Role extends BaseEntity {

    @Generated
    @ColumnDefault("'ROL-' || nextval('roles_role_id_seq')::text")
    @Column(name = "role_id", nullable = false, unique = true,
            insertable = false, updatable = false)
    private String roleId;

    @Setter
    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String code;

    @Setter
    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Setter
    @Column(columnDefinition = "text")
    private String description;

    @OneToMany(mappedBy = "role")
    @BatchSize(size = 100)
    private Set<UserRole> userRoles = new HashSet<>();

    public Set<UserRole> getUserRoles() {
        return Collections.unmodifiableSet(userRoles);
    }

    void addUserRole(UserRole userRole) {
        userRoles.add(userRole);
    }

    void removeUserRole(UserRole userRole) {
        userRoles.remove(userRole);
    }
}
