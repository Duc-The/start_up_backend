package ocop.vn.ocop_tourims_platform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.util.Objects;

@Getter
@Entity
@BatchSize(size = 100)
@Table(name = "notifications", check = {
    @CheckConstraint(constraint = "btrim(title) <> '' AND btrim(content) <> ''"),
    @CheckConstraint(constraint = "target_path LIKE '/%' AND target_path NOT LIKE '//%'")
})
public class Notification extends BaseEntity {

    @Generated
    @ColumnDefault("'NTF-' || nextval('notifications_notification_id_seq')::text")
    @Column(name = "notification_id", nullable = false, unique = true,
            insertable = false, updatable = false)
    private String notificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_ref_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "target_path", nullable = false, columnDefinition = "text")
    private String targetPath;

    @Column(name = "is_read", nullable = false)
    @ColumnDefault("false")
    private boolean read;

    protected Notification() {
    }

    public Notification(User user, String title, String content, String targetPath) {
        if (title == null || title.isBlank() || content == null || content.isBlank()
                || targetPath == null || !targetPath.startsWith("/") || targetPath.startsWith("//")) {
            throw new IllegalArgumentException("Thông báo phải có tiêu đề, nội dung và đường dẫn nội bộ hợp lệ");
        }
        this.user = Objects.requireNonNull(user, "Người dùng không được để trống");
        this.title = title;
        this.content = content;
        this.targetPath = targetPath;
    }

    public void markAsRead() {
        read = true;
    }
}
