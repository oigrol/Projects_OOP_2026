package com.weather.report.repositories;

import java.util.List;
import java.util.Objects;

import com.weather.report.persistence.PersistenceManager;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

/**
 * Generic repository exposing basic CRUD operations backed by the persistence
 * layer.
 * <p>
 * Concrete repositories extend/compose this class to centralise common database
 * access
 * logic for all entities, as described in the README.
 *
 * @param <T>  entity type
 * @param <ID> identifier (primary key) type
 */
public class CRUDRepository<T, ID> {

  protected Class<T> entityClass;

  /**
   * Builds a repository for the given entity class.
   *
   * @param entityClass entity class handled by this repository
   */
  public CRUDRepository(Class<T> entityClass) {
    Objects.requireNonNull(entityClass);
    this.entityClass = entityClass;
  }

  /**
   * Given an entity class retrieves the name of the entity to be used in the
   * queries.
   * 
   * @return the name of the entity (to be used in queries)
   */
  protected String getEntityName() {
    Entity ea = entityClass.getAnnotation(jakarta.persistence.Entity.class);
    if (ea == null)
      throw new IllegalArgumentException("Class " + this.entityClass.getName() + " must be annotated as @Entity");
    if (ea.name().isEmpty())
      return this.entityClass.getSimpleName();
    return ea.name();
  }

  /**
   * Persists a new entity instance.
   *
   * @param entity entity to persist
   * @return persisted entity
   */
  public T create(T entity) {
    EntityManager em = PersistenceManager.getEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
        tx.begin();
        em.persist(entity);
        tx.commit();
        return entity;
    } catch (Exception e) {
        if (tx.isActive()) tx.rollback();
        throw e;
    } finally {
        PersistenceManager.closeEntityManager();
    }
  }

  /**
   * Reads a single entity by identifier.
   *
   * @param id entity identifier (primary key)
   * @return found entity or {@code null} if absent
   */
  public T read(ID id) {
    EntityManager entityManager = PersistenceManager.getEntityManager();
    try {
      T t = entityManager.find(entityClass, id);
      return t;
    } finally {
      PersistenceManager.closeEntityManager();
    }
  }

  /**
   * Reads all entities of the managed type.
   *
   * @return list of all entities
   */
  public List<T> read() {
    EntityManager em = PersistenceManager.getEntityManager();
    try {
        return em.createQuery("SELECT e FROM " + getEntityName() + " e", entityClass).getResultList();
    } finally {
        PersistenceManager.closeEntityManager();
    }
  }

  /**
   * Updates an existing entity.
   *
   * @param entity entity with new state
   * @return updated entity
   */
  public T update(T entity) {
    EntityManager entityManager = PersistenceManager.getEntityManager();
    EntityTransaction transaction = entityManager.getTransaction();

    try {
      transaction.begin();
      T mergedEntity = entityManager.merge(entity);
      transaction.commit();
      return mergedEntity;
    } catch (RuntimeException e) {
      if (transaction.isActive()) {
        transaction.rollback();
      }
      throw e;
    } finally {
      PersistenceManager.closeEntityManager();
    }
  }

  /**
   * Deletes an entity by identifier (primary key).
   *
   * @param id entity identifier (primary key)
   * @return deleted entity
   */
  public T delete(ID id) {
    EntityManager em = PersistenceManager.getEntityManager();
    EntityTransaction tx = em.getTransaction();
    T entity = null;
    try {
        tx.begin();
        entity = em.find(entityClass, id);
        if (entity != null) {
            em.remove(entity);
        }
        tx.commit();
        return entity;
    } catch (Exception e) {
        if (tx.isActive()) tx.rollback();
        throw e;
    } finally {
        PersistenceManager.closeEntityManager();
    }
  }

}
