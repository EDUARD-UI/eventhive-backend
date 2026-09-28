package com.eventhive.app.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Mantener solamente un hilo normalmente activo.
        executor.setCorePoolSize(1);

        // Máximo de 2 correos enviándose simultáneamente.
        executor.setMaxPoolSize(2);

        // Evita acumular cientos de tareas en memoria.
        executor.setQueueCapacity(50);

        executor.setThreadNamePrefix("email-");

        // Si la cola está llena, ejecuta la tarea en el hilo
        // que realizó la llamada en lugar de crear más memoria/hilos.
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.initialize();

        return executor;
    }
}
