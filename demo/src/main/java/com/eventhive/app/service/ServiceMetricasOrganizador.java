package com.eventhive.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventhive.app.exception.ResourceNotFoundException;
import com.eventhive.app.model.Usuario;
import com.eventhive.app.repository.EventoRepository;
import com.eventhive.app.repository.SeguidorRepository;
import com.eventhive.app.repository.UsuarioRepository;
import com.eventhive.app.repository.ValoracionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceMetricasOrganizador {

    private final UsuarioRepository    usuarioRepository;
    private final ValoracionRepository valoracionRepository;
    private final SeguidorRepository   seguidorRepository;
    private final EventoRepository     eventoRepository;

    @Transactional
    public void actualizarMetricasValoracion(Long organizadorId) {
        Usuario organizador = obtener(organizadorId);

        int total = (int) valoracionRepository.countByOrganizadorId(organizadorId);
        double promedio = total > 0
                ? valoracionRepository.calcularPromedioByOrganizadorId(organizadorId)
                : 0.0;

        organizador.setTotalValoraciones(total);
        organizador.setPromedioRating(Math.round(promedio * 10.0) / 10.0);
        usuarioRepository.save(organizador);
    }

    @Transactional
    public void actualizarTotalSeguidores(Long organizadorId) {
        Usuario organizador = obtener(organizadorId);
        int total = (int) seguidorRepository.countByOrganizadorId(organizadorId);
        organizador.setTotalSeguidores(total);
        usuarioRepository.save(organizador);
    }

    @Transactional
    public void actualizarTotalEventos(Long organizadorId) {
        Usuario organizador = obtener(organizadorId);
        int total = (int) eventoRepository.countByOrganizadorId(organizadorId);
        organizador.setTotalEventosCreados(total);
        usuarioRepository.save(organizador);
    }

    //obtener organizador por id
    private Usuario obtener(Long organizadorId) {
        return usuarioRepository.findById(organizadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizador no encontrado"));
    }
}