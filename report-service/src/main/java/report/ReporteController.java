package report;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    // Resumen: total pedidos, ingreso total, ticket promedio.
    @GetMapping("/ventas")
    public ResumenVentas ventas() {
        return reporteService.resumenVentas();
    }

    // Platos mas vendidos (por defecto top 5).
    @GetMapping("/platos-mas-vendidos")
    public List<PlatoVendido> platosMasVendidos(@RequestParam(defaultValue = "5") int limite) {
        return reporteService.platosMasVendidos(limite);
    }
}
