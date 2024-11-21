package com.carincho.springboot.webflux.app.controller;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.carincho.springboot.webflux.app.models.documents.Producto;
import com.carincho.springboot.webflux.app.models.services.ProductoService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;




@RestController
@RequestMapping("api/productos")
public class ProductoController {
	
	@Autowired
	private ProductoService service;
	
	@Value("${config.upload.path}")
	private String path;
	
	/**
	 * 
	 * Retornar el Flux de producto es la forma mas facil 
	 * Retornar el Mono de ResponseEntity se puede manejar la respuesta
	 * 
	 * 
	 * 
	 * 
	 */
	@GetMapping
	public Mono<ResponseEntity<Flux<Producto>>>  listar() {
		
		return Mono.just(ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(service.findAll())
				);
	}
	
	@GetMapping("/{id}")
	public Mono<ResponseEntity<Producto>>ver(@PathVariable String id) {
		
		return service.findById(id)
				.map(p -> ResponseEntity.ok()
						.contentType(MediaType.APPLICATION_JSON)
						.body(p))
				.defaultIfEmpty(ResponseEntity.notFound().build());//Construye la respuesta sin contenido
	}
	
	/**
	 * 
	 * Tiene que ser del tipo Reactivo el parametro de entrada producto cuando se valida y falla en tipo reactivo se puede capturar el error en el operador onErrorResume
	 * 
	 * 
	 */
	
	@PostMapping
	public Mono<ResponseEntity<Map<String, Object>>> crear(@Valid @RequestBody Mono<Producto> monoProducto) {
		

		Map<String, Object>respuesta = new HashMap<>();
		
		
		return monoProducto.flatMap(producto -> {
			
			
			if(producto.getCreateAt() == null) {
				producto.setCreateAt(new Date());
				
			}
			return service.save(producto)
					.map(p -> {
						respuesta.put("producto", p);
						respuesta.put("mensaje", "Producto creado con exito");
						respuesta.put("timestamp", new Date());
						return ResponseEntity
					
							.created(URI.create("/api/productos/".concat(p.getId())))
							.contentType(MediaType.APPLICATION_JSON)
							.body(respuesta);
					});
			
		}).onErrorResume(t -> {
			
			return Mono.just(t).cast(WebExchangeBindException.class)
					.flatMap(e -> Mono.just(e.getFieldErrors()))
					.flatMapMany(Flux::fromIterable)
					.map(fieldError-> "El campo " +  fieldError.getField() + " " +fieldError.getDefaultMessage()) //Se convierte a tipo string cada elemento de la lista
					.collectList()
					.flatMap(list -> {
						respuesta.put("errors", list);
						respuesta.put("timestamp", new Date());
						respuesta.put("status", HttpStatus.BAD_REQUEST.value());
						return Mono.just(ResponseEntity.badRequest().body(respuesta));
						
					});
			
		});
	
		
	}
	
	
	@PutMapping("/{id}")
	public Mono<ResponseEntity<Producto>> editar(@PathVariable String id, @RequestBody Producto producto) {
		
		
		
		return service.findById(id)
				.flatMap(p -> {
					p.setNombre(producto.getNombre());
					p.setPrecio(producto.getPrecio());
					p.setCategoria(producto.getCategoria());
					
					return service.save(p);
				})
				.map(p -> ResponseEntity.created(URI.create("/api/productos/".concat(p.getId())))
						.contentType(MediaType.APPLICATION_JSON)
						.body(p))
				.defaultIfEmpty(ResponseEntity.notFound().build());
				
	}
	
	@DeleteMapping("/{id}")
	public Mono<ResponseEntity<Void>> eliminar(@PathVariable String id) {
		
		return service.findById(id).flatMap(p -> {
			return service.delete(p)
					.then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)));//Esto es para convertir a ResponseEntity el flatMap regresa un Mono<Void>
		});
		
	}
	
	@PostMapping("/upload/{id}")
	public Mono<ResponseEntity<Producto>> upload(@PathVariable String id, @RequestPart FilePart file) {
		
		return service.findById(id).flatMap(p -> {
			
			p.setFoto(UUID.randomUUID().toString() + "-" + file.name()
			.replace(" ", "")
			.replace(":", "")
			.replace("\\", ""));
			
			return file.transferTo(new File(path + p.getFoto())).then(service.save(p));
			
		})
				.map(p -> ResponseEntity.ok(p))
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}
	
	@PostMapping("/v2")
	public Mono<ResponseEntity<Producto>> crearConFoto( Producto producto, @RequestPart FilePart file) {
		
		if(producto.getCreateAt() == null) {
			producto.setCreateAt(new Date());
			
		}
		
		producto.setFoto(UUID.randomUUID().toString() + "-" + file.name()
		.replace(" ", "")
		.replace(":", "")
		.replace("\\", ""));
		
		return file.transferTo(new File(path + producto.getFoto())).then(service.save(producto))
				.map(p -> ResponseEntity.created(URI.create("/api/productos/".concat(p.getId())))
						.contentType(MediaType.APPLICATION_JSON)
						.body(p));
	}
	
	
	
	

}