package com.lombard.repository;

import com.lombard.model.NumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NumberSequenceRepository extends JpaRepository<NumberSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ns FROM NumberSequence ns WHERE ns.prefix = :prefix")
    Optional<NumberSequence> findByPrefixForUpdate(@Param("prefix") String prefix);
}