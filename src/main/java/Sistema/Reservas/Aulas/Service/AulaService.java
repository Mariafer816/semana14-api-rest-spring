package Sistema.Reservas.Aulas.Service;

import Sistema.Reservas.Aulas.Dto.AulaEntrada;
import Sistema.Reservas.Aulas.Dto.AulaSalida;
import Sistema.Reservas.Aulas.Excepcion.RecursoNoEncontradoException;
import Sistema.Reservas.Aulas.Excepcion.SolicitudInvalidaException;
import Sistema.Reservas.Aulas.Model.Aula;
import Sistema.Reservas.Aulas.Repository.AulaRepository;
import Sistema.Reservas.Aulas.Repository.ReservaRepository;
import Sistema.Reservas.Aulas.Util.Paginacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AulaService {

    private final AulaRepository aulaRepository;
    private final ReservaRepository reservaRepository;

    public AulaService(AulaRepository aulaRepository, ReservaRepository reservaRepository) {
        this.aulaRepository = aulaRepository;
        this.reservaRepository = reservaRepository;
    }

    public List<Aula> obtenerTodas() {
        return aulaRepository.findAll(Sort.by("nombre").ascending());
    }

    /** Búsqueda paginada: sin texto devuelve el listado completo. */
    public Page<Aula> buscar(String q, int page, int size) {
        String texto = Paginacion.limpiar(q);
        Sort orden = Sort.by("nombre").ascending();

        if (texto.isEmpty()) {
            return Paginacion.consultar(page, size, orden, p -> aulaRepository.findAll(p));
        }
        return Paginacion.consultar(page, size, orden,
                p -> aulaRepository.findByNombreContainingIgnoreCase(texto, p));
    }

    public Aula guardar(Aula aula) {
        return aulaRepository.save(aula);
    }

    public Aula obtenerPorId(Long id) {
        return aulaRepository.findById(id).orElse(null);
    }

    public void eliminar(Long id) {
        if (reservaRepository.existsByAulaId(id)) {
            throw new IllegalStateException(
                    "No se puede eliminar el aula porque tiene reservas asociadas.");
        }
        aulaRepository.deleteById(id);
    }

    // ==================== Operaciones de la API REST (/api/aulas) ====================

    @Transactional(readOnly = true)
    public List<AulaSalida> consultarTodas() {
        return aulaRepository.findAll(Sort.by("nombre").ascending())
                .stream().map(AulaSalida::desde).toList();
    }

    /** @throws RecursoNoEncontradoException si no existe (HTTP 404) */
    @Transactional(readOnly = true)
    public AulaSalida consultarPorId(Long id) {
        return AulaSalida.desde(buscarOLanzar(id));
    }

    /** @throws SolicitudInvalidaException si los datos no cumplen las reglas (HTTP 400) */
    @Transactional
    public AulaSalida crear(AulaEntrada entrada) {
        validar(entrada);
        Aula aula = new Aula();
        copiar(aula, entrada);
        return AulaSalida.desde(aulaRepository.save(aula));
    }

    @Transactional
    public AulaSalida actualizar(Long id, AulaEntrada entrada) {
        Aula aula = buscarOLanzar(id);
        validar(entrada);
        copiar(aula, entrada);
        return AulaSalida.desde(aulaRepository.save(aula));
    }

    /** No se elimina un aula con reservas (400); si no existe, 404. */
    @Transactional
    public void eliminarPorId(Long id) {
        Aula aula = buscarOLanzar(id);
        if (reservaRepository.existsByAulaId(id)) {
            throw new SolicitudInvalidaException("No se puede eliminar el aula porque tiene reservas asociadas.");
        }
        aulaRepository.delete(aula);
    }

    private Aula buscarOLanzar(Long id) {
        return aulaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un aula con id " + id));
    }

    private void copiar(Aula aula, AulaEntrada e) {
        aula.setNombre(e.nombre().trim());
        aula.setCapacidad(e.capacidad());
        aula.setUbicacion(e.ubicacion().trim());
        aula.setTipo(e.tipo().trim());
        aula.setDisponible(e.disponible() == null || e.disponible());   // sin valor: disponible
    }

    /** Reglas de negocio del aula (el servicio no confía en que el controlador ya validó). */
    private void validar(AulaEntrada e) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (e == null) {
            throw new SolicitudInvalidaException("Debe enviar los datos del aula");
        }
        if (e.nombre() == null || e.nombre().isBlank()) {
            errores.put("nombre", "El nombre del aula es obligatorio");
        }
        if (e.capacidad() == null || e.capacidad() < 1 || e.capacidad() > 500) {
            errores.put("capacidad", "La capacidad debe estar entre 1 y 500");
        }
        if (e.ubicacion() == null || e.ubicacion().isBlank()) {
            errores.put("ubicacion", "La ubicación es obligatoria");
        }
        if (e.tipo() == null || e.tipo().isBlank()) {
            errores.put("tipo", "Debe seleccionar un tipo de aula");
        }
        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException("Los datos enviados no son válidos", errores);
        }
    }
}
