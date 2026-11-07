package com.lombard.service;

import com.lombard.model.NumberSequence;
import com.lombard.repository.NumberSequenceRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тесты генератора номеров (L1, C1, M1 ...).
 */
@ExtendWith(MockitoExtension.class)
class NumberSequenceServiceTest {

    @Mock
    private NumberSequenceRepository numberSequenceRepository;

    private NumberSequenceService numberSequenceService;

    @BeforeEach
    void setUp() {
        numberSequenceService = new NumberSequenceService(numberSequenceRepository);
    }

    // счётчик был 5 — выдаём L5, следующий будет 6
    @Test
    void nextNumber_existingSequence_returnsAndIncrements() {
        NumberSequence seq = new NumberSequence();
        seq.setPrefix("L"); seq.setNextValue(5L);
        when(numberSequenceRepository.findByPrefixForUpdate("L")).thenReturn(Optional.of(seq));
        String result = numberSequenceService.nextNumber("L", "L");
        assertThat(result).isEqualTo("L5");
        assertThat(seq.getNextValue()).isEqualTo(6L);
    }

    // для нового префикса счётчик создаётся с единицы
    @Test
    void nextNumber_newPrefix_startsFromOne() {
        when(numberSequenceRepository.findByPrefixForUpdate("A")).thenReturn(Optional.empty());
        when(numberSequenceRepository.save(any(NumberSequence.class))).thenAnswer(i -> i.getArgument(0));
        String result = numberSequenceService.nextNumber("A", "A");
        assertThat(result).isEqualTo("A1");
        verify(numberSequenceRepository, times(2)).save(any());
    }
}
