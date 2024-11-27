package com.carincho.springboot.webflux.app;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.carincho.springboot.webflux.app.models.documents.Categoria;
import com.carincho.springboot.webflux.app.models.documents.Producto;
import com.carincho.springboot.webflux.app.models.services.ProductoService;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;


/**
 * 
 * Autor Carincho
 */

@AutoConfigureWebTestClient //Es la autoconfiguracion de spring para MOCK
//@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)Levanta el servidor
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)// simulado el ambiente web
class SpringBootWebfluxApirestApplicationTests {

	
	@Autowired
	private WebTestClient client;
	
	@Autowired
	ProductoService service;
	
	@Value("${config.base.endpoint}")
	private String url;
	
	
	@Test
	void listarTest() {
		
		client.get()
		.uri(url)
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
		.uri(url + "/{id}", Collections.singletonMap("id",producto.getId()))//se usa block por que no se puede usar suscribe dentro del contexto de pruebas
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
	
	@Test
	public void crearTest() {
		
		Categoria categoria = service.findByCategoriaNombre("Muebles").block();
		
		Producto producto = new Producto("Mesa Comedor", 100.00, categoria);
		
		client.post().uri(url)
		.contentType(MediaType.APPLICATION_JSON)
		.accept(MediaType.APPLICATION_JSON)
		.body(Mono.just(producto), Producto.class)
		.exchange()
		.expectStatus().isCreated()
		.expectHeader().contentType(MediaType.APPLICATION_JSON)
		.expectBody()
		.jsonPath("$.producto.id").isNotEmpty()
		.jsonPath("$.producto.nombre").isEqualTo("Mesa Comedor")
		.jsonPath("$.producto.categoria.nombre").isEqualTo("Muebles");
	}
	@Test
	public void crear2Test() {
		
		Categoria categoria = service.findByCategoriaNombre("Muebles").block();
		
		Producto producto = new Producto("Mesa Comedor", 100.00, categoria);
		
		client.post().uri(url)
		.contentType(MediaType.APPLICATION_JSON)
		.accept(MediaType.APPLICATION_JSON)
		.body(Mono.just(producto), Producto.class)
		.exchange()
		.expectStatus().isCreated()
		.expectHeader().contentType(MediaType.APPLICATION_JSON)
		.expectBody(new ParameterizedTypeReference<LinkedHashMap<String, Object>>() {})//con ParameterizedTypeReferenceasignamos el tipo de la respuesta
		.consumeWith(response -> {
			
			Object o = response.getResponseBody().get("producto");
			Producto p =new ObjectMapper().convertValue(o,Producto.class);
			
			Assertions.assertThat(p.getId()).isNotEmpty();
			Assertions.assertThat(p.getNombre()).isEqualTo("Mesa Comedor");
			Assertions.assertThat(p.getCategoria().getNombre()).isEqualTo("Muebles");
			
		});
	}
	
	@Test
	public void editarTest() {
		
		Producto producto = service.findByNombre("Drone dji").block();
		
		Categoria categoria = service.findByCategoriaNombre("Tecnologia").block();
		
		Producto productoEdit = new Producto("Drone dji neo mini", 6499.00, categoria);
		
		client.put()
		.uri(url + "/{id}", Collections.singletonMap("id", producto.getId()))
		.contentType(MediaType.APPLICATION_JSON)
		.accept(MediaType.APPLICATION_JSON)
		.body(Mono.just(productoEdit), Producto.class)
		.exchange()
		.expectStatus().isCreated()
		.expectHeader().contentType(MediaType.APPLICATION_JSON)
		.expectBody()
		.jsonPath("$.id").isNotEmpty()
		.jsonPath("$.nombre").isEqualTo("Drone dji neo mini")
		.jsonPath("$.categoria.nombre").isEqualTo("Tecnologia");
		
	}
	
	@Test
	public void eliminarTest() {
		Producto producto = service.findByNombre("Power Block sport").block();
		
		client.delete()
		.uri(url + "/{id}", Collections.singletonMap("id", producto.getId()))
		.exchange()
		.expectStatus().isNoContent()
		.expectBody().isEmpty();
		
		//comprobar que lo borro
		client.get()
		.uri("/api/v2/productos/{id}", Collections.singletonMap("id", producto.getId()))
		.exchange()
		.expectStatus().isNotFound()
		.expectBody().isEmpty();
		
	}

}
