package menu;

// Evento que publica inventory-service cuando cambia disponibilidad de un plato.
// disponible=false -> se acabo un ingrediente y el plato sale del menu.
// disponible=true  -> se repuso stock y el plato vuelve.
public record StockEvent(Long platoId, boolean disponible) {
}
