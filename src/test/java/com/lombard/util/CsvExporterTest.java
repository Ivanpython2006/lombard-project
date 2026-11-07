package com.lombard.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

//Тесты экранирования значений при выгрузке в CSV.

class CsvExporterTest {

    // значения начинающиеся с = + - @, Excel может посчитать формулой поэтому ставим апостроф
    @Test
    void esc_formulaStart_addsQuote() {
        assertThat(CsvExporter.esc("=SUM(A1)")).startsWith("'=");
        assertThat(CsvExporter.esc("+1")).startsWith("'+");
        assertThat(CsvExporter.esc("-1")).startsWith("'-");
        assertThat(CsvExporter.esc("@cmd")).startsWith("'@");
    }

    @Test
    void esc_separatorsAndQuotes_wrapsInQuotes() {
        assertThat(CsvExporter.esc("a;b")).isEqualTo("\"a;b\"");
        assertThat(CsvExporter.esc("a\"b")).isEqualTo("\"a\"\"b\"");
        assertThat(CsvExporter.esc("a\nb")).isEqualTo("\"a\nb\"");
    }

    @Test
    void esc_nullOrEmpty_returnsEmpty() {
        assertThat(CsvExporter.esc(null)).isEmpty();
        assertThat(CsvExporter.esc("")).isEmpty();
    }
}