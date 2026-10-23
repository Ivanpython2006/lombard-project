package com.lombard.service;

import com.lombard.model.NumberSequence;
import com.lombard.repository.NumberSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NumberSequenceService {

    private final NumberSequenceRepository repository;

    public NumberSequenceService(NumberSequenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextNumber(String prefix, String letter) {
        NumberSequence seq = repository.findByPrefixForUpdate(prefix)
                .orElseGet(() -> {
                    NumberSequence s = new NumberSequence();
                    s.setPrefix(prefix);
                    s.setNextValue(1L);
                    return repository.save(s);
                });
        Long next = seq.getNextValue();
        seq.setNextValue(next + 1);
        repository.save(seq);
        return letter + next;
    }
}