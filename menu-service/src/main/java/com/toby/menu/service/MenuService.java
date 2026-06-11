package com.toby.menu.service;

import com.toby.menu.dto.ComboRequest;
import com.toby.menu.dto.PlatoRequest;
import com.toby.menu.exception.RecursoNoEncontradoException;
import com.toby.menu.model.Combo;
import com.toby.menu.model.Plato;
import com.toby.menu.repository.ComboRepository;
import com.toby.menu.repository.PlatoRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MenuService {

    private final PlatoRepository platoRepository;
    private final ComboRepository comboRepository;

    public MenuService(PlatoRepository platoRepository, ComboRepository comboRepository) {
        this.platoRepository = platoRepository;
        this.comboRepository = comboRepository;
    }

    // ---------- PLATOS ----------

    // Menu publico: solo platos activos y con stock
    public List<Plato> listarPlatosDisponibles() {
        return platoRepository.findByActivoTrueAndDisponibleTrue();
    }

    // Vista admin: todos
    public List<Plato> listarTodos() {
        return platoRepository.findAll();
    }

    public Plato obtenerPlato(Long id) {
        return platoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plato no encontrado: " + id));
    }

    public Plato crearPlato(PlatoRequest req) {
        Plato plato = new Plato();
        aplicar(plato, req);
        return platoRepository.save(plato);
    }

    public Plato actualizarPlato(Long id, PlatoRequest req) {
        Plato plato = obtenerPlato(id);
        aplicar(plato, req);
        return platoRepository.save(plato);
    }

    public void eliminarPlato(Long id) {
        if (!platoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Plato no encontrado: " + id);
        }
        platoRepository.deleteById(id);
    }

    // Activa/desactiva publicacion (admin)
    public Plato alternarActivo(Long id) {
        Plato plato = obtenerPlato(id);
        plato.setActivo(!plato.isActivo());
        return platoRepository.save(plato);
    }

    // Lo llama el listener de RabbitMQ cuando inventario avisa stock bajo
    public void marcarDisponibilidad(Long platoId, boolean disponible) {
        platoRepository.findById(platoId).ifPresent(plato -> {
            plato.setDisponible(disponible);
            platoRepository.save(plato);
        });
    }

    private void aplicar(Plato plato, PlatoRequest req) {
        plato.setNombre(req.nombre());
        plato.setDescripcion(req.descripcion());
        plato.setPrecio(req.precio());
        plato.setCategoria(req.categoria());
        plato.setImagenUrl(req.imagenUrl());
    }

    // ---------- COMBOS ----------

    public List<Combo> listarCombos() {
        return comboRepository.findByActivoTrue();
    }

    public Combo crearCombo(ComboRequest req) {
        Combo combo = new Combo();
        combo.setNombre(req.nombre());
        combo.setDescripcion(req.descripcion());
        combo.setPrecio(req.precio());
        combo.setPlatos(resolverPlatos(req.platoIds()));
        return comboRepository.save(combo);
    }

    private Set<Plato> resolverPlatos(Set<Long> ids) {
        Set<Plato> platos = new HashSet<>();
        for (Long id : ids) {
            platos.add(obtenerPlato(id));
        }
        return platos;
    }
}
