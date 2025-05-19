package net.originmobi.pdv.service;

import net.originmobi.pdv.repository.CaixaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test.*;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

public class CaixaServiceTest {

    // Intanciando o service
    private CaixaService caixaService;

    //instanciando as  dependências
    private UsuarioService usuariosMock;
    private CaixaLancamentoService lancamentosMock;
    private CaixaRepository caixasMock;

    @BeforeEach
    // mockando as dependencia
    void inicio() {
        usuariosMock = mock(UsuarioService.class);
        lancamentosMock = mock(CaixaLancamentoService.class);
        caixasMock = mock(CaixaRepository.class);
    }

}
