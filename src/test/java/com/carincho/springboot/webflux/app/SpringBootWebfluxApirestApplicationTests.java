package com.carincho.springboot.webflux.app;

import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.carincho.springboot.webflux.app.models.documents.Producto;
import com.carincho.springboot.webflux.app.models.services.ProductoService;

import reactor.core.publisher.Mono;


/**
 * 
 * Autor Carincho
 */

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SpringBootWebfluxApirestApplicationTests {

	
	@Autowired
	private WebTestClient client;
	
	@Autowired
	ProductoService service;
	
	
	
	@Test
	void listarTest() {
		
		client.get()
		.uri("/api/v2/productos")
		.accept(MediaType.APPLICATION_JSON)
		.exchange()//enviar peticion al endpoint
		.expectStatus().isOk()
		.expectHeader().contentType(MediaType.APPLICATION_JSON)
		.expectBodyList(Producto.class)
		.consumeWith(response -> {
			List<Producto> productos = response.getResponseBody();
			
			productos.forEach(p -> {
				System.out.println(p.getNombre());
			});
			
			Assertions.assertThat(productos.size() > 0).isTrue();
		});
//		.hasSize(12);
	}
	
	/**
	 * 
	 * Las pruebas unitarias deben ser sincrono ya que se realizan dentro del metodo test
	 */
	
	@Test
	void verDetalleTest() {
		
		Producto producto = service.findByNombre("IPhone 16 Pro Max").block();
		
		
		client.get()
		.uri("/api/v2/productos/{id}", Collections.singletonMap("id",producto.getId()))//se usa block por que no se puede usar suscribe dentro del contexto de pruebas
		.accept(MediaType.APPLICATION_JSON)
		.exchange()//enviar peticion al endpoint
		.expectStatus().isOk()
		.expectHeader().contentType(MediaType.APPLICATION_JSON)
		.expectBody(Producto.class)
		.consumeWith(response -> {
			Producto p = response.getResponseBody();
			
			
			Assertions.assertThat(p.getNombre()).isEqualTo("IPhone 16 Pro Max");
			Assertions.assertThat(p.getId()).isNotEmpty();
//			Assertions.assertThat(p.getId().length() > 0).isTrue();
		});
		/*.expectBody()
		.jsonPath("$.id").isEmpty()
		.jsonPath("$.nombre").isEqualTo("IPhone 16 Pro Max");*/
	}

}
