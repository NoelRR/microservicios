package com.toby.menu.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "combos")
public class Combo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private BigDecimal precio;

    @Column(nullable = false)
    private boolean activo = true;

    // Platos que componen el combo
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "combo_platos",
        joinColumns = @JoinColumn(name = "combo_id"),
        inverseJoinColumns = @JoinColumn(name = "plato_id")
    )
    private Set<Plato> platos = new HashSet<>();

    public Combo() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public Set<Plato> getPlatos() { return platos; }
    public void setPlatos(Set<Plato> platos) { this.platos = platos; }
}
