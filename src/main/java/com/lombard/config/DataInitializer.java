package com.lombard.config;

import com.lombard.model.NumberSequence;
import com.lombard.repository.NumberSequenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class); //логгер для вывода ифнормации в консоль

    // Список префиксов для генерации уникальных номеров (пользователей клиентов займов)
    private static final List<String> PREFIXES = List.of(
            "A", // Номера администраторов (Admin)
            "M_USER", // Номера менеджеров (Manager)
            "U_C", // Номера пользователей-клиентов (User Client)
            "C", // Номера самих клиентов в базе (Client)
            "L", // Номера займов (Loan)
            "M_ITEM", // Номера залоговых вещей (Item)
            "O" // Номера заказов (Order)
    );

    @Bean
    public CommandLineRunner initNumberSequences(NumberSequenceRepository repo) {
        return args -> {
            int created = 0; // Счетчик созданных последовательностей

            for (String prefix : PREFIXES) { //проход по каждому префиксу из списка
                if (repo.findById(prefix).isEmpty()) { // Если в базе нет записи с таким префиксом
                    NumberSequence seq = new NumberSequence(); //создал новую последовательность
                    seq.setPrefix(prefix);
                    seq.setNextValue(1L); // начало счет с единицы
                    repo.save(seq); // сохранение в БД
                    created++;
                }
            }

            if (created > 0) { // Если были созданы новые записи - пишу об этом в лог
                log.info("DataInitializer: создано {} счетчиков в number_sequences", created);
            }
        };
    }
}