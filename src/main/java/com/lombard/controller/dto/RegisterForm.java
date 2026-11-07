package com.lombard.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterForm {

    @NotBlank(message = "Логин обязателен")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Только латиница, цифры, _")
    @Size(min = 3, max = 30)
    private String username;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, max = 50, message = "Пароль от 6 до 50 символов")
    private String password;

    @NotBlank(message = "Фамилия обязательна")
    @Pattern(regexp = "[А-ЯЁа-яёA-Za-z][А-ЯЁа-яёA-Za-z\\-]+", message = "Только буквы, минимум 2")
    private String lastName;

    @NotBlank(message = "Имя обязательно")
    @Pattern(regexp = "[А-ЯЁа-яёA-Za-z][А-ЯЁа-яёA-Za-z\\-]+", message = "Только буквы, минимум 2")
    private String firstName;

    // отчество необязательное: пустая строка тоже подходит
    @Pattern(regexp = "([А-ЯЁа-яёA-Za-z][А-ЯЁа-яёA-Za-z\\-]+)?", message = "Отчество: только буквы, минимум 2")
    private String middleName;

    @NotBlank
    @Email(message = "Некорректный email")
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\+7\\d{10}$", message = "Формат +7XXXXXXXXXX")
    private String phone;

    // getters / setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}