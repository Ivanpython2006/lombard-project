package com.lombard.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404 - если не нашел страницу или статический ресурс
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Exception ex, Model model) {
        log.warn("404: {}", ex.getMessage());
        return "error/404";
    }

    // ловлю ошибки целостности БД (например уникальные поля)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrity(DataIntegrityViolationException ex, Model model) {
        log.error("DataIntegrityViolation", ex);
        model.addAttribute("error", "Ошибка целостности данных");
        model.addAttribute("message", "Попытка сохранить некорректные данные. Проверьте правильность ввода.");
        return "error";
    }

    // неверные аргументы (валидация, парсинг)
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        log.warn("IllegalArgument: {}", ex.getMessage());
        model.addAttribute("error", "Некорректный ввод данных");
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    // неправильное состояние например пытаюсь удалить то, что нельзя
    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalState(IllegalStateException ex, Model model) {
        log.warn("IllegalState: {}", ex.getMessage());
        model.addAttribute("error", "Некорректное состояние");
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    // все остальное чтобы приложение не падало
    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        log.error("Unhandled exception", ex);
        model.addAttribute("error", "Внутренняя ошибка сервера");
        model.addAttribute("message", "Произошла непредвиденная ошибка. Попробуйте позже.");
        return "error";
    }
}