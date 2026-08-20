package com.eventhive.app.service;

import com.eventhive.app.enums.MotivosRechazos;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceEstados {

    public List<MotivosRechazos> findMotivosRechazos(){
        return List.of(MotivosRechazos.values());
    }
}
