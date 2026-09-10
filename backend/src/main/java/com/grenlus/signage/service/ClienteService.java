package com.grenlus.signage.service;

import com.grenlus.signage.dtos.ClienteRequestDto;
import com.grenlus.signage.dtos.ClienteResponseDto;
import com.grenlus.signage.entity.Cliente;
import com.grenlus.signage.exception.RecursoNoEncontradoException;
import com.grenlus.signage.repository.ClienteRepository;
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
        Cliente cliente = Cliente.builder()
                .nombre(request.nombre())
                .cuit(request.cuit())
                .email(request.email())
                .telefono(request.telefono())
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
    public ClienteResponseDto buscarPorId(Long id) {
        return toResponse(obtener(id));
    }

    @Transactional
    public ClienteResponseDto actualizar(Long id, ClienteRequestDto request) {
        Cliente cliente = obtener(id);
        cliente.setNombre(request.nombre());
        cliente.setCuit(request.cuit());
        cliente.setEmail(request.email());
        cliente.setTelefono(request.telefono());
        return toResponse(cliente);
    }

    /**
     * Baja logica. No se borra: sus sucursales, contenidos y playlists lo
     * referencian, y un delete fisico dejaria todo eso huerfano.
     */
    @Transactional
    public void desactivar(Long id) {
        obtener(id).setActivo(false);
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
