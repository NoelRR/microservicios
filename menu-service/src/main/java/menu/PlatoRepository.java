package menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlatoRepository extends JpaRepository<Plato, Long> {

    // Solo platos que el admin publico Y que tienen stock
    List<Plato> findByActivoTrueAndDisponibleTrue();

    List<Plato> findByCategoria(String categoria);
}
