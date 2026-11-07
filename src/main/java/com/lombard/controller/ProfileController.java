package com.lombard.controller;

import com.lombard.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/password")
    public String passwordForm() {
        return "profile_password";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth,
                                 RedirectAttributes ra) {

        // Проверка, что новый пароль не пустой
        if (newPassword == null || newPassword.length() < 6) {
            ra.addFlashAttribute("error", "Пароль должен быть не короче 6 символов");
            return "redirect:/profile/password";
        }

        // Проверка, что новый и подтверждение совпадают
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Новый пароль и подтверждение не совпадают");
            return "redirect:/profile/password";
        }

        // Проверка текущего пароля
        if (!userService.checkPassword(auth.getName(), currentPassword)) {
            ra.addFlashAttribute("error", "Текущий пароль неверный");
            return "redirect:/profile/password";
        }

        // Сохраняем новый пароль
        userService.changePassword(auth.getName(), newPassword);
        ra.addFlashAttribute("success", "Пароль успешно изменён");
        return "redirect:/profile/password";
    }
}