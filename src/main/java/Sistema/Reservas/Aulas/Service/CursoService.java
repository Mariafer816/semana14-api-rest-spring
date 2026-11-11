package Sistema.Reservas.Aulas.Service;

import Sistema.Reservas.Aulas.Dto.CursoEntrada;
import Sistema.Reservas.Aulas.Dto.CursoSalida;
import Sistema.Reservas.Aulas.Excepcion.RecursoNoEncontradoException;
import Sistema.Reservas.Aulas.Excepcion.SolicitudInvalidaException;
import Sistema.Reservas.Aulas.Model.Curso;
import Sistema.Reservas.Aulas.Repository.CursoRepository;
import Sistema.Reservas.Aulas.Repository.ReservaRepository;
import Sistema.Reservas.Aulas.Util.Paginacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;
    private final ReservaRepository reservaRepository;

    public CursoService(CursoRepository cursoRepository, ReservaRepository reservaRepository) {
        this.cursoRepository = cursoRepository;
        this.reservaRepository = reservaRepository;
    }

    @Transactional(readOnly = true)
    public List<Curso> listar() {
        return cursoRepository.findAll(Sort.by("nombre").ascending());
    }

    @Transactional(readOnly = true)
    public Optional<Curso> buscarOpcionalPorId(Long id) {
        return cursoRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Curso buscarPorId(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado con el ID: " + id));
    }

    @Transactional
    public Curso crear(String nombre, int horas) {
        return cursoRepository.save(new Curso(nombre, horas));
    }

    @Transactional
    public Curso actualizar(Long id, String nombre, int horas) {
        Curso curso = buscarPorId(id);
        curso.setNombre(nombre);
        curso.setHoras(horas);
        return cursoRepository.save(curso);
    }

    @Transactional
    public void eliminar(Long id) {
        Curso curso = buscarPorId(id);
        if (reservaRepository.existsByCursoId(id)) {
            throw new IllegalStateException(
                    "No se puede eliminar el curso porque tiene reservas asociadas.");
        }
        cursoRepository.delete(curso);
    }

    /** Búsqueda paginada por parte del nombre; sin texto devuelve todos. */
    @Transactional(readOnly = true)
    public Page<Curso> buscar(String q, int page, int size) {
        String texto = Paginacion.limpiar(q);
        Sort orden = Sort.by("nombre").ascending();

        if (texto.isEmpty()) {
            return Paginacion.consultar(page, size, orden, p -> cursoRepository.findAll(p));
        }
        return Paginacion.consultar(page, size, orden,
                p -> cursoRepository.findByNombreContainingIgnoreCase(texto, p));
    }

    // ==================== Operaciones de la API REST (/api/cursos) ====================

    @Transactional(readOnly = true)
    public List<CursoSalida> consultarTodos() {
        return listar().stream().map(CursoSalida::desde).toList();
    }

    /** @throws RecursoNoEncontradoException si no existe (HTTP 404) */
    @Transactional(readOnly = true)
    public CursoSalida consultarPorId(Long id) {
        return CursoSalida.desde(buscarOLanzar(id));
    }

    /** @throws SolicitudInvalidaException si los datos no cumplen las reglas (HTTP 400) */
    @Transactional
    public CursoSalida crear(CursoEntrada entrada) {
        validar(entrada);
        return CursoSalida.desde(cursoRepository.save(new Curso(entrada.nombre().trim(), entrada.horas())));
    }

    @Transactional
    public CursoSalida actualizar(Long id, CursoEntrada entrada) {
        Curso curso = buscarOLanzar(id);
        validar(entrada);
        curso.setNombre(entrada.nombre().trim());
        curso.setHoras(entrada.horas());
        return CursoSalida.desde(cursoRepository.save(curso));
    }

    /** No se elimina un curso con reservas (400); si no existe, 404. */
    @Transactional
    public void eliminarPorId(Long id) {
        Curso curso = buscarOLanzar(id);
        if (reservaRepository.existsByCursoId(id)) {
            throw new SolicitudInvalidaException("No se puede eliminar el curso porque tiene reservas asociadas.");
        }
        cursoRepository.delete(curso);
    }

    private Curso buscarOLanzar(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un curso con id " + id));
    }

    private void validar(CursoEntrada e) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (e == null) {
            throw new SolicitudInvalidaException("Debe enviar los datos del curso");
        }
        if (e.nombre() == null || e.nombre().isBlank()) {
            errores.put("nombre", "El nombre del curso es obligatorio");
        }
        if (e.horas() == null || e.horas() < 1) {
            errores.put("horas", "Las horas deben ser al menos 1");
        }
        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException("Los datos enviados no son válidos", errores);
        }
    }
}
