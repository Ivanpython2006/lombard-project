package com.lombard.controller;

import com.lombard.model.Client;
import com.lombard.service.ClientService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/clients")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminClientController {

    private final ClientService clientService;

    public AdminClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    // ===== СОЗДАНИЕ =====

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("client", new Client());
        return "admin_client_new";
    }

    @PostMapping("/new")
    public String create(@RequestParam(required = false) String lastName,
                         @RequestParam(required = false) String firstName,
                         @RequestParam(required = false) String middleName,
                         @RequestParam(required = false) String email,
                         @RequestParam(required = false) String phone,
                         RedirectAttributes ra) {

        // форма присылает ФИО по частям, собираю в одну строку
        String fullName = joinName(lastName, firstName, middleName);
        String nameError = checkName(lastName, firstName, middleName);
        if (nameError != null) {
            ra.addFlashAttribute("error", nameError);
            return "redirect:/admin/clients/new";
        }

        phone = phone != null ? phone.trim() : "";
        if (phone.isEmpty()) {
            ra.addFlashAttribute("error", "Телефон обязателен");
            return "redirect:/admin/clients/new";
        }
        if (!phone.matches("^\\+7\\d{10}$")) {
            ra.addFlashAttribute("error", "Телефон в формате +7XXXXXXXXXX");
            return "redirect:/admin/clients/new";
        }
        if (clientService.existsByPhone(phone)) {
            ra.addFlashAttribute("error", "Клиент с таким телефоном уже существует");
            return "redirect:/admin/clients/new";
        }

        email = email != null ? email.trim() : null;
        if (email != null && !email.isBlank() && clientService.existsByEmail(email)) {
            ra.addFlashAttribute("error", "Клиент с таким email уже существует");
            return "redirect:/admin/clients/new";
        }

        Client client = new Client();
        client.changeFullName(fullName);
        client.setPhone(phone);
        client.setEmail(email != null && !email.isBlank() ? email : null);
        clientService.save(client);

        ra.addFlashAttribute("success", "Клиент «" + client.getFullName() + "» создан");
        return "redirect:/client/search";
    }

    // склеиваю фамилию, имя и отчество через пробел (пустые части пропускаю)
    private static String joinName(String lastName, String firstName, String middleName) {
        StringBuilder sb = new StringBuilder();
        for (String part : new String[]{lastName, firstName, middleName}) {
            if (part != null && !part.isBlank()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(part.trim());
            }
        }
        return sb.toString();
    }

    // та же проверка, что и в форме: только буквы и дефис, минимум 2 символа
    private static String checkName(String lastName, String firstName, String middleName) {
        String namePattern = "[А-ЯЁа-яёA-Za-z][А-ЯЁа-яёA-Za-z\\-]+";
        if (lastName == null || lastName.isBlank() || firstName == null || firstName.isBlank()) {
            return "Фамилия и имя обязательны";
        }
        if (!lastName.trim().matches(namePattern) || !firstName.trim().matches(namePattern)) {
            return "Фамилия и имя: только буквы, минимум 2 символа";
        }
        if (middleName != null && !middleName.isBlank() && !middleName.trim().matches(namePattern)) {
            return "Отчество: только буквы, минимум 2 символа";
        }
        return null;
    }

    // ===== РЕДАКТИРОВАНИЕ =====

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Client client = clientService.findById(id);
        if (client == null) {
            ra.addFlashAttribute("error", "Клиент не найден");
            return "redirect:/client/search";
        }
        model.addAttribute("client", client);
        return "admin_client_edit";
    }

    @PostMapping("/{id}/edit")
    public String editSave(@PathVariable Long id,
                           @RequestParam(required = false) String lastName,
                           @RequestParam(required = false) String firstName,
                           @RequestParam(required = false) String middleName,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String phone,
                           RedirectAttributes ra) {
        Client client = clientService.findById(id);
        if (client == null) {
            ra.addFlashAttribute("error", "Клиент не найден");
            return "redirect:/client/search";
        }

        String name = joinName(lastName, firstName, middleName);
        String nameError = checkName(lastName, firstName, middleName);
        if (nameError != null) {
            ra.addFlashAttribute("error", nameError);
            return "redirect:/admin/clients/" + id + "/edit";
        }
        if (phone != null && !phone.isEmpty() && !phone.matches("^\\+7\\d{10}$")) {
            ra.addFlashAttribute("error", "Телефон в формате +7XXXXXXXXXX");
            return "redirect:/admin/clients/" + id + "/edit";
        }
        if (phone != null && !phone.isBlank()
                && clientService.existsByPhoneExceptId(phone.trim(), id)) {
            ra.addFlashAttribute("error", "Телефон уже используется другим клиентом");
            return "redirect:/admin/clients/" + id + "/edit";
        }
        if (email != null && !email.isBlank()
                && clientService.existsByEmailExceptId(email.trim(), id)) {
            ra.addFlashAttribute("error", "Email уже используется другим клиентом");
            return "redirect:/admin/clients/" + id + "/edit";
        }

        client.changeFullName(name);
        client.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        client.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
        clientService.save(client);

        ra.addFlashAttribute("success", "Данные клиента обновлены");
        return "redirect:/client/search";
    }
}