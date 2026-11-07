package com.lombard.util;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class CsvExporter {

    private static final String SEP = ";";

    public static void write(HttpServletResponse response,
                             String fileName,
                             List<String[]> rows,
                             String... headers) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + fileName + "\"");

        try (PrintWriter writer = response.getWriter()) {
            // кириллица в Microsoft Excel при открытии файла
            writer.write('\ufeff');

            writer.println(String.join(SEP, headers));
            for (String[] row : rows) {
                writer.println(String.join(SEP, row));
            }
        }
    }

    public static String esc(String value) {
        if (value == null) return "";
        if (value.isEmpty()) return "";

        // Защита от формул Excel
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            value = "'" + value;
        }

        // Если в тексте есть разделитель кавычки или перенос строки то оборот в кавычки
        if (value.contains(SEP) || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }
}