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

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("client", new Client());
        return "admin_client_new";
    }

    @PostMapping("/new")
    public String create(@RequestParam String fullName,
                         @RequestParam(required = false) String email,
                         @RequestParam(required = false) String phone,
                         RedirectAttributes ra) {

        if (fullName == null || fullName.trim().isEmpty()) {
            ra.addFlashAttribute("error", "ФИО обязательно");
            return "redirect:/admin/clients/new";
        }
        if (fullName.trim().split("\\s+").length < 2) {
            ra.addFlashAttribute("error", "ФИО должно содержать минимум имя и фамилию");
            return "redirect:/admin/clients/new";
        }
        if (phone == null || phone.isEmpty()) {
            ra.addFlashAttribute("error", "Телефон обязателен");
            return "redirect:/admin/clients/new";
        }
        if (!phone.matches("^\\+7\\d{10}$")) {
            ra.addFlashAttribute("error", "Телефон в формате +7XXXXXXXXXX");
            return "redirect:/admin/clients/new";
        }

        if (clientService.existsByPhone(phone.trim())) {
            ra.addFlashAttribute("error", "Клиент с таким телефоном уже существует");
            return "redirect:/admin/clients/new";
        }
        if (email != null && !email.isBlank() && clientService.existsByEmail(email.trim())) {
            ra.addFlashAttribute("error", "Клиент с таким email уже существует");
            return "redirect:/admin/clients/new";
        }

        Client client = new Client();
        client.setFullName(fullName.trim());
        client.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        client.setPhone(phone.trim());
        clientService.save(client);

        ra.addFlashAttribute("success", "Клиент «" + client.getFullName() + "» создан");
        return "redirect:/client/search";
    }

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
                           @RequestParam String fullName,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String phone,
                           RedirectAttributes ra) {
        Client client = clientService.findById(id);
        if (client == null) {
            ra.addFlashAttribute("error", "Клиент не найден");
            return "redirect:/client/search";
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            ra.addFlashAttribute("error", "ФИО обязательно");
            return "redirect:/admin/clients/" + id + "/edit";
        }
        if (fullName.trim().split("\\s+").length < 2) {
            ra.addFlashAttribute("error", "ФИО должно содержать минимум имя и фамилию");
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

        client.setFullName(fullName.trim());
        client.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        client.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
        clientService.save(client);

        ra.addFlashAttribute("success", "Данные клиента обновлены");
        return "redirect:/client/search";
    }
}