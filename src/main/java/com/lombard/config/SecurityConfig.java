package com.lombard.config;

import com.lombard.model.User;
import com.lombard.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { // храню CSRF-токен в куках (удобнее с формами
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookiePath("/");
        csrfRepo.setCookieName("XSRF-TOKEN");
        csrfRepo.setHeaderName("X-XSRF-TOKEN");

        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepo)
                        .csrfTokenRequestHandler(csrfHandler)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)  // сессия по необходимости
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers( // пускаю всех на логин регистрацию и к статике
                                "/login", "/register",
                                "/style.css", "/*.css", "/*.js", "/*.png", "/*.jpg", "/*.ico",
                                "/css/**", "/js/**", "/images/**",
                                "/uploads/**",
                                "/favicon.ico"
                        ).permitAll()

                        // страницы ошибок тоже открыты
                        .requestMatchers("/403", "/404", "/error", "/error/**").permitAll()

                        // разделы для админов и менеджеров
                        .requestMatchers("/admin/notifications", "/admin/notifications/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/admin/warehouse", "/admin/warehouse/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/admin/orders", "/admin/orders/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/admin/clients", "/admin/clients/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/admin/intake", "/admin/intake/**").hasAnyRole("ADMIN", "MANAGER")

                        //а это только для самого админа
                        .requestMatchers("/admin/stats", "/admin/stats/**").hasRole("ADMIN")
                        .requestMatchers("/admin/users", "/admin/users/**").hasRole("ADMIN")
                        .requestMatchers("/admin/managers", "/admin/managers/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // займы идут менеджерам и админам клиентам только просмотр своих
                        .requestMatchers("/loans/export").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/loans/{id}/approve").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/loans/{id}/status").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/loans/{id}/edit").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/loans/{id}/extend").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/loans/{id}/delete").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/loans", "/loans/").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/loans/**").authenticated()

                        .requestMatchers("/collaterals/**").hasAnyRole("ADMIN", "MANAGER") // залоги только сотрудники

                        // поиск создание редактирование и удаление клиентов только сотрудникам
                        .requestMatchers("/client/search", "/client/new", "/client/*/edit", "/client/export", "/client/*/delete")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .requestMatchers("/client/loans", "/client/loans/new").hasRole("CLIENT")
                        .requestMatchers("/client/**").hasAnyRole("ADMIN", "MANAGER", "CLIENT")

                        .requestMatchers("/support").hasRole("CLIENT") //оставшиеся
                        .requestMatchers("/profile/**").authenticated()
                        .requestMatchers("/shop", "/shop/**").authenticated()
                        .requestMatchers("/about").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex // если нет прав кидаю на 403
                        .accessDeniedPage("/403")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            Optional<User> user = userRepository.findByUsername(username);
            if (user.isPresent()) {
                User u = user.get();
                return org.springframework.security.core.userdetails.User
                        .withUsername(u.getUsername())
                        .password(u.getPassword())
                        .roles(u.getRole())
                        .build();
            } else {
                throw new UsernameNotFoundException("Пользователь не найден");
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}