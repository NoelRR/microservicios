package com.toby.menu.controller;

import com.toby.menu.dto.ComboRequest;
import com.toby.menu.dto.PlatoRequest;
import com.toby.menu.model.Combo;
import com.toby.menu.model.Plato;
import com.toby.menu.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    // ---------- PLATOS ----------

    // Publico: menu disponible para clientes
    @GetMapping("/items")
    public List<Plato> listarDisponibles() {
        return menuService.listarPlatosDisponibles();
    }

    // Admin: ver todos (incluye ocultos/sin stock)
    @GetMapping("/items/all")
    public List<Plato> listarTodos() {
        return menuService.listarTodos();
    }

    @GetMapping("/items/{id}")
    public Plato obtener(@PathVariable Long id) {
        return menuService.obtenerPlato(id);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public Plato crear(@Valid @RequestBody PlatoRequest req) {
        return menuService.crearPlato(req);
    }

    @PutMapping("/items/{id}")
    public Plato actualizar(@PathVariable Long id, @Valid @RequestBody PlatoRequest req) {
        return menuService.actualizarPlato(id, req);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        menuService.eliminarPlato(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/items/{id}/toggle")
    public Plato alternar(@PathVariable Long id) {
        return menuService.alternarActivo(id);
    }

    // ---------- COMBOS ----------

    @GetMapping("/combos")
    public List<Combo> listarCombos() {
        return menuService.listarCombos();
    }

    @PostMapping("/combos")
    @ResponseStatus(HttpStatus.CREATED)
    public Combo crearCombo(@Valid @RequestBody ComboRequest req) {
        return menuService.crearCombo(req);
    }
}
