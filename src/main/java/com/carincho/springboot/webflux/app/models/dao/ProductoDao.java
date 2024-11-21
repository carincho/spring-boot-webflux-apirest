package com.carincho.springboot.webflux.app.models.dao;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.carincho.springboot.webflux.app.models.documents.Producto;

import reactor.core.publisher.Mono;

public interface ProductoDao extends ReactiveMongoRepository<Producto, String>{
	
	public Mono<Producto>findByNombre(String nombre);
	
	@Query("{'nombre': ? 0}")
	public Mono<Producto>obtenePorNombre(String nombre);

}
