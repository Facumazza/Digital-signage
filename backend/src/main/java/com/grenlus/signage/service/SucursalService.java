package com.grenlus.signage.service;

import com.grenlus.signage.dtos.SucursalRequestDto;
import com.grenlus.signage.dtos.SucursalResponseDto;
import com.grenlus.signage.entity.Cliente;
import com.grenlus.signage.entity.Sucursal;
import com.grenlus.signage.exception.RecursoNoEncontradoException;
import com.grenlus.signage.repository.ClienteRepository;
import com.grenlus.signage.repository.SucursalRepository;
import com.grenlus.signage.security.ControlAcceso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SucursalService {

    private final SucursalRepository sucursalRepository;
    private final ClienteRepository clienteRepository;
    private final ControlAcceso controlAcceso;

    public SucursalService(SucursalRepository sucursalRepository,
                           ClienteRepository clienteRepository,
                           ControlAcceso controlAcceso) {
        this.sucursalRepository = sucursalRepository;
        this.clienteRepository = clienteRepository;
        this.controlAcceso = controlAcceso;
    }

    @Transactional(readOnly = true)
    public List<SucursalResponseDto> listarTodas() {
        Long propio = controlAcceso.clienteDelUsuario();

        // Un ADMIN_CLIENTE ve solo sus locales. El SUPER_ADMIN, todos.
        return sucursalRepository.findAll().stream()
                .filter(s -> propio == null
                        || (s.getCliente() != null && propio.equals(s.getCliente().getId())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SucursalResponseDto buscarPorId(Long id) {
        return toResponse(obtener(id));
    }

    @Transactional
    public SucursalResponseDto crear(SucursalRequestDto request, Long clienteId) {
        controlAcceso.verificar(clienteId);

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId));

        Sucursal sucursal = new Sucursal();
        sucursal.setNombre(request.nombre());
        sucursal.setDireccion(request.direccion());
        sucursal.setCiudad(request.ciudad());
        sucursal.setProvincia(request.provincia());
        sucursal.setCliente(cliente);
        return toResponse(sucursalRepository.save(sucursal));
    }

    @Transactional
    public SucursalResponseDto actualizar(Long id, SucursalRequestDto request) {
        Sucursal sucursal = obtener(id);
        sucursal.setNombre(request.nombre());
        sucursal.setDireccion(request.direccion());
        sucursal.setCiudad(request.ciudad());
        sucursal.setProvincia(request.provincia());
        return toResponse(sucursal);
    }

    /**
     * Baja logica. Antes borraba fisicamente, y una sucursal con pantallas
     * viola el FK: pantalla.sucursal_id es obligatorio. Ademas las pantallas
     * de una sucursal cerrada siguen existiendo fisicamente en el local.
     */
    @Transactional
    public void desactivar(Long id) {
        obtener(id).setActivo(false);
    }

    /**
     * Vuelve a poner la sucursal en servicio.
     *
     * Una baja logica sin esto no es reversible: es un borrado que ademas deja
     * la fila ocupando su nombre. Si un local cierra por refaccion y reabre,
     * tiene que poder volver sin recrearlo y perder sus pantallas.
     */
    @Transactional
    public void reactivar(Long id) {
        obtener(id).setActivo(true);
    }

    private Sucursal obtener(Long id) {
        Sucursal sucursal = sucursalRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal", id));

        controlAcceso.verificar(
                sucursal.getCliente() == null ? null : sucursal.getCliente().getId());
        return sucursal;
    }

    private SucursalResponseDto toResponse(Sucursal s) {
        Cliente cliente = s.getCliente();
        return new SucursalResponseDto(
                s.getId(), s.getNombre(), s.getDireccion(), s.getCiudad(),
                s.getProvincia(), s.getActivo(),
                cliente == null ? null : cliente.getId(),
                cliente == null ? null : cliente.getNombre());
    }
}
