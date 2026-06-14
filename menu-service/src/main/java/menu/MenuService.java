package menu;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {

    private final PlatoRepository platoRepository;

    public MenuService(PlatoRepository platoRepository) {
        this.platoRepository = platoRepository;
    }

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
}
