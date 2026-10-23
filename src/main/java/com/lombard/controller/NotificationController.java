package com.lombard.controller;

import com.lombard.model.User;
import com.lombard.service.NotificationService;
import com.lombard.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService,
                                  UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    private User currentUser(Authentication auth) {
        if (auth == null) return null;
        return userService.findByUsername(auth.getName());
    }

    // ===== КЛИЕНТ =====

    @GetMapping("/client/notifications")
    public String clientNotifications(@RequestParam(defaultValue = "unread") String tab,
                                      Model model, Authentication auth, RedirectAttributes ra) {
        return renderNotifications(tab, model, auth, ra, false);
    }

    @PostMapping("/client/notifications/{id}/read")
    public String clientMarkOne(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User user = currentUser(auth);
        if (user != null && notificationService.markOneRead(id, user.getId())) {
            ra.addFlashAttribute("success", "Уведомление отмечено прочитанным");
        }
        return "redirect:/client/notifications?tab=unread";
    }

    @PostMapping("/client/notifications/mark-read")
    public String clientMarkRead(Authentication auth, RedirectAttributes ra) {
        User user = currentUser(auth);
        if (user != null) {
            notificationService.markAllRead(user.getId());
        }
        ra.addFlashAttribute("success", "Все уведомления прочитаны");
        return "redirect:/client/notifications?tab=read";
    }

    // ===== МЕНЕДЖЕР / АДМИН =====

    @GetMapping("/admin/notifications")
    public String adminNotifications(@RequestParam(defaultValue = "unread") String tab,
                                     Model model, Authentication auth, RedirectAttributes ra) {
        return renderNotifications(tab, model, auth, ra, true);
    }

    @PostMapping("/admin/notifications/{id}/read")
    public String adminMarkOne(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User user = currentUser(auth);
        if (user != null && notificationService.markOneRead(id, user.getId())) {
            ra.addFlashAttribute("success", "Уведомление отмечено прочитанным");
        }
        return "redirect:/admin/notifications?tab=unread";
    }

    @PostMapping("/admin/notifications/mark-read")
    public String adminMarkRead(Authentication auth, RedirectAttributes ra) {
        User user = currentUser(auth);
        if (user != null) {
            notificationService.markAllRead(user.getId());
        }
        ra.addFlashAttribute("success", "Все уведомления прочитаны");
        return "redirect:/admin/notifications?tab=read";
    }

    // ===== ОБЩИЙ МЕТОД =====

    private String renderNotifications(String tab, Model model, Authentication auth,
                                       RedirectAttributes ra, boolean isStaff) {
        User user = currentUser(auth);
        if (user == null) {
            ra.addFlashAttribute("error", "Профиль не найден");
            return "redirect:/";
        }

        long unreadCount = notificationService.countUnread(user.getId());
        long readCount = notificationService.findByUser(user.getId()).size() - unreadCount;

        java.util.List<com.lombard.model.Notification> items;
        if ("read".equals(tab)) {
            items = notificationService.findReadByUser(user.getId());
        } else {
            items = notificationService.findUnreadByUser(user.getId());
            tab = "unread";
        }

        model.addAttribute("notifications", items);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("readCount", readCount);
        model.addAttribute("tab", tab);
        model.addAttribute("isStaff", isStaff);
        return "notifications";
    }
}