package Sistema.Reservas.Aulas.Service;

import Sistema.Reservas.Aulas.Dto.ReservaEntrada;
import Sistema.Reservas.Aulas.Dto.ReservaSalida;
import Sistema.Reservas.Aulas.Excepcion.RecursoNoEncontradoException;
import Sistema.Reservas.Aulas.Excepcion.SolicitudInvalidaException;
import Sistema.Reservas.Aulas.Model.Aula;
import Sistema.Reservas.Aulas.Model.Curso;
import Sistema.Reservas.Aulas.Model.Reserva;
import Sistema.Reservas.Aulas.Model.Usuario;
import Sistema.Reservas.Aulas.Repository.AulaRepository;
import Sistema.Reservas.Aulas.Repository.CursoRepository;
import Sistema.Reservas.Aulas.Repository.ReservaRepository;
import Sistema.Reservas.Aulas.Repository.UsuarioRepository;
import Sistema.Reservas.Aulas.Util.Paginacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final AulaRepository aulaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;

    public ReservaService(ReservaRepository reservaRepository, AulaRepository aulaRepository,
                          UsuarioRepository usuarioRepository, CursoRepository cursoRepository) {
        this.reservaRepository = reservaRepository;
        this.aulaRepository = aulaRepository;
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
    }

    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }

    private static final Sort ORDEN = Sort.by(Sort.Order.desc("fecha"), Sort.Order.asc("horaInicio"));

    /** Búsqueda paginada por aula, usuario o curso; sin texto devuelve todas. */
    public Page<Reserva> buscar(String q, int page, int size) {
        String texto = Paginacion.limpiar(q);

        if (texto.isEmpty()) {
            return Paginacion.consultar(page, size, ORDEN, p -> reservaRepository.findAll(p));
        }
        return Paginacion.consultar(page, size, ORDEN, p -> reservaRepository.buscar(texto, p));
    }

    /** Reservas de un aula, paginadas. */
    public Page<Reserva> buscarPorAula(Long aulaId, int page, int size) {
        return Paginacion.consultar(page, size, ORDEN, p -> reservaRepository.findByAulaId(aulaId, p));
    }

    public Optional<Reserva> buscarPorId(Long id) {
        return reservaRepository.findById(id);
    }

    public List<Reserva> buscarPorFecha(java.time.LocalDate fecha) {
        return reservaRepository.findByFecha(fecha);
    }

    public List<Reserva> buscarPorAula(Long aulaId) {
        return reservaRepository.findByAulaId(aulaId);
    }

    public Reserva guardar(Reserva reserva) {

        boolean existe;

        if (reserva.getId() == null) {
            existe = reservaRepository
                    .existsByAulaIdAndFechaAndHoraInicioLessThanAndHoraFinGreaterThan(
                            reserva.getAula().getId(),
                            reserva.getFecha(),
                            reserva.getHoraFin(),
                            reserva.getHoraInicio()
                    );
        } else {
            // Edición: se ignora la propia reserva al buscar cruces de horario
            existe = reservaRepository
                    .existsByAulaIdAndFechaAndHoraInicioLessThanAndHoraFinGreaterThanAndIdNot(
                            reserva.getAula().getId(),
                            reserva.getFecha(),
                            reserva.getHoraFin(),
                            reserva.getHoraInicio(),
                            reserva.getId()
                    );
        }

        if (existe) {
            throw new IllegalArgumentException(
                    "El aula ya está reservada en ese horario."
            );
        }

        return reservaRepository.save(reserva);
    }

    public void eliminar(Long id) {
        reservaRepository.deleteById(id);
    }

    // ==================== Operaciones de la API REST (/api/reservas) ====================

    @Transactional(readOnly = true)
    public List<ReservaSalida> consultarTodas() {
        return reservaRepository.findAll().stream().map(ReservaSalida::desde).toList();
    }

    /** @throws RecursoNoEncontradoException si no existe (HTTP 404) */
    @Transactional(readOnly = true)
    public ReservaSalida consultarPorId(Long id) {
        return ReservaSalida.desde(buscarOLanzar(id));
    }

    @Transactional(readOnly = true)
    public List<ReservaSalida> consultarPorFecha(LocalDate fecha) {
        return reservaRepository.findByFecha(fecha).stream().map(ReservaSalida::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaSalida> consultarPorAula(Long aulaId) {
        return reservaRepository.findByAulaId(aulaId).stream().map(ReservaSalida::desde).toList();
    }

    /** @throws SolicitudInvalidaException horario incoherente, relación inexistente o aula ocupada (HTTP 400) */
    @Transactional
    public ReservaSalida crear(ReservaEntrada entrada) {
        Reserva reserva = new Reserva();
        aplicar(reserva, entrada);
        return ReservaSalida.desde(guardarValidando(reserva));
    }

    @Transactional
    public ReservaSalida actualizar(Long id, ReservaEntrada entrada) {
        Reserva reserva = buscarOLanzar(id);
        aplicar(reserva, entrada);
        return ReservaSalida.desde(guardarValidando(reserva));
    }

    @Transactional
    public void eliminarPorId(Long id) {
        reservaRepository.delete(buscarOLanzar(id));
    }

    private Reserva buscarOLanzar(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una reserva con id " + id));
    }

    /** Aula ocupada en ese horario: guardar() lanza IllegalArgumentException y aquí pasa a 400. */
    private Reserva guardarValidando(Reserva reserva) {
        try {
            return guardar(reserva);
        } catch (IllegalArgumentException e) {
            throw new SolicitudInvalidaException(e.getMessage());
        }
    }

    /** Valida la entrada y copia los datos a la reserva (aula, usuario y curso deben existir). */
    private void aplicar(Reserva reserva, ReservaEntrada e) {
        if (e == null) {
            throw new SolicitudInvalidaException("Debe enviar los datos de la reserva");
        }
        Map<String, String> errores = new LinkedHashMap<>();

        if (e.fecha() == null) {
            errores.put("fecha", "La fecha es obligatoria");
        } else if (e.fecha().isBefore(LocalDate.now())) {
            errores.put("fecha", "La fecha no puede ser anterior a hoy");
        }
        if (e.horaInicio() == null) {
            errores.put("horaInicio", "La hora de inicio es obligatoria");
        }
        if (e.horaFin() == null) {
            errores.put("horaFin", "La hora de finalización es obligatoria");
        } else if (e.horaInicio() != null && !e.horaFin().isAfter(e.horaInicio())) {
            errores.put("horaFin", "La hora de finalización debe ser posterior a la hora de inicio");
        }

        Aula aula = e.aulaId() == null ? null : aulaRepository.findById(e.aulaId()).orElse(null);
        if (aula == null) {
            errores.put("aulaId", "No existe un aula con id " + e.aulaId());
        }
        Usuario usuario = e.usuarioId() == null ? null : usuarioRepository.findById(e.usuarioId()).orElse(null);
        if (usuario == null) {
            errores.put("usuarioId", "No existe un usuario con id " + e.usuarioId());
        }
        Curso curso = e.cursoId() == null ? null : cursoRepository.findById(e.cursoId()).orElse(null);
        if (curso == null) {
            errores.put("cursoId", "No existe un curso con id " + e.cursoId());
        }

        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException("Los datos enviados no son válidos", errores);
        }
        reserva.setAula(aula);
        reserva.setUsuario(usuario);
        reserva.setCurso(curso);
        reserva.setFecha(e.fecha());
        reserva.setHoraInicio(e.horaInicio());
        reserva.setHoraFin(e.horaFin());
    }
}
