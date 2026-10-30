package com.eventhive.app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.context.SpringBootTest;

import com.eventhive.app.repository.CompraRepository;

@SpringBootTest
class DemoApplicationTests {

	@Autowired
	private CompraRepository compraRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void contextLoads() {
	}

	@Test
	void serializaPaginasConEstructuraViaDtoEstable() throws Exception {
		JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(
				new PageImpl<>(List.of("item"), PageRequest.of(0, 5), 1)));

		assertTrue(json.has("content"));
		assertTrue(json.has("page"));
		assertTrue(json.get("page").has("size"));
		assertTrue(json.get("page").has("totalElements"));
		assertTrue(json.get("page").has("totalPages"));
		assertTrue(json.get("page").has("number"));
	}

	@Test
	void paginaComprasNoAplicaFetchDeColeccionEnMemoria() {
		assertTrue(compraRepository.findByClienteIdConItems(Long.MAX_VALUE, PageRequest.of(0, 10)).isEmpty());
	}

}
