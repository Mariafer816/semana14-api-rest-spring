package Sistema.Reservas.Aulas.Service;

import Sistema.Reservas.Aulas.Dto.UsuarioEntrada;
import Sistema.Reservas.Aulas.Dto.UsuarioSalida;
import Sistema.Reservas.Aulas.Excepcion.RecursoNoEncontradoException;
import Sistema.Reservas.Aulas.Excepcion.SolicitudInvalidaException;
import Sistema.Reservas.Aulas.Model.Usuario;
import Sistema.Reservas.Aulas.Repository.ReservaRepository;
import Sistema.Reservas.Aulas.Repository.UsuarioRepository;
import Sistema.Reservas.Aulas.Util.Paginacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class UsuarioService {

    private static final Set<String> ROLES = Set.of("DOCENTE", "ESTUDIANTE", "ADMINISTRATIVO");

    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, ReservaRepository reservaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll(Sort.by("nombre").ascending());
    }

    /** Búsqueda paginada por parte del nombre; sin texto devuelve todos. */
    public Page<Usuario> buscar(String q, int page, int size) {
        String texto = Paginacion.limpiar(q);
        Sort orden = Sort.by("nombre").ascending();

        if (texto.isEmpty()) {
            return Paginacion.consultar(page, size, orden, p -> usuarioRepository.findAll(p));
        }
        return Paginacion.consultar(page, size, orden,
                p -> usuarioRepository.findByNombreContainingIgnoreCase(texto, p));
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    /** true si ya hay otro usuario con ese correo (idActual = null al crear). */
    public boolean correoEnUso(String correo, Long idActual) {
        if (idActual == null) {
            return usuarioRepository.existsByCorreoIgnoreCase(correo);
        }
        return usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(correo, idActual);
    }

    public Usuario guardar(Usuario usuario) {
        // El formulario de usuarios no incluye la contraseña: al editar se conserva la que ya tenía.
        if (usuario.getId() != null) {
            usuarioRepository.findById(usuario.getId())
                    .ifPresent(existente -> usuario.setPassword(existente.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    public void eliminar(Long id) {
        if (reservaRepository.existsByUsuarioId(id)) {
            throw new IllegalStateException(
                    "No se puede eliminar el usuario porque tiene reservas asociadas.");
        }
        usuarioRepository.deleteById(id);
    }

    // ==================== Operaciones de la API REST (/api/usuarios) ====================

    @Transactional(readOnly = true)
    public List<UsuarioSalida> consultarTodos() {
        return usuarioRepository.findAll(Sort.by("nombre").ascending())
                .stream().map(UsuarioSalida::desde).toList();
    }

    /** @throws RecursoNoEncontradoException si no existe (HTTP 404) */
    @Transactional(readOnly = true)
    public UsuarioSalida consultarPorId(Long id) {
        return UsuarioSalida.desde(buscarOLanzar(id));
    }

    /** La contraseña no se maneja por la API: se crea desde /registro. */
    @Transactional
    public UsuarioSalida crear(UsuarioEntrada entrada) {
        validar(entrada, null);
        Usuario usuario = new Usuario();
        copiar(usuario, entrada);
        return UsuarioSalida.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioSalida actualizar(Long id, UsuarioEntrada entrada) {
        Usuario usuario = buscarOLanzar(id);   // se conserva la contraseña que ya tenía
        validar(entrada, id);
        copiar(usuario, entrada);
        return UsuarioSalida.desde(usuarioRepository.save(usuario));
    }

    /** No se elimina un usuario con reservas (400); si no existe, 404. */
    @Transactional
    public void eliminarPorId(Long id) {
        Usuario usuario = buscarOLanzar(id);
        if (reservaRepository.existsByUsuarioId(id)) {
            throw new SolicitudInvalidaException("No se puede eliminar el usuario porque tiene reservas asociadas.");
        }
        usuarioRepository.delete(usuario);
    }

    private Usuario buscarOLanzar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con id " + id));
    }

    private void copiar(Usuario usuario, UsuarioEntrada e) {
        usuario.setNombre(e.nombre().trim());
        usuario.setCorreo(e.correo().trim());
        usuario.setRol(e.rol());
    }

    /** Campos obligatorios, rol permitido y correo no repetido. */
    private void validar(UsuarioEntrada e, Long idActual) {
        if (e == null) {
            throw new SolicitudInvalidaException("Debe enviar los datos del usuario");
        }
        Map<String, String> errores = new LinkedHashMap<>();
        if (e.nombre() == null || e.nombre().isBlank()) {
            errores.put("nombre", "El nombre es obligatorio");
        }
        if (e.correo() == null || e.correo().isBlank()) {
            errores.put("correo", "El correo es obligatorio");
        } else if (correoEnUso(e.correo().trim(), idActual)) {
            errores.put("correo", "Ya existe un usuario con ese correo");
        }
        if (e.rol() == null || !ROLES.contains(e.rol())) {
            errores.put("rol", "El rol debe ser DOCENTE, ESTUDIANTE o ADMINISTRATIVO");
        }
        if (!errores.isEmpty()) {
            throw new SolicitudInvalidaException("Los datos enviados no son válidos", errores);
        }
    }
}
