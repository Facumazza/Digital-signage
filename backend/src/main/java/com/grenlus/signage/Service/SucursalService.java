package com.grenlus.signage.service;

import com.grenlus.signage.entity.Cliente;
import com.grenlus.signage.entity.Sucursal;
import com.grenlus.signage.repository.ClienteRepository;
import com.grenlus.signage.repository.SucursalRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SucursalService {

    private final SucursalRepository sucursalRepository;
    private final ClienteRepository clienteRepository;

    public SucursalService(
            SucursalRepository sucursalRepository,
            ClienteRepository clienteRepository
    ) {
        this.sucursalRepository = sucursalRepository;
        this.clienteRepository = clienteRepository;
    }

    public List<Sucursal> listarTodas() {
        return sucursalRepository.findAll();
    }

    public Sucursal buscarPorId(Long id) {
        return sucursalRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Sucursal no encontrada"));
    }

    public Sucursal crear(Sucursal sucursal, Long clienteId) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() ->
                        new RuntimeException("Cliente no encontrado"));

        sucursal.setCliente(cliente);

        return sucursalRepository.save(sucursal);
    }

    public Sucursal actualizar(Long id, Sucursal datos) {

        Sucursal sucursal = buscarPorId(id);

        sucursal.setNombre(datos.getNombre());
        sucursal.setDireccion(datos.getDireccion());
        sucursal.setCiudad(datos.getCiudad());
        sucursal.setProvincia(datos.getProvincia());
        sucursal.setActivo(datos.getActivo());

        return sucursalRepository.save(sucursal);
    }

    public void eliminar(Long id) {

        Sucursal sucursal = buscarPorId(id);

        sucursalRepository.delete(sucursal);
    }
}