package com.lombard.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_number", unique = true, length = 20)
    private String clientNumber;

    @NotBlank(message = "ФИО обязательно")
    @Column(name = "name", nullable = false)
    private String fullName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @NotBlank(message = "Телефон обязателен")
    @Pattern(regexp = "^\\+7\\d{10}$",
             message = "Телефон в формате +7XXXXXXXXXX (11 цифр)")
    @Column(nullable = false)
    private String phone;

    @Email(message = "Некорректный email")
    private String email;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    public Client() {}

    public Client(String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClientNumber() { return clientNumber; }
    public void setClientNumber(String clientNumber) { this.clientNumber = clientNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    @PrePersist
    @PreUpdate
    public void rebuildFullName() {
        StringBuilder sb = new StringBuilder();
        if (lastName != null && !lastName.isBlank()) sb.append(lastName.trim());
        if (firstName != null && !firstName.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(firstName.trim());
        }
        if (middleName != null && !middleName.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middleName.trim());
        }
        if (sb.length() > 0) {
            this.fullName = sb.toString();
        } else if (this.fullName == null || this.fullName.isBlank()) {
            this.fullName = "Без имени";
        }

        if (clientNumber == null || clientNumber.isBlank()) {
            clientNumber = "C_UNKNOWN";
        }
    }
}