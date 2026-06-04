package com.example.demo.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.CategoriaDTO;
import com.example.demo.dto.CategoriaEventosDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.model.Categoria;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.EventoRepository;
import com.example.demo.utils.Utilidades;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceCategoria {

    private final CategoriaRepository categoriaRepository;
    private final EventoRepository    eventoRepository;

    @Value("${upload.path:uploads/categorias}")
    private String uploadPath;

    @Transactional(readOnly = true)
    public Page<Categoria> obtenerTodasCategorias(Pageable pageable) {
        return categoriaRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Categoria obtenerCategoriaPorId(String id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Categoría no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> obtenerCategoriaDTO() {
        return categoriaRepository.findAll().stream()
                .map(c -> new CategoriaDTO(c.getId(), c.getNombre()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Categoria> obtenerTop4Categorias() {
        return categoriaRepository.findTop4ByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<CategoriaEventosDTO> obtenerCategoriasConEventos() {
        return categoriaRepository.findAll().stream()
                .map(cat -> {
                    CategoriaEventosDTO dto = new CategoriaEventosDTO();
                    dto.setId(cat.getId());
                    dto.setNombre(cat.getNombre());
                    dto.setTotalEventos((int) eventoRepository.countByCategoriaId(cat.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void crearCategoria(String nombre, MultipartFile foto) throws IOException {
        if (categoriaRepository.existsByNombre(nombre))
            throw new BusinessException("Categoría ya existe");

        Categoria c = new Categoria();
        c.setNombre(nombre);

        if (foto != null && !foto.isEmpty()) {
            Utilidades.validarFoto(foto);
            c.setFoto(Utilidades.guardarFoto(foto, uploadPath));
        }

        categoriaRepository.save(c);
    }

    @Transactional
    public void actualizarCategoria(String id, String nombre, MultipartFile foto) throws IOException {
        Categoria existente = obtenerCategoriaPorId(id);

        if (!existente.getNombre().equalsIgnoreCase(nombre) && categoriaRepository.existsByNombre(nombre))
            throw new BusinessException("Ya existe una categoría con ese nombre");

        existente.setNombre(nombre);

        if (foto != null && !foto.isEmpty()) {
            Utilidades.validarFoto(foto);
            if (existente.getFoto() != null) Utilidades.eliminarFoto(existente.getFoto(), uploadPath);
            existente.setFoto(Utilidades.guardarFoto(foto, uploadPath));
        }

        categoriaRepository.save(existente);
    }

    @Transactional
    public void eliminarCategoria(String id) {
        Categoria cat = obtenerCategoriaPorId(id);
        long eventos = eventoRepository.countByCategoriaId(id);

        if (eventos > 0)
            throw new BusinessException(
                "No se puede eliminar '" + cat.getNombre() + "' porque tiene " + eventos + " evento(s)");

        if (cat.getFoto() != null) Utilidades.eliminarFoto(cat.getFoto(), uploadPath);
        categoriaRepository.deleteById(id);
    }
}