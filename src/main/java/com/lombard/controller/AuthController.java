package com.lombard.controller;

import com.lombard.controller.dto.RegisterForm;
import com.lombard.model.Client;
import com.lombard.model.User;
import com.lombard.service.ClientService;
import com.lombard.service.UserService;
import jakarta.validation.Valid;
import com.lombard.util.TransactionUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

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
    public String register(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    @Transactional
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult result,
                           Model model) {

        if (result.hasErrors()) {
            model.addAttribute("error", result.getFieldError() != null
                    ? result.getFieldError().getDefaultMessage()
                    : "Ошибка валидации");
            return "register";
        }

        String normalizedPhone = form.getPhone().trim();

        if (clientService.existsByPhone(normalizedPhone)) {
            model.addAttribute("error", "Клиент с таким телефоном уже зарегистрирован");
            return "register";
        }
        if (form.getEmail() != null && !form.getEmail().isBlank()
                && clientService.existsByEmail(form.getEmail().trim())) {
            model.addAttribute("error", "Клиент с таким email уже зарегистрирован");
            return "register";
        }

        try {
            User user = userService.register(form.getUsername().trim(), form.getPassword(), "CLIENT");

            Client client = new Client();
            client.setLastName(form.getLastName().trim());
            client.setFirstName(form.getFirstName().trim());
            client.setMiddleName(form.getMiddleName() == null || form.getMiddleName().isBlank()
                    ? null : form.getMiddleName().trim());
            client.setEmail(form.getEmail() != null && !form.getEmail().isBlank()
                    ? form.getEmail().trim() : null);
            client.setPhone(normalizedPhone);
            client.setUser(user);
            clientService.save(client);

        } catch (RuntimeException e) {
            // откатываю регистрацию целиком, чтобы не остался пользователь без профиля клиента
            TransactionUtils.rollbackCurrent();
            model.addAttribute("error", e.getMessage());
            return "register";
        }
        return "redirect:/login";
    }
}