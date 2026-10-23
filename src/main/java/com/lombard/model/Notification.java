package com.lombard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    @Column(name = "arg1", length = 300)
    private String arg1;

    @Column(name = "arg2", length = 300)
    private String arg2;

    @Column(name = "arg3", length = 300)
    private String arg3;

    @Column(name = "arg4", length = 300)
    private String arg4;

    @Column(name = "arg5", length = 300)
    private String arg5;

    @Column(length = 30)
    private String type;

    @Column(name = "is_read", nullable = false)
    private Boolean read = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (read == null) read = false;
    }

    public Notification() {}

    public Notification(User user, String eventType, String type, String... args) {
        this.user = user;
        this.eventType = eventType;
        this.type = type;
        this.read = false;
        this.createdAt = LocalDateTime.now();
        if (args != null) {
            if (args.length > 0) this.arg1 = args[0];
            if (args.length > 1) this.arg2 = args[1];
            if (args.length > 2) this.arg3 = args[2];
            if (args.length > 3) this.arg4 = args[3];
            if (args.length > 4) this.arg5 = args[4];
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getArg1() { return arg1; }
    public void setArg1(String arg1) { this.arg1 = arg1; }
    public String getArg2() { return arg2; }
    public void setArg2(String arg2) { this.arg2 = arg2; }
    public String getArg3() { return arg3; }
    public void setArg3(String arg3) { this.arg3 = arg3; }
    public String getArg4() { return arg4; }
    public void setArg4(String arg4) { this.arg4 = arg4; }
    public String getArg5() { return arg5; }
    public void setArg5(String arg5) { this.arg5 = arg5; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}