package com.grenlus.signage.service;

import com.grenlus.signage.dtos.ClienteRequestDto;
import com.grenlus.signage.dtos.ClienteResponseDto;
import com.grenlus.signage.entity.Cliente;
import com.grenlus.signage.exception.RecursoNoEncontradoException;
import com.grenlus.signage.exception.ReglaNegocioException;
import com.grenlus.signage.repository.ClienteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public ClienteResponseDto crear(ClienteRequestDto request) {
        String cuit = limpiar(request.cuit());
        if (cuit != null && clienteRepository.existsByCuit(cuit)) {
            throw new ReglaNegocioException("Ya existe un cliente con el CUIT " + cuit);
        }
        Cliente cliente = Cliente.builder()
                .nombre(request.nombre().trim())
                .cuit(cuit)
                .email(limpiar(request.email()))
                .telefono(limpiar(request.telefono()))
                .build();
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDto> listarActivos() {
        return clienteRepository.findByActivoTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDto> listarTodos() {
        return clienteRepository.findAll(Sort.by("nombre")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponseDto buscarPorId(Long id) {
        return toResponse(obtener(id));
    }

    @Transactional
    public ClienteResponseDto actualizar(Long id, ClienteRequestDto request) {
        Cliente cliente = obtener(id);
        String cuit = limpiar(request.cuit());
        if (cuit != null && clienteRepository.existsByCuitAndIdNot(cuit, id)) {
            throw new ReglaNegocioException("Ya existe otro cliente con el CUIT " + cuit);
        }
        cliente.setNombre(request.nombre().trim());
        cliente.setCuit(cuit);
        cliente.setEmail(limpiar(request.email()));
        cliente.setTelefono(limpiar(request.telefono()));
        return toResponse(cliente);
    }

    /**
     * Un campo opcional vacio se guarda como null. Si no, dos clientes sin CUIT
     * quedarian con "" y chocarian en la validacion de CUIT repetido.
     */
    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    /**
     * Baja logica. No se borra: sus sucursales, contenidos y playlists lo
     * referencian, y un delete fisico dejaria todo eso huerfano.
     */
    @Transactional
    public void desactivar(Long id) {
        obtener(id).setActivo(false);
    }

    /** La vuelta de la baja logica: sin esto, desactivar es irreversible. */
    @Transactional
    public void reactivar(Long id) {
        obtener(id).setActivo(true);
    }

    private Cliente obtener(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", id));
    }

    private ClienteResponseDto toResponse(Cliente c) {
        return new ClienteResponseDto(
                c.getId(), c.getNombre(), c.getCuit(), c.getEmail(),
                c.getTelefono(), c.getActivo(), c.getFechaAlta());
    }
}
