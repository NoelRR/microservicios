package com.toby.menu.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "platos")
public class Plato {

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
    private String categoria; // HAMBURGUESA, BEBIDA, ACOMPANAMIENTO, POSTRE

    private String imagenUrl;

    // activo = el admin lo publica/oculta manualmente
    @Column(nullable = false)
    private boolean activo = true;

    // disponible = hay stock de ingredientes (lo maneja inventario via RabbitMQ)
    @Column(nullable = false)
    private boolean disponible = true;

    public Plato() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
}
