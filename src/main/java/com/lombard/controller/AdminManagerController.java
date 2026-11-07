package com.lombard.controller;

import com.lombard.model.User;
import com.lombard.service.ManagerStatsService;
import com.lombard.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/managers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagerController {

    private final UserService userService;
    private final ManagerStatsService statsService;

    public AdminManagerController(UserService userService,
                                  ManagerStatsService statsService) {
        this.userService = userService;
        this.statsService = statsService;
    }

    @GetMapping
    public String list(Model model) {
        List<User> managers = userService.findByRole("MANAGER");
        model.addAttribute("managers", managers);
        return "admin_managers";
    }

    @GetMapping("/{id}")
    public String profile(@PathVariable Long id, Model model, RedirectAttributes ra) {
        User manager = userService.findById(id);
        if (manager == null || !"MANAGER".equals(manager.getRole())) {
            ra.addFlashAttribute("error", "Менеджер не найден");
            return "redirect:/admin/managers";
        }

        String username = manager.getUsername();

        model.addAttribute("manager", manager);
        model.addAttribute("statsMonth", statsService.getStatsThisMonth(username));
        model.addAttribute("statsAllTime", statsService.getStatsAllTime(username));
        model.addAttribute("history", statsService.getAllActions(username));
        return "admin_manager_profile";
    }
}