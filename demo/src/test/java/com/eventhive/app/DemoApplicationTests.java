package com.eventhive.app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.context.SpringBootTest;

import com.eventhive.app.repository.CompraRepository;

@SpringBootTest
class DemoApplicationTests {

	@Autowired
	private CompraRepository compraRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void paginaComprasNoAplicaFetchDeColeccionEnMemoria() {
		assertTrue(compraRepository.findByClienteIdConItems(Long.MAX_VALUE, PageRequest.of(0, 10)).isEmpty());
	}

}
