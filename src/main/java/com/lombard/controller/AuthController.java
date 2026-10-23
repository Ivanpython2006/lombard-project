package com.lombard.controller;

import com.lombard.model.Client;
import com.lombard.model.User;
import com.lombard.service.ClientService;
import com.lombard.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;
    private final ClientService clientService;

    public AuthController(UserService userService, ClientService clientService) {
        this.userService = userService;
        this.clientService = clientService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "error/403";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    @Transactional
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String firstName,
                           @RequestParam String lastName,
                           @RequestParam(required = false) String middleName,
                           @RequestParam String email,
                           @RequestParam String phone,
                           Model model) {

        String cleanPhone = phone.replaceAll("\\D", "");
        if (cleanPhone.length() != 11 || !cleanPhone.startsWith("7")) {
            model.addAttribute("error", "Телефон должен быть в формате +7XXXXXXXXXX (ровно 11 цифр, начиная с 7)");
            return "register";
        }
        String normalizedPhone = "+" + cleanPhone;

        String nameRegex = "^[А-ЯЁа-яёA-Za-z][А-ЯЁа-яёA-Za-z\\-]+$";
        String fName = firstName != null ? firstName.trim() : "";
        String lName = lastName != null ? lastName.trim() : "";
        String mName = middleName != null ? middleName.trim() : "";

        if (fName.isEmpty() || lName.isEmpty()) {
            model.addAttribute("error", "Имя и фамилия обязательны");
            return "register";
        }
        if (!fName.matches(nameRegex)) {
            model.addAttribute("error", "Имя должно содержать только буквы (без цифр и символов), минимум 2 буквы");
            return "register";
        }
        if (!lName.matches(nameRegex)) {
            model.addAttribute("error", "Фамилия должна содержать только буквы (без цифр и символов), минимум 2 буквы");
            return "register";
        }
        if (!mName.isEmpty() && !mName.matches(nameRegex)) {
            model.addAttribute("error", "Отчество должно содержать только буквы (без цифр и символов), минимум 2 буквы");
            return "register";
        }
        if (password == null || password.length() < 6) {
            model.addAttribute("error", "Пароль должен быть не короче 6 символов");
            return "register";
        }
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("error", "Логин не может быть пустым");
            return "register";
        }
        if (!username.trim().matches("^[A-Za-z0-9_]+$")) {
            model.addAttribute("error", "Логин может содержать только английские буквы, цифры и _");
            return "register";
        }

        if (clientService.existsByPhone(normalizedPhone)) {
            model.addAttribute("error", "Клиент с таким телефоном уже зарегистрирован");
            return "register";
        }
        if (email != null && !email.isBlank() && clientService.existsByEmail(email.trim())) {
            model.addAttribute("error", "Клиент с таким email уже зарегистрирован");
            return "register";
        }

        try {
            User user = userService.register(username.trim(), password, "CLIENT");

            Client client = new Client();
            client.setLastName(lName);
            client.setFirstName(fName);
            client.setMiddleName(mName.isEmpty() ? null : mName);
            client.setEmail(email != null && !email.isBlank() ? email.trim() : null);
            client.setPhone(normalizedPhone);
            client.setUser(user);
            clientService.save(client);

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
        return "redirect:/login";
    }
}