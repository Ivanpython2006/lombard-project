package com.lombard.controller;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Тесты общего обработчика ошибок: каждое исключение должно вести на свою страницу с понятным текстом

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFound_returns404Page() {
        Model model = new ExtendedModelMap();
        assertEquals("error/404",
                handler.handleNotFound(new RuntimeException("404"), model));
    }

    @Test
    void dataIntegrityViolation_returnsErrorPage() {
        Model model = new ExtendedModelMap();
        assertEquals("error",
                handler.handleDataIntegrity(new DataIntegrityViolationException("dup"), model));
        assertEquals("Ошибка целостности данных", model.getAttribute("error"));
    }

    @Test
    void illegalArgument_returnsErrorWithMessage() {
        Model model = new ExtendedModelMap();
        assertEquals("error",
                handler.handleIllegalArgument(new IllegalArgumentException("bad"), model));
        assertEquals("Некорректный ввод данных", model.getAttribute("error"));
        assertEquals("bad", model.getAttribute("message"));
    }

    @Test
    void illegalState_returnsErrorPage() {
        Model model = new ExtendedModelMap();
        assertEquals("error",
                handler.handleIllegalState(new IllegalStateException("bad state"), model));
        assertEquals("Некорректное состояние", model.getAttribute("error"));
    }

    @Test
    void otherException_returnsServerErrorPage() {
        Model model = new ExtendedModelMap();
        assertEquals("error",
                handler.handleException(new RuntimeException("boom"), model));
        assertEquals("Внутренняя ошибка сервера", model.getAttribute("error"));
    }
}