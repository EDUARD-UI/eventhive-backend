package com.example.demo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.config.SupabaseStorageConfig;
import com.example.demo.dto.CategoriaDTO;
import com.example.demo.dto.CategoriaEventosDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.model.Categoria;
import com.example.demo.repository.CategoriaRepository;
import com.example.demo.repository.EventoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceCategoria {

    private final CategoriaRepository    categoriaRepository;
    private final EventoRepository       eventoRepository;
    private final SupabaseStorageService storageService;
    private final SupabaseStorageConfig  storageConfig;

    @Transactional(readOnly = true)
    public Page<Categoria> obtenerTodasCategorias(Pageable pageable) {
        return categoriaRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Categoria obtenerCategoriaPorId(Long id) {
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
    public void crearCategoria(String nombre, MultipartFile foto) {
        if (categoriaRepository.existsByNombre(nombre))
            throw new BusinessException("Categoría ya existe");

        Categoria c = new Categoria();
        c.setNombre(nombre);

        if (foto != null && !foto.isEmpty())
            c.setFoto(storageService.subirImagenCategoria(foto));

        categoriaRepository.save(c);
    }

    @Transactional
    public void actualizarCategoria(Long id, String nombre, MultipartFile foto) {
        Categoria existente = obtenerCategoriaPorId(id);

        if (!existente.getNombre().equalsIgnoreCase(nombre) && categoriaRepository.existsByNombre(nombre))
            throw new BusinessException("Ya existe una categoría con ese nombre");

        existente.setNombre(nombre);

        if (foto != null && !foto.isEmpty()) {
            eliminarFotoAnterior(existente.getFoto());
            existente.setFoto(storageService.subirImagenCategoria(foto));
        }

        categoriaRepository.save(existente);
    }

    @Transactional
    public void eliminarCategoria(Long id) {
        Categoria cat = obtenerCategoriaPorId(id);
        long eventos  = eventoRepository.countByCategoriaId(id);

        if (eventos > 0)
            throw new BusinessException(
                "No se puede eliminar '" + cat.getNombre() + "' porque tiene " + eventos + " evento(s)");

        eliminarFotoAnterior(cat.getFoto());
        categoriaRepository.deleteById(id);
    }

    private void eliminarFotoAnterior(String urlFoto) {
        if (urlFoto == null || urlFoto.isBlank()) return;
        String nombre = storageService.extraerNombreArchivo(urlFoto);
        storageService.eliminarArchivo(storageConfig.getBucketCategorias(), nombre);
    }
}