package com.eventhive.app.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.eventhive.app.config.SupabaseStorageConfig;
import com.eventhive.app.dto.response.CategoriaDTO;
import com.eventhive.app.dto.response.CategoriaEventosDTO;
import com.eventhive.app.exception.BusinessException;
import com.eventhive.app.model.Categoria;
import com.eventhive.app.repository.CategoriaRepository;
import com.eventhive.app.repository.EventoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceCategoria {

    private final CategoriaRepository categoriaRepository;
    private final EventoRepository eventoRepository;
    private final SupabaseStorageService storageService;
    private final SupabaseStorageConfig storageConfig;

    //CONSULTAS
    @Transactional(readOnly = true)
    public Page<Categoria> obtenerTodasCategorias(Pageable pageable) {
        return categoriaRepository.findAll(pageable);
    }

    public Categoria obtenerCategoriaPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Categoría no encontrada"));
    }

    //devuelve id, nombre y foto de todas las categorias
    @Transactional(readOnly = true)
    public List<CategoriaDTO> obtenerCategoriaDTO() {
        return categoriaRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public List<CategoriaDTO> obtenerTop4Categorias() {
        return categoriaRepository.findTop4PorEventos(PageRequest.of(0, 4))
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoriaEventosDTO> obtenerCategoriasConEventos() {
        return categoriaRepository.obtenerCategoriasConCantidadEventos()
                .stream()
                .map(row -> {
                    CategoriaEventosDTO dto = new CategoriaEventosDTO();
                    dto.setId((Long) row[0]);
                    dto.setNombre((String) row[1]);
                    dto.setTotalEventos(((Long) row[2]).intValue());
                    return dto;
                })
                .toList();
    }

    //OPERACIONES CRUD
    @Transactional
    public void crearCategoria(String nombre, MultipartFile foto) {
        String nombreNormalizado = nombre.trim();

        if (categoriaRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new BusinessException("Categoría ya existe");
        }

        Categoria c = new Categoria();
        c.setNombre(nombreNormalizado);

        if (foto != null && !foto.isEmpty())
            c.setFoto(storageService.subirImagenCategoria(foto));

        categoriaRepository.save(c);
    }

    @Transactional
    public void actualizarCategoria(Long id, String nombre, MultipartFile foto) {
        String nombreNormalizado = nombre.trim();
        Categoria existente = obtenerCategoriaPorId(id);

        if (!existente.getNombre().equalsIgnoreCase(nombreNormalizado)
                && categoriaRepository.existsByNombreIgnoreCase(nombreNormalizado))
            throw new BusinessException("Ya existe una categoría con ese nombre");

        existente.setNombre(nombreNormalizado);

        if (foto != null && !foto.isEmpty()) {
            eliminarFotoAnterior(existente.getFoto());
            existente.setFoto(storageService.subirImagenCategoria(foto));
        }

        categoriaRepository.save(existente);
    }

    @Transactional
    public void eliminarCategoria(Long id) {
        Categoria cat = obtenerCategoriaPorId(id);
        long eventos = eventoRepository.countByCategoriaId(id);

        if (eventos > 0)
            throw new BusinessException(
                    "No se puede eliminar '" + cat.getNombre() + "' porque tiene " + eventos + " evento(s)");

        eliminarFotoAnterior(cat.getFoto());
        categoriaRepository.deleteById(id);
    }

    //METODOS AUXILIARES Y DE MAPEO
    private void eliminarFotoAnterior(String urlFoto) {
        if (urlFoto == null || urlFoto.isBlank()) return;
        String nombre = storageService.extraerNombreArchivo(urlFoto);
        storageService.eliminarArchivo(storageConfig.getBucketCategorias(), nombre);
    }

    public CategoriaDTO toDTO(Categoria categoria){
        CategoriaDTO dto = new CategoriaDTO();
        dto.setId(categoria.getId());
        dto.setNombre(categoria.getNombre());
        dto.setUrlFoto(categoria.getFoto());
        return dto;
    }
}